package cmdexec

import org.bukkit.Bukkit
import org.bukkit.command.CommandMap
import pluginloader.api.LoaderPlugin
import pluginloader.api.nonNull
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

internal class CompiledRawUnsafe(
    private val line: String,
    private val command: String,
    private val sourceMap: Array<String?>,
    private val usedInput: IntArray,
    private val mapping: List<Pair<String, KClass<*>>>
): Compiled{
    private var safeRealization: Compiled? = null

    override fun call(plugin: LoaderPlugin, input: InputArgs, strCache: StrCache) {
        safeRealization.nonNull {
            it.call(plugin, input, strCache)
            return
        }
        val cmd = commandsMap[command] ?: return
        val args = arrayOfNulls<String?>(sourceMap.size)
        System.arraycopy(sourceMap, 0, args, 0, sourceMap.size)
        usedInput.forEach{i ->
            val inp = i and 0xFF
            val str = strCache[inp]
            if(str.contains(' ')){
                safeRealization = CompiledRaw.parse(line, mapping)
                safeRealization!!.call(plugin, input, strCache)
                return
            }
            args[i shr 16 and 0xFF] = str
        }
        @Suppress("UNCHECKED_CAST")
        cmd.execute(sender, command, args as Array<String>)
    }

    override fun needStringCache(): Boolean = true

    companion object {
        fun parse(line: String, map: List<Pair<String, KClass<*>>>): Compiled {
            val split = line.split(' ')
            val argSize = split.size - 1
            var usedInputSize = 0
            repeat(argSize) arg@{i ->
                val arg = split[i + 1]
                map.forEach{pair ->
                    if(arg.contains(pair.first)){
                        if(arg.length == pair.first.length){
                            usedInputSize++
                        }else return CompiledRaw.parse(line, map)//back to safe realization
                        return@arg
                    }
                }
            }
            val sourceStrings = arrayOfNulls<String?>(argSize)
            val usedInput = IntArray(usedInputSize)
            var usedPointer = 0
            repeat(argSize) arg@{i ->
                val arg = split[i + 1]
                map.forEachIndexed{argIndex, pair ->
                    if(arg == pair.first){
                        usedInput[usedPointer++] = argIndex or (i shl 16)
                        return@arg
                    }
                }
                sourceStrings[i] = arg
            }
            if(usedPointer == 0) return CompiledJustExecUnsafe(line)
            return CompiledRawUnsafe(line, split[0], sourceStrings, usedInput, map)
        }
    }
}

internal class CompiledJustExecUnsafe(cmd: String): Compiled{
    private var badCommand = false
    private val command: String
    private val sourceArgs: Array<String>
    private var usedArgs: Array<String>

    init {
        val split = cmd.split(" ")
        command = split[0]
        sourceArgs = split.drop(1).toTypedArray()
        usedArgs = sourceArgs.copyOf()
    }

    override fun call(input: InputArgs) {
        val cmd = commandsMap[command] ?: return
        if(badCommand) usedArgs = sourceArgs.clone()
        else if(usedArgs.contentDeepEquals(sourceArgs).not()){
            usedArgs = sourceArgs.clone()
            badCommand = true
        }
        cmd.execute(sender, command, usedArgs)
    }
}

private val sender = Bukkit.getConsoleSender()
private val commandsMap by lazy{commandMap.known}
private val modifiersField by lazy {
    return@lazy try{
        val field = Field::class.java.getDeclaredField("modifiers")
        if (!field.isAccessible) field.isAccessible = true
        field
    }catch (ex: NoSuchFieldException){
        null
    }catch (ex: SecurityException){
        null
        //?..
    }
}

private fun Field.unsetFinal(): Field {
    if(modifiers and Modifier.FINAL != 0){
        modifiersField?.setInt(this, modifiers and Modifier.FINAL.inv())
    }
    isAccessible = true
    return this
}

private val commandMap: CommandMap by lazy {
    val field = Bukkit.getServer()::class.java.getDeclaredField("commandMap")
    field.unsetFinal()
    field.get(Bukkit.getServer()) as CommandMap
}

private val CommandMap.known: MutableMap<String, org.bukkit.command.Command> by lazy {
    try {
        @Suppress("UNCHECKED_CAST")
        return@lazy commandMap::class.java.getDeclaredField("knownCommands").unsetFinal()
            .get(commandMap) as MutableMap<String, org.bukkit.command.Command>
    }catch (ignore: NoSuchFieldException){
        @Suppress("UNCHECKED_CAST")//1.13+~
        return@lazy commandMap::class.java.superclass.getDeclaredField("knownCommands").unsetFinal()
            .get(commandMap) as MutableMap<String, org.bukkit.command.Command>
    }
}