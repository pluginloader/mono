package configs

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import pluginloader.api.Load
import pluginloader.api.LoaderPlugin
import java.io.File
import java.nio.file.Files

@Serializable
private data class Config(val dir: String = "conf", val uniquePaths: Map<String, String> = emptyMap(), val yaml: Boolean = false)
private var config: Config = Config()

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
    return File("${config.dir}/${name}.$extension")
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

fun <T> LoaderPlugin.readConfig(serializer: KSerializer<T>): T?{
    val text = conf(this.name, "json")
    if(config.yaml){
        if(text == null){
            val yaml = conf(this.name, "yml") ?: return null
            return YAML.decodeFromString(serializer, yaml)
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