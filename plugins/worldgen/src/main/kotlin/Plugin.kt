package worldgen

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.scheduler.BukkitTask
import pluginloader.api.Command
import pluginloader.api.Config
import pluginloader.api.Unload
import pluginloader.api.runTaskLater

private val list = ArrayList<BukkitTask>()

private const val prefix = "§8[§aPlu§8]§f"

@Command("worldgen")
internal fun command(sender: CommandSender, args: Array<String>){
    if(!sender.isOp)return
    if(args.size < 5){
        sender.sendMessage("$prefix Usage: §6/worldgen [World] [Start x] [Start z] [End x] [End z] {chunk per tick}")
        return
    }
    val world = Bukkit.getWorld(args[0])
    val startX = args[1].toInt()
    val startZ = args[2].toInt()
    val endX = args[3].toInt()
    val endZ = args[4].toInt()
    val chunkStartX = startX / 16
    val chunkStartZ = startZ / 16
    val chunkEndX = endX / 16
    val chunkEndZ = endZ / 16
    var count = 0
    var ticks = 0
    val chunksPerTick = if(args.size == 5) 10 else args[5].toInt()
    for(chunkX in chunkStartX..chunkEndX){
        for(chunkZ in chunkStartZ..chunkEndZ){
            if(world.isChunkGenerated(chunkX, chunkZ))continue
            count++
            list.add(runTaskLater(ticks, GenerateTask(world, chunkX, chunkZ)))
            if(count == chunksPerTick){
                count = 0
                ticks++
            }
        }
    }
    println("$prefix Time to generate: §6${ticks / 20}§f seconds")
}

@Unload
internal fun unload(){
    list.forEach{if(!it.isCancelled)it.cancel()}
}

private class GenerateTask(private val world: World, private val x: Int, private val z: Int): () -> Unit{
    override fun invoke() {
        world.regenerateChunk(x, z)
    }
}