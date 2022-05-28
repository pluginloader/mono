package worldgen

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.scheduler.BukkitTask
import pluginloader.api.*

private val list = ArrayList<BukkitTask>()

@Command("worldgen", op = true)
internal fun command(sender: Sender, args: Args){
    if(args.size < 5){
        sender.sendMessage("$prefix Usage: §6/")
        return
    }
    args.use(sender, 5, "worldgen [World] [Start x] [Start z] [End x] [End z] {chunk per tick}") ?: return
    val world = args.cantFind(sender, Bukkit.getWorld(args[0]), "world", args[0]) ?: return
    val startX = args.int(sender, 1) ?: return
    val startZ = args.int(sender, 2) ?: return
    val endX = args.int(sender, 3) ?: return
    val endZ = args.int(sender, 4) ?: return
    val chunkStartX = startX / 16
    val chunkStartZ = startZ / 16
    val chunkEndX = endX / 16
    val chunkEndZ = endZ / 16
    var count = 0
    var ticks = 0
    val chunksPerTick = if(args.size == 5) 10 else args.int(sender, 5) ?: return
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
    sender.sendMessage("$prefix Time to generate: §6${ticks / 20}§f seconds")
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