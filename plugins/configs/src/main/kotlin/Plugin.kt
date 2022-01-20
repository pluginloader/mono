package configs

import com.charleskorn.kaml.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.nodes.*
import pluginloader.api.*
import java.io.File
import java.lang.StringBuilder
import java.nio.file.Files

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigsAPI

interface Configs{
    fun <T> readConfig(serializer: KSerializer<T>): T?

    fun <T> writeConfig(serializer: KSerializer<T>, value: T)

    fun supportInline(callback: ((key: String, value: (String) -> String) -> Unit) -> Unit)

    fun useInline(text: String): String
}

@Serializable
private data class Config(
    val dir: String = "conf",
    val yaml: Boolean = false,
    val uniquePaths: Map<String, String> = emptyMap(),
    val inlineDirs: List<String> = listOf(),
    val asideDirs: List<String> = listOf(),
)
private var config: Config = Config()
private val inlines = HashMap<String, (String) -> String>()
private val inlineSources = HashMap<String, Pair<() -> Unit, List<String>>>()

private fun conf(name: String, extension: String): String? {
    val file = getConf(name, extension)
    if(!file.exists())return null
    return file.readText()
}

private fun conf(name: String, extension: String, text: String) {
    val file = getConf(name, extension)
    if(!file.exists()){
        Files.createDirectories(file.parentFile.toPath())
        Files.createFile(file.toPath())
    }
    file.writeText(text)
}

private fun getConf(name: String, extension: String): File {
    config.uniquePaths[name]?.apply {
        val file = File(this)
        return if(file.isDirectory){
            File(file, "$name.$extension")
        } else {
            file
        }
    }
    val normalFile = File("${config.dir}/${name}.$extension")
    config.asideDirs.forEach{
        val file = File("$it/${name}.$extension")
        if(file.exists()){
            if(normalFile.exists()){
                val rename = File("${config.dir}/${name}.$extension.aside_removed")
                normalFile.renameTo(rename)
            }
            return file
        }
    }
    return normalFile
}

@InternalSerializationApi
@Load
internal fun load(plugin: LoaderPlugin){
    config = plugin.readConfig(Config.serializer()){config}
    plugin.fieldReplacer<Conf, Any>(Conf::class){loaderPlugin, _, conf ->
        try {
            val serializer = conf::class.serializer()
            val value = loaderPlugin.readConfig(serializer)
            if (value == null) {
                @Suppress("UNCHECKED_CAST")
                loaderPlugin.writeConfig(serializer as KSerializer<Any>, conf)
                return@fieldReplacer conf
            }
            return@fieldReplacer value
        }catch (ex: Throwable){
            throw object: java.lang.Throwable("Plugin: ${loaderPlugin.name} failed load config ${ex.message}", null, false, false){} as Throwable
        }
    }
    File(config.dir).mkdirs()
    config.inlineDirs.forEach{
        val file = File(it)
        if(!file.exists() || !file.isDirectory)return@forEach
        val nameStart = if(it.endsWith("/")) it.substring(0, it.length - 1) else it
        file.listFiles()!!.forEach inline@{inlineFile ->
            if(inlineFile.endsWith(".yml").not())return@inline
            readInline(nameStart + "/" + inlineFile.name, inlineFile)
        }
    }
    caching {//old versions support
        plugin.fieldReplacer(ConfigsAPI::class){plu -> object: Configs{
            override fun <T> readConfig(serializer: KSerializer<T>): T? {
                return plu.readConfig(serializer)
            }

            override fun <T> writeConfig(serializer: KSerializer<T>, value: T) {
                plu.writeConfig(serializer, value)
            }

            override fun supportInline(callback: ((String, (String) -> String) -> Unit) -> Unit) {
                val list = ArrayList<String>()
                fun loadInl(){
                    callback{key, value ->
                        list.add(key)
                        inlines[key] = value
                    }
                }
                loadInl()
                inlineSources["plu/" + plu.name] = Pair(::loadInl, list)
            }

            override fun useInline(text: String): String {
                return preprocessYml(text)
            }
        }}
        plugin.cmd("configs", {sender, args ->
            val help = "Usage: /configs [inline]"
            if(args.isEmpty()){
                sender.sendMessage(help)
                return@cmd
            }
            when(args[0].lowercase()){
                "inline" -> {
                    if(args.size == 1){
                        sender.sendMessage("Usage: /configs inline [name]")
                        return@cmd
                    }
                    val pair = inlineSources.remove(args[1])
                    if(pair == null){
                        sender.sendMessage("Can't find source ${args[1]}")
                        return@cmd
                    }
                    pair.second.forEach{inline -> inlines.remove(inline)}
                    pair.first()
                    sender.sendMessage("Reloaded inline source")
                }
                else -> sender.sendMessage(help)
            }
        })
    }
}

