package chancecmd

import cmdexec.Commands
import configs.conf
import kotlinx.serialization.Serializable
import pluginloader.api.*
import kotlin.random.Random

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    command(true, name = "chancecmd"){sender, args ->
        args.use(sender, 3, "chancecmd [player] [type] [chance 0.0-100.0]") ?: return@command
        val player = args.player(sender, 0) ?: return@command
        val cmd = args.cantFind(sender, config.mapping[args[1]], "mapping", args[1]) ?: return@command
        val chance = args.double(sender, 2) ?: return@command
        if(chance <= Random.nextDouble(0.0, 100.0))return@command
        cmd.exec(this, player)
    }
}

@Serializable
internal class Config{val mapping = mapOf("type" to Commands("text %player% random!"))}