package removeitem

import pluginloader.api.*

@Command("removeitem", op = true)
internal fun cmd(sender: Sender, args: Args){
    args.use(sender, 2, "removeitem [player] [amount]") ?: return
    (args.player(sender, 0) ?: return).inventory.itemInMainHand.amount -= args.int(sender, 1) ?: return
}