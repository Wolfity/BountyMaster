package me.wolfity.bountymaster

import me.wolfity.bountymaster.commands.BountyBoardCommand
import me.wolfity.bountymaster.commands.BountyCommand
import me.wolfity.bountymaster.commands.BountyHistoryCommand
import me.wolfity.bountymaster.commands.UserCommandParameter
import me.wolfity.bountymaster.commands.UserParameterType
import me.wolfity.bountymaster.db.DatabaseManager
import me.wolfity.bountymaster.listeners.BountyListener
import me.wolfity.bountymaster.manager.BountyManager
import me.wolfity.bountymaster.tasks.BountyHealthCheckTask
import me.wolfity.developmentutil.ext.registerListener
import me.wolfity.developmentutil.files.CustomConfig
import me.wolfity.developmentutil.gui.GUIListener
import me.wolfity.developmentutil.player.PlayerManager
import me.wolfity.developmentutil.player.PlayerRegistryListener
import me.wolfity.developmentutil.sql.PlayerRegistry
import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.java.JavaPlugin
import revxrsal.commands.Lamp
import revxrsal.commands.bukkit.BukkitLamp
import revxrsal.commands.bukkit.actor.BukkitCommandActor


lateinit var plugin: BountyMaster

class BountyMaster : JavaPlugin() {

    companion object {
        // TODO after release
//        const val RESOURCE_ID = -1
    }

    private lateinit var _playerRegistry: PlayerManager
    private lateinit var lamp: Lamp<BukkitCommandActor>
    lateinit var dbConfig: CustomConfig

    val playerRegistry: PlayerManager
        get() = _playerRegistry

    private lateinit var _bountyManager: BountyManager

    val bountyManager: BountyManager
        get() = _bountyManager

    private lateinit var _economy: Economy
    val economy: Economy
        get() = _economy

    override fun onEnable() {
        plugin = this

        loadFiles()
        DatabaseManager.init()

        setupLamp()

        registerManagers()
        registerCommands()
        registerListeners()

        BountyHealthCheckTask().runTaskTimerAsynchronously(this, 20L, 20L)
    }

    override fun onDisable() {
    }

    private fun registerCommands() {
        lamp.register(BountyCommand())
        lamp.register(BountyBoardCommand())
        lamp.register(BountyHistoryCommand())
    }

    private fun loadFiles() {
        saveDefaultConfig()
        this.dbConfig = CustomConfig(this, "db.yml")
    }

    private fun setupLamp() {
        this.lamp = BukkitLamp.builder(this)
            .parameterTypes {
                it.addParameterType(UserCommandParameter::class.java, UserParameterType())
            }
            .build()
    }

    private fun registerListeners() {
        PlayerRegistryListener(playerRegistry).registerListener(this)
        GUIListener().registerListener(this)
        BountyListener().registerListener(this)
    }

    private fun registerManagers() {
        setupEconomy()
        this._playerRegistry = PlayerManager()
        this._bountyManager = BountyManager()
    }

    private fun setupEconomy() {
        if (server.pluginManager.getPlugin("Vault") == null) {
            throw IllegalArgumentException("Vault was not found!")
        }
        val rsp = server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            throw IllegalArgumentException("No Economy plugin has been found!")
        }
        _economy = rsp.getProvider()
    }

}
