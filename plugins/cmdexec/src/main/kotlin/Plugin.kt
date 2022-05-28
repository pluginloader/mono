package cmdexec

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import pluginloader.api.*
import java.lang.ref.SoftReference
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

@Serializable(CommandsSerializer::class)
class Commands(internal val commands: List<String>){
    @Transient
    private var mapping: List<Pair<String, KClass<*>>>? = null
    @Transient
    private var compiled: Compiled? = null

    fun args(mapping: List<Pair<String, KClass<*>>>){
        this.mapping = mapping
    }

    private fun compile(plugin: LoaderPlugin): Compiled {
        if (commands.isEmpty()) {
            val comp = emptyCompiled
            compiled = comp
            return comp
        }
        val map = mapping!!
        if (commands.size == 1) {
            val comp = compilePainful(plugin, commands[0], map)
            compiled = comp
            return comp
        }
        val array = Array(commands.size) { compilePainful(plugin, commands[it], map) }

        val comp = CompiledMultiple(array)
        compiled = comp
        return comp
    }

    fun exec(plugin: LoaderPlugin){
        val comp = compiled ?: compile(plugin)
        comp.call(plugin, emptyArray, emptyStrCache)
    }

    fun exec(plugin: LoaderPlugin, player: Player){
        val array = arrayOf<Any>(player)
        var comp = compiled
        if(comp == null){
            if(mapping == null) mapping = listOf("%player%" to Player::class)
            comp = compile(plugin)
        }
        execute(plugin, comp, array)
    }

    fun exec(plugin: LoaderPlugin, player: Player, replace: Replacing.() -> Unit){
        exec(plugin){replace("%player%", player);replace(this)}
    }

    fun exec(plugin: LoaderPlugin, replace: Replacing.() -> Unit){
        var comp = compiled
        val array: Array<Any>
        if(comp == null){
            val generate = GenerateReplacing()
            replace(generate)
            array = generate.list.toTypedArray()
            mapping = generate.metadata
            comp = compile(plugin)
        }else{
            val replacing = ArrayReplacing(mapping!!.size)
            replace(replacing)
            array = replacing.array.requireNoNulls()
        }
        execute(plugin, comp, array)
    }

    fun execute(plugin: LoaderPlugin, vararg args: Any){//args in theory is array<any>, maybe can unsafe cast
        val array = Array(args.size, args::get)
        val comp = compiled ?: compile(plugin)
        execute(plugin, comp, array)
    }

    private fun execute(plugin: LoaderPlugin, comp: Compiled, array: Array<Any>){
        val stringCache = DefaultStrCache(array, arrayOfNulls(array.size), mapping!!)
        comp.call(plugin, InputArgs(array), stringCache)
    }
}

fun Commands(vararg command: String): Commands{
    return Commands(command.toList())
}

interface StrCache{
    operator fun get(index: Int): String
}

interface Replacing{
    fun replace(source: String, replaceTo: String): Replacing

    fun replace(source: String, replaceTo: Number): Replacing

    fun replace(source: String, replaceTo: Player): Replacing
}

interface Compiled{
    fun call(input: InputArgs){}

    fun call(plugin: LoaderPlugin, input: InputArgs) {
        call(input)
    }

    fun call(plugin: LoaderPlugin, input: InputArgs, strCache: StrCache) {
        call(plugin, input)
    }

    fun needStringCache(): Boolean = false
}

@JvmInline
value class InputArgs internal constructor(private val array: Array<Any>){
    operator fun get(index: Int): Any = array[index]

    val size: Int get() = array.size
}

private class ArrayReplacing(size: Int): Replacing{
    val array: Array<Any?> = arrayOfNulls(size)
    var pointer = 0

    override fun replace(source: String, replaceTo: String): Replacing {
        array[pointer++] = replaceTo
        return this
    }

    override fun replace(source: String, replaceTo: Number): Replacing {
        array[pointer++] = replaceTo
        return this
    }

