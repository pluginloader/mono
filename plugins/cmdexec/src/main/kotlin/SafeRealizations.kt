package cmdexec

import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import pluginloader.api.LoaderPlugin
import java.lang.StringBuilder
import kotlin.reflect.KClass

internal class CompiledRaw(
    private val strArray: Array<String>,
    private val usedInput: IntArray,
    private val toString: IntArray,
): Compiled{
    private val preSize = strArray.sumOf{it.length}

    override fun call(plugin: LoaderPlugin, input: InputArgs, strCache: StrCache) {
        var calcSize = 0
        toString.forEach{i ->
            calcSize += strCache[i].length
        }
        val buf = StringBuilder(preSize + calcSize)
        strArray.forEachIndexed{ index, str ->
            buf.append(str)
            val i = usedInput[index]
            if(i == -1)return@forEachIndexed
            buf.append(strCache[i])
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), buf.toString())
    }

    override fun needStringCache(): Boolean = true

    companion object{
        fun parse(line: String, map: List<Pair<String, KClass<*>>>): Compiled{
            val gets = ArrayList<Triple<Int, Int, Int>>()//i, where find, size
            map.forEachIndexed{i, it ->
                var startFind = 0
                while (true) {
                    val index = line.indexOf(it.first, startFind)
                    if (index == -1) return@forEachIndexed
                    gets.add(Triple(i, index, it.first.length))
                    startFind = index + it.first.length
                }
            }
            if(gets.isEmpty()){
                return CompiledJustExec(line)
            }
            gets.sortBy{it.second}
            val list = ArrayList<String>()
            val input = ArrayList<Int>()
            val inputToString = HashSet<Int>(map.size)
            var latestEnd = 0
            gets.forEach{triple ->
                input.add(triple.first)
                inputToString.add(triple.first)
                val end = triple.second
                list.add(line.substring(latestEnd, end))
                latestEnd = end + triple.third
            }
            if(latestEnd != line.length){
                list.add(line.substring(latestEnd, line.length))
                input.add(-1)
            }
            return CompiledRaw(list.toTypedArray(), input.toIntArray(), inputToString.toIntArray())
        }
    }
}

internal class CompiledJustExec(private val command: String): Compiled{
    override fun call(input: InputArgs) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
    }
}