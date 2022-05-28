package ksyml

import com.charleskorn.kaml.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import org.snakeyaml.engine.v2.api.LoadSettings
import pluginloader.api.LoaderPlugin
import pluginloader.api.throwReadable
import java.io.File

val yaml = Yaml(configuration = YamlConfiguration(extensionDefinitionPrefix = "x-", strictMode = true))

fun <T> LoaderPlugin.decodeYaml(yml: String, serializer: KSerializer<T>, file: String): T{
    try {
        return yaml.decodeFromString(serializer, yml)
    }catch (ex: Throwable){
        throwReadable("§c$file\n§7${ex.message}", ex)
    }
}

fun <T> LoaderPlugin.decodeYaml(file: File, serializer: KSerializer<T>): T{
    return decodeYaml(file.readText(), serializer, file.name)
}

fun LoaderPlugin.yamlToJson(yaml: String): JsonObject {
    fun encodeToJson(node: Any?): JsonElement{
        return when(node){
            null -> JsonNull
            is String -> JsonPrimitive(node)
            is Number -> JsonPrimitive(node)
            is Boolean -> JsonPrimitive(node)
            is Map<*, *> -> {
                val map = HashMap<String, JsonElement>(node.size)
                node.forEach{map[it.key.toString()] = encodeToJson(it.value)}
                JsonObject(map)
            }
            is List<*> -> {
                val list = ArrayList<JsonElement>(node.size)
                node.forEach{list.add(encodeToJson(it))}
                JsonArray(list)
            }
            else -> error("Unknown subclass $node")
        }
    }
    return encodeToJson(rawYAML.loadFromString(yaml)) as JsonObject
}

private val rawYAML = org.snakeyaml.engine.v2.api.Load(LoadSettings.builder().build())