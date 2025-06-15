package me.wolfity.bountymaster.gui

import me.wolfity.bountymaster.BountyMaster
import me.wolfity.bountymaster.manager.Bounty
import me.wolfity.developmentutil.gui.PaginatedGUI
import me.wolfity.developmentutil.util.TimeFormatConfig
import me.wolfity.developmentutil.util.buildItem
import me.wolfity.developmentutil.util.formatTime
import me.wolfity.developmentutil.util.launchAsync
import me.wolfity.developmentutil.util.style
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class BountyBoardGUI(
    bountyPlugin: BountyMaster,
    player: Player,
    items: List<Bounty>,
) : BountyGUI(bountyPlugin, player, items, style("<green>Bounty Board"), "bounty-board-gui-border-item") {


    override suspend fun createItem(bounty: Bounty): Pair<ItemStack, () -> Unit> {
        val bountyCreator = bountyPlugin.playerRegistry.getDataByUUID(bounty.creator)!!
        val bountyTarget = bountyPlugin.playerRegistry.getDataByUUID(bounty.target)!!

        val now = System.currentTimeMillis()
        val bountyStart = bounty.createdAt
        val bountyEnd = bountyStart + bounty.duration

        val expire = bountyEnd - now

        val formattedExpire = formatTime(expire, TimeFormatConfig())

        val title = plugin.config.getString("bounty-board-icon-title")!!
            .replace("{target}", bountyTarget.name)
            .replace("{price}", bounty.reward.toString())

        val lore = plugin.config.getStringList("bounty-board-icon-lore").map {
            it.replace("{target}", bountyTarget.name)
                .replace("{price}", bounty.reward.toString())
                .replace("{creator}", bountyCreator.name)
                .replace("{expireTime}", formattedExpire)
        }.map { style(it) }

        val item = buildItem(Material.PLAYER_HEAD, style(title)) {
            setLore(lore)
            setCustomTexture(bountyTarget.skin)
        }
        return return item to {}
    }
}