package money

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import pluginloader.api.Load
import pluginloader.api.Plugin
import provide.Provider
import provide.registerProvider

interface Money{
    fun get(player: OfflinePlayer): Double

    fun has(player: OfflinePlayer, money: Double): Boolean

    fun withdraw(player: OfflinePlayer, money: Double)

    fun deposit(player: OfflinePlayer, money: Double)

    companion object{
        fun get(player: OfflinePlayer): Double{
            return provider.actual.get(player)
        }

        fun has(player: OfflinePlayer, money: Double): Boolean{
            return provider.actual.has(player, money)
        }

        fun withdraw(player: OfflinePlayer, money: Double){
            provider.actual.withdraw(player, money)
        }

        fun deposit(player: OfflinePlayer, money: Double){
            provider.actual.deposit(player, money)
        }
    }
}

private lateinit var provider: Provider<Money>

@Load
internal fun load(plugin: Plugin){
    provider = plugin.registerProvider(Money::class, VaultMoney())
}

private class VaultMoney: Money{
    private val vault = Bukkit.getServicesManager().getRegistration(Economy::class.java)

    override fun get(player: OfflinePlayer): Double = vault.provider.getBalance(player)

    override fun has(player: OfflinePlayer, money: Double) = vault.provider.has(player, money)

    override fun withdraw(player: OfflinePlayer, money: Double) {
        vault.provider.withdrawPlayer(player, money)
    }

    override fun deposit(player: OfflinePlayer, money: Double) {
        vault.provider.depositPlayer(player, money)
    }
}