    override fun replace(source: String, replaceTo: Player): Replacing {
        array[pointer++] = replaceTo
        return this
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class CmdExecAPI

interface CmdExec{
    fun register(
        command: String,
        callback: (inputPlugin: LoaderPlugin, execute: String, mapping: List<Pair<String, KClass<*>>>) -> Compiled,
        vararg aliases: String
    )
}

private var debug: String? = null
private var debugPlayer: Player? = null

@Command("cmdexec", op = true)
internal fun cmd(player: Player, args: Args){
    if(debug != null){
        debug = null
        debugPlayer = null
        player.sendMessage("$prefix Debug disabled")
        return
    }
    debugPlayer = player
    debug = if(args.isEmpty()) "" else args[0]
    if(debug == ""){
        player.sendMessage("$prefix Enabled full debug, use §6/cmdexec§f to disable")
    }else{
        player.sendMessage("$prefix Enabled debug for plugins with name contains §6$debug")
    }
}

@Listener
internal fun onQuit(event: PlayerQuitEvent){
    if(event.player == debugPlayer){
        debugPlayer = null
        debug = null
    }
}

@Load
internal fun load(plugin: LoaderPlugin) {
    plugin.fieldReplacer(CmdExecAPI::class){plu ->
        object: CmdExec{
            override fun register(
                command: String,
                callback: (inputPlugin: LoaderPlugin, execute: String, mapping: List<Pair<String, KClass<*>>>) -> Compiled,
                vararg aliases: String
            ) {
                remapped[command]?.removeIf{it.get()?.dropCompiled() == null}
                nativeCompilers[command] = callback
                aliases.forEach{
                    nativeCompilers[it] = callback
                    remapped[it]?.removeIf{link -> link.get()?.dropCompiled() == null}
                }
                plu.unloadHandler{
                    remapped[command]?.removeIf{it.get()?.dropCompiled() == null}
                    nativeCompilers.remove(command)
                    aliases.forEach{
                        nativeCompilers.remove(it)
                        remapped[it]?.removeIf{link -> link.get()?.dropCompiled() == null}
                    }
                }
            }
        }
    }
}

private class GenerateReplacing: Replacing{
    val list = ArrayList<Any>()
    val metadata = ArrayList<Pair<String, KClass<*>>>()

    override fun replace(source: String, replaceTo: String): Replacing {
        list.add(replaceTo)
        metadata.add(Pair(source, String::class))
        return this
    }

    override fun replace(source: String, replaceTo: Number): Replacing {
        list.add(replaceTo)
        metadata.add(Pair(source, Number::class))
        return this
    }

    override fun replace(source: String, replaceTo: Player): Replacing {
        list.add(replaceTo)
        metadata.add(Pair(source, Player::class))
        return this
    }
}

private class CompiledMultiple(private val methods: Array<Compiled>): Compiled{
    override fun call(plugin: LoaderPlugin, input: InputArgs, strCache: StrCache) {
        methods.forEach{it.call(plugin, input, strCache)}
    }
}

private val sourceSerializer = ListSerializer(String.serializer())
private class CommandsSerializer: KSerializer<Commands> {
    override val descriptor = sourceSerializer.descriptor

    override fun deserialize(decoder: Decoder): Commands {
        return Commands(sourceSerializer.deserialize(decoder))
    }

    override fun serialize(encoder: Encoder, value: Commands) {
        sourceSerializer.serialize(encoder, value.commands)
    }
}

private val emptyArray = InputArgs(emptyArray())
private val emptyCompiled = CompiledMultiple(emptyArray())
private val emptyStrCache = object: StrCache{
    override fun get(index: Int): String {
        error("Can't be called")
    }
}
private class DefaultStrCache(
    private val array: Array<Any>,
    private val cache: Array<Any?>,
    private val mapping: List<Pair<String, KClass<*>>>,
): StrCache{
    override fun get(index: Int): String {
        val inp = cache[index]
        var str: String? = if(inp == null || inp.javaClass != String::class.java) null else inp as String
        if(str == null){
            val value = array[index]
            str = when(mapping[index].second){
                Player::class -> (value as Player).name
                else -> value.toString()
            } as String
            cache[index] = str
        }
        return str
    }
}

private fun compilePainful(plugin: LoaderPlugin, line: String, mapping: List<Pair<String, KClass<*>>>): Compiled{
    val space = line.indexOf(' ')
    val onlyCommand = space == -1 || space + 1 >= line.length
    val command = if(onlyCommand) line else line.substring(0, space)
    val comp = RemappedCompiled(plugin, line, mapping)
    remapped.computeIfAbsent(command){ArrayList()}.add(SoftReference(comp))
    return comp
}

private fun compileSingleLine(plugin: LoaderPlugin, line: String, map: List<Pair<String, KClass<*>>>): Compiled{
    val space = line.indexOf(' ')
    val onlyCommand = space == -1 || space + 1 >= line.length
    val command = if(onlyCommand) line else line.substring(0, space)
    nativeCompilers[command].nonNull{return it(plugin, if(onlyCommand) "" else line.substring(space + 1), map)}
    return CompiledRawUnsafe.parse(line, map)
}

private class RemappedCompiled(
    private val plu: LoaderPlugin,
    private val line: String,
    private val mapping: List<Pair<String, KClass<*>>>
): Compiled{
    private var compiled: Compiled? = null

    fun dropCompiled(){
        compiled = null
    }

    private fun get(): Compiled{
        if(compiled != null)return compiled as Compiled
        val comp = compileSingleLine(plu, line, mapping)
        compiled = comp
        return comp
    }

    override fun call(plugin: LoaderPlugin, input: InputArgs, strCache: StrCache) {
        if(debug != null && plugin.name.contains(debug!!)){
            debugPlayer!!.sendMessage("§8[§cD§8]§f Plu §6${plugin.name}§f:§7 $line")
        }
        get().call(plugin, input, strCache)
    }

    override fun needStringCache(): Boolean {
        return get().needStringCache()
    }
}

private val remapped = HashMap<String, ArrayList<SoftReference<RemappedCompiled>>>()
private val nativeCompilers = HashMap<String, (LoaderPlugin, String, List<Pair<String, KClass<*>>>) -> Compiled>()