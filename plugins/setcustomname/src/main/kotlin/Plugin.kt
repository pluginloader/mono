package setcustomname

import playerinfo.PlayerReadable
import pluginloader.api.*

@Command("setcustomname", op = true)
internal fun command(sender: Sender, args: Args){
    args.use(sender, 2, "setcustomname [player] [<readable>\\nlol]") ?: return
    val player = args.player(sender, 0) ?: return
    runAsync {
        val readable = PlayerReadable(player.uuid)
        runTask {
            player.customName = args.drop(1).joinToString(" ").replace("\\n", "\n").replace("<readable>", readable)
        }
    }
}