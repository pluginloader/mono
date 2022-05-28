package morestats

import booster.booster
import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player
import pluginloader.api.*
import sspi.SSpi
import sspi.sSpi
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

interface Stats{
    fun add(uuid: UUID, stat: String, n: Double, needBooster: Boolean = true)

    fun get(uuid: UUID, stat: String): Double

    fun getAll(uuid: UUID): Map<String, Double>

    fun onLoad(callback: (UUID) -> Unit)

    fun onUpdate(type: String, callback: (UUID, old: Double, new: Double) -> Unit)
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class StatsAPI

@Conf
internal var config = Config()

private lateinit var spi: SSpi<PlayerStats>
private lateinit var plu: LoaderPlugin
private val updateHandlers = HashMap<String, ArrayList<(UUID, old: Double, new: Double) -> Unit>>()

@Command("snowstats", op = true, aliases = ["showstats"])
internal fun snowstats(sender: Sender, args: Args){
    val argsSize = args.size
    if(sender !is Player && argsSize == 0){
        sender.sendMessage("$prefix Usage: §6/snowstats [player]")
        return
    }
    val player = if(argsSize == 0) sender as Player else
        args.player(sender, 0) ?: return
    val stats = args.nonNull(sender, spi[player.uuid]){"Player §6${player.name}§f not loaded"} ?: return
    sender.sendMessage("$prefix Stats of player §6${player.name}")
    stats.stats.forEach{sender.sendMessage("$prefix §e${it.key}§f: §6${it.value}")}
}

@Command("morestats", op = true)
internal fun morestats(sender: Sender, args: Args){
    args.use(sender, 3, "morestats [player] [stat] [n] {need booster? true/false}") ?: return
    val player = args.player(sender, 0) ?: return
    val stat = args[1]
    val n = args.double(sender, 2) ?: return
    val needBuster = if(args.size == 3) true else args[3].toBoolean()
    args.nonNull(sender, spi[player.uuid]){"Player §6${player.name}§f not loaded"} ?: return
    addStat(player.uuid, stat, n, needBuster)
    if(sender is Player)sender.sendMessage("$prefix Add §6$n§f in stat §6$stat§f to player §6${player.name}")
}

private val onLoad = ArrayList<(UUID) -> Unit>()

@Load
internal fun load(plugin: Plugin){
    plugin.fieldReplacer(StatsAPI::class){plu ->
        object : Stats {
            override fun add(uuid: UUID, stat: String, n: Double, needBooster: Boolean) {
                addStat(uuid, stat, n, needBooster)
            }

            override fun get(uuid: UUID, stat: String): Double {
                return (spi[uuid] ?: return 0.0).stats[stat] ?: 0.0
            }

            override fun getAll(uuid: UUID): Map<String, Double> {
                return (spi[uuid] ?: return emptyMap()).stats
            }

            override fun onLoad(callback: (UUID) -> Unit) {
                onlinePlayers.forEach{if(spi[it] != null)callback(it.uuid)}
                onLoad.add(callback)
                plu.unloadHandler{onLoad.remove(callback)}
            }

            override fun onUpdate(type: String, callback: (UUID, old: Double, new: Double) -> Unit) {
                var handlers = updateHandlers[type]
                if(handlers == null){
                    handlers = ArrayList()
                    updateHandlers[type] = handlers
                }
                handlers.add(callback)
                plu.unloadHandler{
                    val h = updateHandlers[type] ?: return@unloadHandler
                    h.remove(callback)
                    if(h.isEmpty()) updateHandlers.remove(type)
                }
            }
        }
    }
    spi = plugin.sSpi(load = {player, _ ->
        onLoad.forEach{caching{it(player.uuid)}?.printStackTrace()}
    })
    plu = plugin
    if(config.autosaveTime != -1) plugin.runTaskTimer(config.autosaveTime){
        onlinePlayers.forEach{spi.forceSave(it)}
    }
}

private fun addStat(uuid: UUID, stat: String, n: Double, needBooster: Boolean){
    val stats = spi[uuid] ?: return
    val busted = if(needBooster) plu.booster(uuid, config.busterPrefix + stat, n) else n
    val current = stats.stats.getOrDefault(stat, 0.0)
    val new = busted + current
    updateHandlers[stat]?.forEach{caching{it(uuid, current, new)}}
    stats.stats[stat] = new
}

@Serializable
internal class Config{
    val table = "morestats"
    val busterPrefix = "stats_"
    val autosaveTime = 1200
}

@Serializable
internal class PlayerStats(val stats: MutableMap<String, Double>)