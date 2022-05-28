package gui

import cmdexec.Commands
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import pluginloader.api.*

@Serializable
data class ConfigInventory(
    val title: String = "Inventory",
    val context: List<String> = listOf("xxxxxxxxx", "---------", "xxxxxxxxx"),
    val auto: Map<Char, Item> = mapOf('0' to Item.default()),
    val autoCmd: Map<Char, List<String>> = mapOf('0' to listOf("!cmd %player%"))
){
    constructor(title: String = "Inventory",
                context: List<String> = listOf("xxxxxxxxx", "---------", "xxxxxxxxx"),
                auto: Map<Char, Item> = mapOf('0' to Item.default())) : this(title, context, auto, HashMap())

    fun active(): ActiveInventory{
        return active(title)
    }

    fun active(title: String): ActiveInventory{
        val size = context[0].length * context.size
        val inventory = Bukkit.createInventory(null, size, title)
        val active = ActiveInventory(inventory, context, size)
        auto.forEach{
            val commands = autoCmd[it.key]
            val compile = if(commands == null) null else Commands(commands)
            active.fill(it.key, it.value.item()){event -> compile?.exec(plu, event.whoClicked as Player)}
        }
        return active
    }
}

class ActiveInventory(private val inventory: Inventory, context: List<String>, size: Int){
    private val chars = CharArray(size)
    private val actions = Array<((InventoryClickEvent) -> Unit)?>(size){null}
    private val filled = BooleanArray(size)
    private var outsideClick: (InventoryClickEvent) -> Unit = {}

    init{
        var i = 0
        context.forEach{it.forEach{ c -> chars[i++] = c}}
    }

    internal fun event(event: InventoryClickEvent){
        if(event.rawSlot < 0 || event.rawSlot >= chars.size){
            outsideClick(event)
            return
        }
        actions[event.rawSlot]?.invoke(event)
    }

    fun open(player: Player){
        player.openInventory(inventory)
        opened[inventory] = this
    }

    fun fill(char: Char, stack: ItemStack, action: ((InventoryClickEvent) -> Unit)? = null) = set(char, stack, action)
    fun add(char: Char, stack: ItemStack, action: ((InventoryClickEvent) -> Unit)? = null) = set(char, stack, action){return}

    fun outside(action: ((InventoryClickEvent) -> Unit)){
        outsideClick = action
    }

    private inline fun set(char: Char, stack: ItemStack, noinline action: ((InventoryClickEvent) -> Unit)?, find: () -> Unit = {}){
        chars.forEachIndexed{index, c ->
            if(c != char)return@forEachIndexed
            if(filled[index])return@forEachIndexed
            filled[index] = true
            action.nonNull{actions[index] = it}
            inventory.setItem(index, stack)
            find()
        }
    }
}

private val opened = HashMap<Inventory, ActiveInventory>()

@Plu
private lateinit var plu: LoaderPlugin

@Load
internal fun Plugin.load(){
    listener<InventoryCloseEvent>{runTaskLater(1){if(viewers.isEmpty()) opened.remove(inventory)}}
    listener<InventoryClickEvent>{
        val opened = opened[inventory] ?: return@listener
        cancel()
        opened.event(this)
    }
    listener<InventoryDragEvent>{if(opened.containsKey(inventory))cancel()}
}