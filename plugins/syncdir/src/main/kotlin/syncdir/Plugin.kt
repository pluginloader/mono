package syncdir

import configs.Conf
import kotlinx.serialization.Serializable
import pluginloader.api.*
import java.io.File

@Conf
internal val config = Config()

@Command("syncdir", op = true)
internal fun sync(sender: Sender, args: Args){
    if(args.isEmpty()){
        sender.sendMessage("${Color.ORANGE}Usage: /syncdir [type]")
        return
    }
    config.syncDirs[args[0]].nonNull {
        val from = File(it.from)
        if(!from.isDirectory)from.copyTo(File(it.to), true)
        else File(it.from).copyRecursively(File(it.to), true)
        sender.sendMessage("${Color.ORANGE}Copied!")
    }
}

@Serializable
internal class Config(val syncDirs: Map<String, SyncDir> = mapOf("example" to SyncDir()))

@Serializable
internal class SyncDir(val from: String = "example2", val to: String = "example3")