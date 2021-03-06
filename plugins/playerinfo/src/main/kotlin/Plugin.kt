package playerinfo

import configs.conf
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import pluginloader.api.Load
import pluginloader.api.Plugin
import provide.Provider
import provide.registerProvider
import java.util.*

fun interface PlayerReadable{
    fun readable(uuid: UUID): String

    companion object{
        operator fun invoke(uuid: UUID) = full.actual.readable(uuid)
    }
}

fun interface PlayerNick{
    fun nick(uuid: UUID): String?

    companion object{
        operator fun invoke(uuid: UUID) = nick.actual.nick(uuid)
    }
}

fun interface PlayerColor{
    fun color(uuid: UUID): String

    companion object{
        operator fun invoke(uuid: UUID) = color.actual.color(uuid)
    }
}

fun interface PlayerPrefix{
    fun prefix(uuid: UUID): String

    companion object{
        operator fun invoke(uuid: UUID) = prefix.actual.prefix(uuid)
    }
}

fun interface PlayerSuffix {
    fun suffix(uuid: UUID): String

    companion object{
        operator fun invoke(uuid: UUID) = suffix.actual.suffix(uuid)
    }
}

fun interface PlayerUUIDByNick: (String) -> UUID?{
    companion object{
        operator fun invoke(nick: String) = uuidByNick.actual(nick)
    }
}

private lateinit var full: Provider<PlayerReadable>
private lateinit var nick: Provider<PlayerNick>
private lateinit var color: Provider<PlayerColor>
private lateinit var prefix: Provider<PlayerPrefix>
private lateinit var suffix: Provider<PlayerSuffix>
private lateinit var uuidByNick: Provider<PlayerUUIDByNick>

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    full = registerProvider(PlayerReadable::class, PlayerReadable { uuid ->
        val color = PlayerColor(uuid)
        val prefix = PlayerPrefix(uuid)
        val suffix = PlayerSuffix(uuid)
        "${if (prefix.isEmpty()) "" else "$prefix "}${color.ifEmpty{config.defaultColor}}${PlayerNick(uuid)}${if (suffix.isEmpty()) "" else " $suffix"}"
    })
    nick = registerProvider(PlayerNick::class, PlayerNick{Bukkit.getPlayer(it)?.name})
    color = registerProvider(PlayerColor::class, PlayerColor{config.defaultColor})
    prefix = registerProvider( PlayerPrefix::class, PlayerPrefix{""})
    suffix = registerProvider(PlayerSuffix::class, PlayerSuffix{""})
    uuidByNick = registerProvider(PlayerUUIDByNick::class, object: PlayerUUIDByNick{
        override fun invoke(nick: String): UUID? {
            Bukkit.getOfflinePlayers().forEach{if(it.name == nick)return it.uniqueId}
            return null
        }
    })
}

@Serializable
internal class Config{val defaultColor = "??7"}

