package morestats

import booster.booster
import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import playerinfo.PlayerReadable
import pluginloader.api.*
import spi.SPI
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

private lateinit var spi: SPI<PlayerStats>
private lateinit var plu: LoaderPlugin
private val players = HashMap<UUID, PlayerStats>()
private val updateHandlers = HashMap<String, ArrayList<(UUID, old: Double, new: Double) -> Unit>>()

private const val prefix = "§8[§aPlu§8]§f"

@Command("snowstats", op = true, aliases = ["showstats"])
internal fun snowstats(sender: Sender, args: Args){
    val argsSize = args.size
    if(sender !is Player && argsSize == 0){
        sender.sendMessage("$prefix Usage: §6/snowstats [player]")
        return
    }
    val player = if(argsSize == 0) sender as Player else {
        val pl = Bukkit.getPlayer(args[0])
        if(pl == null){
            sender.sendMessage("$prefix §cPlayer §6'${args[0]}'§c not found")
            return
        }
        pl
    }
    val stats = players[player.uuid]
    if(stats == null){
        sender.sendMessage("$prefix §cPlayer §6'${player.name}'§c not loaded")
        return
    }
    sender.sendMessage("$prefix Stats of player §6'${player.name}'")
    stats.stats.forEach{sender.sendMessage("$prefix §e${it.key}§f: §6${it.value}")}
}

@Command("morestats", op = true)
internal fun morestats(sender: Sender, args: Args){
    if(args.size < 3){
        sender.sendMessage("$prefix Usage: §6/morestats [player] [stat] [n] {need booster? true/false}")
        return
    }
    val player = Bukkit.getPlayer(args[0])
    if(player == null){
        sender.sendMessage("$prefix §cPlayer §6'${args[0]}'§c not found")
        return
    }
    val stat = args[1]
    val n = args[2].toDoubleOrNull()
    if(n == null){
        sender.sendMessage("$prefix §6'${args[2]}'§c not a number")
        return
    }
    val needBuster = if(args.size == 3) true else args[3].toBoolean()
    val stats = players[player.uuid]
    if(stats == null){
        sender.sendMessage("$prefix §cPlayer §6'${player.name}'§c not loaded")
        return
    }
    addStat(player.uuid, stat, n, needBuster)
    if(sender is Player)sender.sendMessage("$prefix Add §6'$n'§f in stat §6'$stat'§f to player §6'${player.name}'")
}

@Listener
internal fun join(event: PlayerJoinEvent){
    playerJoin(event.uuid)
}

@Listener
internal fun quit(event: PlayerQuitEvent){
    playerQuit(event.uuid)
}

private var task: BukkitTask? = null
private val onLoad = ArrayList<(UUID) -> Unit>()

@Load
internal fun load(plugin: Plugin){
    plugin.fieldReplacer(StatsAPI::class){plu ->
        object : Stats {
            override fun add(uuid: UUID, stat: String, n: Double, needBooster: Boolean) {
                addStat(uuid, stat, n, needBooster)
            }

            override fun get(uuid: UUID, stat: String): Double {
                return (players[uuid] ?: return 0.0).stats[stat] ?: 0.0
            }

            override fun getAll(uuid: UUID): Map<String, Double> {
                return (players[uuid] ?: return emptyMap()).stats
            }

            override fun onLoad(callback: (UUID) -> Unit) {
                players.keys.forEach(callback)
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
    plu = plugin
    spi = SPI.get(plugin, config.table, PlayerStats.serializer())
    onlinePlayers.forEach{playerJoin(it.uuid)}
    if(config.autosaveTime != -1)
        task = runTaskTimer(config.autosaveTime){
            onlinePlayers.forEach{spi.save(it.uuid, players[it.uuid] ?: return@forEach)}
        }
}

private fun addStat(uuid: UUID, stat: String, n: Double, needBooster: Boolean){
    val stats = players[uuid] ?: return
    val busted = if(needBooster) plu.booster(uuid, config.busterPrefix + stat, n) else n
    val current = stats.stats.getOrDefault(stat, 0.0)
    val new = busted + current
    updateHandlers[stat]?.forEach{caching{it(uuid, current, new)}}
    stats.stats[stat] = new
}

@Unload
internal fun unload(){
    onlinePlayers.forEach{playerQuit(it.uuid)}
    task.nonNull(BukkitTask::cancel)
}

private fun playerJoin(uuid: UUID){
    spi.load(uuid){
        players[uuid] = it ?: PlayerStats(HashMap())
        onLoad.forEach{caching{it(uuid)}?.printStackTrace()}
    }
}

private fun playerQuit(uuid: UUID){
    spi.save(uuid, players.remove(uuid) ?: return)
}

@Serializable
internal class Config(val table: String = "morestats", val busterPrefix: String = "stats_", val autosaveTime: Int = 1200)

@Serializable
internal class PlayerStats(val stats: MutableMap<String, Double>)