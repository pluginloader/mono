package text

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pluginloader.api.*
import provide.Provider
import provide.registerProvider
import java.util.concurrent.CompletableFuture
import kotlin.collections.ArrayList

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class TextAPI

interface Text{
    fun buildMessage(text: String): CompletableFuture<List<TextComponent>>

    fun componentReplacer(replacer: (String) -> Triple<String, BaseComponent, String>?)
}

fun interface Broadcast: (String) -> Unit{
    companion object{
        operator fun invoke(str: String) = broadcast.actual(str)
    }
}

fun interface BroadcastLocal: (String) -> Unit{
    companion object{
        operator fun invoke(str: String) = broadcastLocal.actual(str)
    }
}

fun interface BroadcastChat: (String, Player) -> Unit{
    companion object{
        operator fun invoke(str: String, player: Player) = broadcastChat.actual(str, player)
    }
}

@Load
internal fun Plugin.load() {
    broadcast = registerProvider(Broadcast::class, Broadcast{str ->
        buildMessageInternal(str).whenComplete{ it, _ -> it.forEach{onlinePlayers.forEach{ p -> p.sendMessage(it)}}}
    })
    broadcastLocal = registerProvider(BroadcastLocal::class, BroadcastLocal{str ->
        buildMessageInternal(str).whenComplete{ it, _ -> it.forEach{onlinePlayers.forEach{ p -> p.sendMessage(it)}}}
    })
    broadcastChat = registerProvider(BroadcastChat::class, BroadcastChat{str, _ ->
        buildMessageInternal(str).whenComplete{ it, _ -> it.forEach{onlinePlayers.forEach{ p -> p.sendMessage(it)}}}
    })

    fieldReplacer(TextAPI::class){plu -> object: Text{
        override fun buildMessage(text: String): CompletableFuture<List<TextComponent>> {
            return buildMessageInternal(text)
        }

        override fun componentReplacer(replacer: (String) -> Triple<String, BaseComponent, String>?) {
            componentReplacer.add(replacer)
            plu.unloadHandler{componentReplacer.remove(replacer)}
        }
    }}

    command(true, name = "broad"){_, args -> Broadcast(args.joinToString(separator = " "))}
    command(true, name = "broadlocal"){_, args -> BroadcastLocal(args.joinToString(separator = " "))}

    command(true, name = "text"){sender, args ->
        args.use(sender, 2, "text [player] [msg]") ?: return@command
        val player = args.player(sender, 0) ?: return@command
        buildMessageInternal(args.drop(1).joinToString(" ")).whenComplete{ list, _ -> list.forEach{player.sendMessage(it)}}
    }
    command(true, name = "chatmsg"){sender, args ->
        args.use(sender, 2, "chatmsg [player] [msg]") ?: return@command
        val player = args.player(sender, 0) ?: return@command
        val str = args.drop(1).joinToString(" ").replace("&", "§")
        BroadcastChat(str, player)
    }
}

private lateinit var broadcast: Provider<Broadcast>
private lateinit var broadcastLocal: Provider<BroadcastLocal>
private lateinit var broadcastChat: Provider<BroadcastChat>

private val componentReplacer = ArrayList<(String) -> Triple<String, BaseComponent, String>?>()
private fun buildComponent(inputText: String): CompletableFuture<TextComponent>{
    val text = inputText.replace('&', '§')
    if(componentReplacer.isEmpty()) {
        return CompletableFuture.completedFuture(TextComponent(text))
    }
    val future = CompletableFuture<TextComponent>()
    runAsync {
        val component = TextComponent()
        component.extra = ArrayList()
        fun use(str: String){
            componentReplacer.forEach{
                val triple = it(str) ?: return@forEach
                use(triple.first)
                component.addExtra(triple.second)
                use(triple.third)
                return
            }
            component.extra.add(TextComponent(str))
        }
        use(text)
        runTask{future.complete(component)}
    }
    return future
}

private fun buildMessageInternal(text: String): CompletableFuture<List<TextComponent>>{
    if(text.contains('\n')){
        val future = CompletableFuture<List<TextComponent>>()
        val split = text.split('\n')
        val components = arrayOfNulls<CompletableFuture<TextComponent>>(split.size)
        text.split('\n').forEachIndexed{i, it -> components[i] = buildComponent(it)}
        CompletableFuture.allOf(*components).whenComplete{_, _ -> components.map{it!!.get()}}
        return future
    }else{
        val future = buildComponent(text)
        return if(future.isDone){
            CompletableFuture.completedFuture(listOf(future.get()))
        }else{
            val ret = CompletableFuture<List<TextComponent>>()
            future.whenComplete{it, _ -> ret.complete(listOf(it))}
            ret
        }
    }
}