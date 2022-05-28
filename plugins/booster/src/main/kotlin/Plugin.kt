package booster

import configs.conf
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import metadata.MetadataStorage
import metadata.metadata
import org.bukkit.Bukkit
import pluginloader.api.*
import pstore.PStore
import pstore.getOr
import pstore.pStore
import spi.SPI
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

interface BoostInfo{
    val type: String
    val boost: Double
    val end: Long

    val infinity get() = end == Long.MAX_VALUE

    fun end(): Boolean = end <= System.currentTimeMillis()

    fun timeToEndInMinutes(): Int = (end - System.currentTimeMillis()).toInt() / 60000
}

interface Booster{
    fun calculate(uuid: UUID, input: Double): Double
}

private val playerListener = ArrayList<(String, UUID) -> Unit>()
private val globalListener = ArrayList<(String) -> Unit>()
private val listenBoosters = ArrayList<(UUID, String, Double) -> Double>()

fun LoaderPlugin.listenBooster(listen: (uuid: UUID, booster: String, current: Double) -> Double){
    listenBoosters.add(listen)
    unloadHandler{listenBoosters.remove(listen)}
}

fun LoaderPlugin.onUpdate(player: (String, UUID) -> Unit, global: (String) -> Unit){
    playerListener.add(player)
    globalListener.add(global)
    unloadHandler{
        playerListener.remove(player)
        globalListener.remove(global)
    }
}

fun LoaderPlugin.boostInfo(uuid: UUID): List<BoostInfo>?{
    val boosts = (pStore[uuid] ?: return null)
    val list = ArrayList<BoostInfo>()
    boosts.values.forEach(list::addAll)
    return list
}

fun LoaderPlugin.booster(type: String): Booster{
    return object: Booster{
        override fun calculate(uuid: UUID, input: Double): Double {
            return calculate(uuid, type, input)
        }
    }
}

fun LoaderPlugin.booster(uuid: UUID, type: String, input: Double): Double{
    return calculate(uuid, type, input)
}

private fun calculate(uuid: UUID, type: String, input: Double): Double{
    var bst = 1.0
    var multiplyBoosters: ArrayList<Boost>? = null
    fun useIterators(boosters: MutableList<Boost>, global: Boolean){
        var call = false
        val iter = boosters.iterator()
        while (iter.hasNext()){
            val next = iter.next()
            if(next.end()){
                call = true
                iter.remove()
                continue
            }
            if(next.multiply){
                if(multiplyBoosters == null)multiplyBoosters = ArrayList(2)
                multiplyBoosters!!.add(next)
                continue
            }
            bst += next.boost
        }
        if(call) {
            if(global)globalListener.forEach{calling -> calling(type)}
            else playerListener.forEach{calling -> calling(type, uuid)}
        }
    }
    globalBoosters[type].nonNull{useIterators(it, true)}
    pStore[uuid].nonNull{useIterators(it[type] ?: return@nonNull, false)}
    listenBoosters.forEach{caching {bst = it(uuid, type, bst)}}
    multiplyBoosters.nonNull{bst *= 1.0 + it.sumOf{boost -> boost.boost}}
    return input * bst
}

private val globalBoosters = HashMap<String, MutableList<Boost>>()
private lateinit var pStore: PStore<HashMap<String, MutableList<Boost>>>
private lateinit var storage: MetadataStorage<Boost>
private lateinit var spi: SPI<List<Boost>>

