package booster

import configs.Conf
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import metadata.MetadataStorage
import metadata.metadata
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import pluginloader.api.*
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
    val boosts = (playerBoosters[uuid] ?: return null)
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
    globalBoosters[type].nonNull{
        var call = false
        val iter = it.iterator()
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
        if(call) globalListener.forEach{calling -> calling(type)}
    }
    playerBoosters[uuid].nonNull{it.nonNull{boosterMap -> boosterMap[type].nonNull{
        var call = false
        val iter = it.iterator()
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
        if(call) playerListener.forEach{calling -> calling(type, uuid)}
    }}}
    listenBoosters.forEach{caching {bst = it(uuid, type, bst)}}
    multiplyBoosters.nonNull{bst *= 1.0 + it.sumOf{boost -> boost.boost}}
    return input * bst
}

@Conf
internal var config = Config()

private val globalBoosters = HashMap<String, MutableList<Boost>>()
private val playerBoosters = HashMap<UUID, HashMap<String, MutableList<Boost>>>()
private lateinit var storage: MetadataStorage<Boost>
private lateinit var spi: SPI<List<Boost>>

@Listener
internal fun join(event: PlayerJoinEvent){
    playerJoin(event.uuid)
}

@Listener
internal fun quit(event: PlayerQuitEvent){
    playerQuit(event.uuid)
}

private const val prefix = "§8[§aPlu§8]§f"

@Command("booster", op = true)
internal fun cmdBooster(sender: Sender, args: Args){
    if(args.isEmpty()){
        sender.sendMessage("$prefix Usage: §6/booster [player|global] {player} [add|clear|list|calculate]...")
        return
    }
    val context = when(args[0]){
        "player" -> {
            if(args.size == 1){
                sender.sendMessage("$prefix Usage: §6/booster player [player] [add|clear|list|update]...")
                return
            }
            val player = Bukkit.getPlayer(args[1])
            if(player == null){
                sender.sendMessage("$prefix §cPlayer §6'${args[1]}'§c not found")
                return
            }
            playerBoosters.computeIfAbsent(player.uuid){HashMap()}
        }
        "global" -> globalBoosters
        else -> {
            sender.sendMessage("$prefix Usage: §6/booster [player|global] {player} [add|clear|list]...")
            return
        }
    }
    val isPlayer = args[0] == "player"
    val offset = if(isPlayer) 2 else 1
    if(args.size == offset){
        sender.sendMessage("$prefix Usage: §6/booster [player|global] {player} [add|clear|list]...")
        return
    }
    when(args[offset]){
        "add" -> {
            if(args.size < offset + 4){
                sender.sendMessage("$prefix Usage: §6/booster [player|global] {player} add [type] [time in minutes|*] [boost]")
                return
            }
            val type = args[offset + 1]
            val timeInMinutes = if(args[offset + 2] == "*") Long.MAX_VALUE else args[offset + 2].toLongOrNull()
            if(timeInMinutes == null){
                sender.sendMessage("$prefix §cTime in minutes §6'${args[offset + 2]}'§c not a number")
                return
            }
            val boostStr = args[offset + 3]
            val multiply = boostStr[0] == '*'
            val boost = (if(multiply) boostStr.substring(1) else boostStr).toDoubleOrNull()
            if(boost == null){
                sender.sendMessage("$prefix §cBoost §6'${args[offset + 3]}'§c not a number")
                return
            }
            val booster = Boost(type, if(timeInMinutes == Long.MAX_VALUE) timeInMinutes else System.currentTimeMillis() + (timeInMinutes * 60000), boost, multiply)
            if(isPlayer)context.computeIfAbsent(type){ArrayList()}.add(booster)
            else storage.push(booster)
            sender.sendMessage("$prefix New booster with type §6'$type'§f, time: §6'${if(booster.infinity) "infinity" else "$timeInMinutes minutes"}'§f, boost §6'$boost'§f, multiply §6'$multiply'")
            if(isPlayer){
                val uuid = Bukkit.getPlayer(args[1]).uuid
                playerListener.forEach{it(type, uuid)}
            }
        }
        "clear" -> {
            context.forEach{
                it.value.forEach booster@{booster ->
                    if(booster.end())return@booster
                    sender.sendMessage("$prefix Delete booster with type §6'${booster.type}'§f time §6'${
                        if(booster.infinity) "infinity" 
                        else "${booster.timeToEndInMinutes()} minutes"
                    }'§f, boost §6'${booster.boost}'§f, multiply §6'${booster.multiply}'")
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
            if(args.size < offset + 2){
                sender.sendMessage("$prefix Usage: §6/booster [player|global] {player} calculate [type] [input]")
                return
            }
            val result = calculate((Bukkit.getPlayer(args[1]) ?: return).uuid, args[offset + 1], args[offset + 2].toDouble())
            sender.sendMessage("$prefix Result: §6'$result'")
        }
        "update" -> {
            if(args.size < 4){
                sender.sendMessage("$prefix Usage: §6/booster player [player] update [type]")
                return
            }
            val player = Bukkit.getPlayer(args[1]) ?: return
            playerListener.forEach{it(args[3], player.uuid)}
        }
        else -> {
            sender.sendMessage("$prefix Usage: §6/booster [player|global] {player} [add|clear|list]...")
        }
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
    spi = SPI.get(plugin, config.tableName, ListSerializer(Boost.serializer()))
    Bukkit.getOnlinePlayers().forEach{playerJoin(it.uuid)}
}

@Unload
internal fun unload(){
    Bukkit.getOnlinePlayers().forEach{playerQuit(it.uuid)}
}

@Command("dropboosters", op = true)
internal fun cmd(sender: Sender, args: Args){
    spi.iterate({uuid, boosters ->
        val new = ArrayList<Boost>()
        boosters.forEach{if(it.infinity)new.add(it)}
        spi.save(uuid, new)
    }){
        sender.sendMessage("ok")
    }
}

private fun playerJoin(uuid: UUID){
    spi.load(uuid){ maybe ->
        playerBoosters.remove(uuid)
        maybe.nonNull { list ->
            val map = playerBoosters.computeIfAbsent(uuid){HashMap()}
            list.forEach{if(!it.end()){
                map.computeIfAbsent(it.type){ArrayList()}.add(it)
                playerListener.forEach{calling -> calling(it.type, uuid)}
            }}
        }
    }
}

private fun playerQuit(uuid: UUID){
    val boosters = playerBoosters.remove(uuid) ?: return
    val saveBoosters = ArrayList<Boost>()
    boosters.forEach{saveBoosters.addAll(it.value)}
    spi.save(uuid, saveBoosters)
}

@Serializable
private data class Boost(override val type: String, override val end: Long, override val boost: Double, val multiply: Boolean = false): BoostInfo

@Serializable
internal class Config(val tableName: String = "boosters")