private fun readInline(name: String, file: File){
    caching{
        val json = yamlToJson(file.readText())
        inlineSources[name] = Pair({readInline(name, file)}, ArrayList(json.keys))
        json.forEach{entry ->
            inlines[entry.key] = {entry.value.toString()}
        }
    }?.printStackTrace()
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Conf

private val json = Json{
    prettyPrint = true
    encodeDefaults = true
}

private val YAML = Yaml(configuration = YamlConfiguration(extensionDefinitionPrefix = "x-", strictMode = true))

fun <T> LoaderPlugin.writeConfig(serializer: KSerializer<T>, value: T){
    if(config.yaml)conf(this.name, "yml", YAML.encodeToString(serializer, value))
    else conf(this.name, "json", json.encodeToString(serializer, value))
}

inline fun <reified T> LoaderPlugin.readConfig(serializer: KSerializer<T>, default: () -> T): T{
    val value = readConfig(serializer)
    if(value == null){
        val init = default()
        writeConfig(serializer, init)
        return init
    }
    return value
}

private fun preprocessYml(yaml: String): String{
    val editedYaml: String
    if(!yaml.contains('<'))editedYaml = yaml
    else {
        val builder = StringBuilder(yaml.length)
        val buffer = StringBuilder()
        var bufferStarted = false
        yaml.toCharArray().forEach {
            if (bufferStarted) {
                if (it == '\n' || it == '\r') {
                    builder.append('<').append(buffer).append(it)
                    bufferStarted = false
                    buffer.clear()
                    return@forEach
                }
                if (it == '>') {
                    val buf = buffer.toString()
                    val split = buf.split("-", limit = 2)
                    val inline = inlines[split[0]]
                    if (inline == null) {
                        builder.append('<').append(buf).append('>')
                    } else {
                        builder.append(inline(if(split.size == 1) "" else split[1]))
                    }
                    buffer.clear()
                    bufferStarted = false
                } else {
                    buffer.append(it)
                }
            } else {
                if (it == '<') {
                    bufferStarted = true
                    return@forEach
                }
                builder.append(it)
            }
        }
        editedYaml = builder.toString()
    }
    return editedYaml
}

fun <T> LoaderPlugin.readConfig(serializer: KSerializer<T>): T?{
    val text = conf(this.name, "json")
    if(config.yaml){
        if(text == null){
            val yaml = conf(this.name, "yml") ?: return null
            return YAML.decodeFromString(serializer, preprocessYml(yaml))
        }else{
            getConf(this.name, "json").apply{renameTo(File(parentFile, "$name.backup"))}
            val value = Json.decodeFromString(serializer, text)
            writeConfig(serializer, value)
            return value
        }
    }else{
        if(text == null)return null
    }
    return Json.decodeFromString(serializer, text)
}

private val rawYAML = org.snakeyaml.engine.v2.api.Load(LoadSettings.builder().build())

private fun yamlToJson(string: String): JsonObject{
    fun encodeToJson(node: Any?): JsonElement{
        return when(node){
            null -> JsonNull
            is String -> JsonPrimitive(node)
            is Number -> JsonPrimitive(node)
            is Boolean -> JsonPrimitive(node)
            is Map<*, *> -> {
                val map = HashMap<String, JsonElement>()
                node.forEach{map[it.key.toString()] = encodeToJson(it.value)}
                JsonObject(map)
            }
            is List<*> -> {
                val list = ArrayList<JsonElement>()
                node.forEach{list.add(encodeToJson(it))}
                JsonArray(list)
            }
            else -> error("Unknown subclass $node")
        }
    }
    return encodeToJson(rawYAML.loadFromString(string)) as JsonObject
}