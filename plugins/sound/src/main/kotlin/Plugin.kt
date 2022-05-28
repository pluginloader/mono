package sound

import pluginloader.api.*

@Command("sound", op = true)
internal fun sound(sender: Sender, args: Args){
    args.use(sender, 4, "sound [player] [sound] [volume] [pitch]")
    val player = args.player(sender, 0) ?: return
    val volume = args.float(sender, 2) ?: return
    val pitch = args.float(sender, 3) ?: return
    player.playSound(player.location, args[1], volume, pitch)
}