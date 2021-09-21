package provide

import pluginloader.api.LoaderPlugin
import kotlin.reflect.KClass

private val map = HashMap<KClass<out Any>, Provider<out Any>>()

inline fun <reified T: Any> provide() = provide(T::class)

fun <T: Any> provide(kClass: KClass<T>): Provider<T>{
    @Suppress("UNCHECKED_CAST")
    return (map[kClass] ?: error("Provider not found $kClass")) as Provider<T>
}

inline fun <reified T: Any> LoaderPlugin.registerProvider(impl: T){
    registerProvider(T::class, impl)
}

fun <T: Any> LoaderPlugin.registerProvider(clazz: KClass<T>, impl: T): Provider<T>{
    val provider = registerProvider(clazz)
    provider.add(impl)
    unloadHandler{provider.remove(impl)}
    return provider
}

fun <T: Any> LoaderPlugin.registerProvider(clazz: KClass<T>): Provider<T>{
    val provider = map[clazz]
    if(provider != null){
        @Suppress("UNCHECKED_CAST")
        return provider as Provider<T>
    }
    val provide = ProviderImpl<T>()
    map[clazz] = provide
    unloadHandler{map.remove(clazz)}
    return provide
}

interface Provider<T>{
    fun add(provider: T)
    fun remove(provider: T)

    val actualOrNull: T?
    val actual: T get() = actualOrNull ?: error("Provider not found")
    val all: Collection<T>
}

private class ProviderImpl<T> : Provider<T>{
    private val providers = ArrayList<T>()

    override fun add(provider: T) {
        providers.add(provider)
    }

    override fun remove(provider: T) {
        providers.remove(provider)
    }

    override val actualOrNull: T? get() = if(providers.isEmpty()) null else providers[providers.size - 1]
    override val all: Collection<T> get() = providers
}