@Command("booster", op = true)
internal fun cmdBooster(sender: Sender, args: Args){
    val use = "booster [player|global] {player} [add|clear|list|calculate]..."
    args.use(sender, 1, use) ?: return
    val context = when(args[0]){
        "player" -> {
            args.use(sender, 2, "booster player [player] [add|clear|list|update]...") ?: return
            pStore[args.player(sender, 1) ?: return] ?: return
        }
        "global" -> globalBoosters
        else -> {
            args.use(sender, 100, use)
            return
        }
    }
    val isPlayer = context != globalBoosters
    val offset = if(isPlayer) 2 else 1
    if(args.size == offset) args.use(sender, 100, use) ?: return
    when(args[offset]){
        "add" -> {
            args.use(sender, offset + 4, "booster [player|global] {player} add [type] [time in minutes|*] [boost]") ?: return
            val type = args[offset + 1]
            val timeInMinutes = if(args[offset + 2] == "*") Long.MAX_VALUE else (args.int(sender, offset + 2) ?: return).toLong()
            val boostStr = args[offset + 3]
            val multiply = boostStr[0] == '*'
            val boost = args.nonNull(sender, (if(multiply) boostStr.substring(1) else boostStr).toDoubleOrNull()){
                "Boost §6${args[offset + 3]}§f not a number"
            } ?: return
            val booster = Boost(type, if(timeInMinutes == Long.MAX_VALUE) timeInMinutes else System.currentTimeMillis() + (timeInMinutes * 60000), boost, multiply)
            if(isPlayer)context.computeIfAbsent(type){ArrayList()}.add(booster)
            else storage.push(booster)
            sender.sendMessage("$prefix New booster with type §6$type§f, time: §6${if(booster.infinity) "infinity" else "$timeInMinutes minutes"}§f, boost §6$boost§f, multiply §6$multiply")
            if(isPlayer){
                val uuid = Bukkit.getPlayer(args[1]).uuid
                playerListener.forEach{it(type, uuid)}
            }
        }
        "clear" -> {
            context.forEach{
                it.value.forEach booster@{booster ->
                    if(booster.end())return@booster
                    sender.sendMessage("$prefix Delete booster with type §6${booster.type}§f time §6${
                        if(booster.infinity) "infinity" 
                        else "${booster.timeToEndInMinutes()} minutes"
                    }§f, boost §6${booster.boost}§f, multiply §6${booster.multiply}")
                }
            }
            if(isPlayer)context.clear()
            else globalBoosters.values.forEach{it.forEach(storage::drop)}
        }
        "list" -> {
            context.forEach{
                it.value.forEach booster@{booster ->
                    if(booster.end())return@booster
                    sender.sendMessage("$prefix Booster with type §6'${booster.type}'§f time §6'${
                        if(booster.infinity) "infinity"
                        else "${booster.timeToEndInMinutes()} minutes"
                    }'§f, boost §6'${booster.boost}'§f, multiply §6'${booster.multiply}'")
                }
            }
        }
        "calculate" -> {
            args.use(sender, offset + 2, "booster [player|global] {player} calculate [type] [input]") ?: return
            val result = calculate((args.player(sender, 1) ?: return).uuid, args[offset + 1], args[offset + 2].toDouble())
            sender.sendMessage("$prefix Result: §6'$result'")
        }
        "update" -> {
            args.use(sender, 4, "booster player [player] update [type]") ?: return
            val player = args.player(sender, 1) ?: return
            playerListener.forEach{it(args[3], player.uuid)}
        }
        else -> args.use(sender, 100, use)
    }
}

@Load
internal fun load(plugin: LoaderPlugin){
    storage = plugin.metadata(Boost.serializer(),
        {if(!it.end()){
            globalBoosters.computeIfAbsent(it.type){ArrayList()}.add(it)
            globalListener.forEach{calling -> calling(it.type)}
        } },
        {booster ->
            globalBoosters[booster.type].nonNull{it.remove(booster)}
            globalListener.forEach{calling -> calling(booster.type)}
        }
    )
    val config = plugin.conf(::Config)
    spi = SPI.get(plugin, config.tableName, ListSerializer(Boost.serializer()))
    pStore = plugin.pStore(load = {player -> spi.load(player.uuid) spi@{input ->
        if(player.isOnline.not())return@spi
        val boosters = pStore.getOr(player, ::HashMap)
        if(input == null)return@spi
        input.forEach{
            if(it.end())return@forEach
            boosters.computeIfAbsent(it.type){ArrayList()}.add(it)
        }
    }}, unload = {player, boosters ->
        boosters ?: return@pStore
        val saveBoosters = ArrayList<Boost>()
        boosters.forEach{saveBoosters.addAll(it.value)}
        spi.save(player.uuid, saveBoosters)
    })
}

@Serializable
private data class Boost(override val type: String, override val end: Long, override val boost: Double, val multiply: Boolean = false): BoostInfo

@Serializable
internal class Config{val tableName= "boosters"}
