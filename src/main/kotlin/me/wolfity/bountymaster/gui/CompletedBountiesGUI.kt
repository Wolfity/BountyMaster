package me.wolfity.bountymaster.gui

import me.wolfity.bountymaster.BountyMaster
import me.wolfity.bountymaster.manager.Bounty
import me.wolfity.developmentutil.util.buildItem
import me.wolfity.developmentutil.util.style
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CompletedBountiesGUI(
    javaPlugin: BountyMaster,
    player: Player,
    items: List<Bounty>
) : BountyGUI(
    javaPlugin,
    player,
    items,
    style("<green>Completed Bounties"),
    "completed-bounty-board-gui-border-item",
) {

    override suspend fun createItem(bounty: Bounty): Pair<ItemStack, () -> Unit> {
        val bountyCreator = bountyPlugin.playerRegistry.getDataByUUID(bounty.creator)!!
        val bountyTarget = bountyPlugin.playerRegistry.getDataByUUID(bounty.target)!!

        val bountyDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
        val formattedCompletionTime = bountyDateTimeFormatter.format(
            Instant.ofEpochMilli(bounty.completedAt!!).atZone(ZoneId.systemDefault())
        )

        val title = plugin.config.getString("completed-bounty-board-icon-title")!!
            .replace("{target}", bountyTarget.name)
            .replace("{price}", bounty.reward.toString())

        val lore = plugin.config.getStringList("completed-bounty-board-icon-lore").map {
            it.replace("{target}", bountyTarget.name)
                .replace("{price}", bounty.reward.toString())
                .replace("{creator}", bountyCreator.name)
                .replace("{completionTime}", formattedCompletionTime)
        }.map { style(it) }

        val item = buildItem(Material.PLAYER_HEAD, style(title)) {
            setLore(lore)
            setCustomTexture(bountyTarget.skin)
        }
        return return item to {}
    }

}