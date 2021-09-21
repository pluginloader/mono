package donate

import pluginloader.api.Load
import pluginloader.api.LoaderPlugin
import provide.Provider
import provide.registerProvider
import java.util.*

fun interface Donate: (UUID, String, Int, () -> Unit) -> Unit{
    companion object: (UUID, String, Int, () -> Unit) -> Unit{
        override fun invoke(uuid: UUID, decription: String, price: Int, ok: () -> Unit) {
            donate.actual(uuid, decription, price, ok)
        }
    }
}

fun interface DonateWithError: (UUID, String, Int, () -> Unit, () -> Unit) -> Unit{
    companion object: (UUID, String, Int, () -> Unit, () -> Unit) -> Unit{
        override fun invoke(uuid: UUID, decription: String, price: Int, ok: () -> Unit, error: () -> Unit) {
            donateWithError.actual(uuid, decription, price, ok, error)
        }
    }
}

fun interface Coupon: (UUID, Int) -> Int{
    companion object: (UUID, Int) -> Int{
        override fun invoke(uuid: UUID, price: Int): Int = coupon.actual(uuid, price)
    }
}

private lateinit var donate: Provider<Donate>
private lateinit var donateWithError: Provider<DonateWithError>
private lateinit var coupon: Provider<Coupon>

@Load
internal fun load(plugin: LoaderPlugin){
    donate = plugin.registerProvider(Donate::class)
    donateWithError = plugin.registerProvider(DonateWithError::class)
    coupon = plugin.registerProvider(Coupon::class)
}