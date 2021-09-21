package cuboid

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.bukkit.Location
import org.bukkit.entity.Player
import pluginloader.api.V3

@Serializable
class Cuboid(
    val one: V3,
    val two: V3,
){
    @Transient
    private val minX = minOf(one.x, two.x)
    @Transient
    private val minY = minOf(one.y, two.y)
    @Transient
    private val minZ = minOf(one.z, two.z)
    @Transient
    private val maxX = maxOf(one.x, two.x)
    @Transient
    private val maxY = maxOf(one.y, two.y)
    @Transient
    private val maxZ = maxOf(one.z, two.z)

    operator fun contains(player: Player): Boolean{
        return contains(player.location)
    }

    operator fun contains(loc: Location): Boolean{
        return loc.x in minX..maxX && loc.y in minY..maxY && loc.z in minZ..maxZ
    }
}