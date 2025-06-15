package me.wolfity.bountymaster.gui

import me.wolfity.bountymaster.BountyMaster
import me.wolfity.bountymaster.manager.Bounty
import me.wolfity.developmentutil.gui.PaginatedGUI
import me.wolfity.developmentutil.util.buildItem
import me.wolfity.developmentutil.util.launchAsync
import me.wolfity.developmentutil.util.style
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

abstract class BountyGUI(
    val bountyPlugin: BountyMaster,
    player: Player,
    items: List<Bounty>,
    val name: Component,
    val borderItemKey: String
) : PaginatedGUI<Bounty>(bountyPlugin, 54, name, player, 1, 30, items) {

    protected val BORDER_SLOTS = listOf(
        // Top row
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        // Left column
        9, 18, 27, 36,
        // Right column
        17, 26, 35, 44,
        // Bottom row
        46, 47, 48, 49, 50, 51, 52
    )

    init {
        constructBorder()
        constructGUI()
        openGUI()
    }

    private fun constructGUI() {
        val slots = (0 until 54).filter { it !in BORDER_SLOTS }

        val pageItems = getPageItems(getCurrentPage())
        launchAsync {
            pageItems.forEachIndexed { index, bounty ->
                if (index < slots.size) {
                    val slot = slots[index]
                    val bountyItem = createItem(bounty)
                    setItem(slot, bountyItem.first) {
                        bountyItem.second()
                    }
                }
            }
        }
    }

    abstract suspend fun createItem(bounty: Bounty) : Pair<ItemStack, () -> Unit>

    private fun constructBorder() {
        val borderItem = buildItem(Material.valueOf(bountyPlugin.config.getString(borderItemKey)!!))
        BORDER_SLOTS.forEach {
            setItem(it, borderItem)
        }

        if (hasPreviousPage()) {
            setItem(45, getPageLeftItem()) {
                decreaseCurrentPage()
                constructGUI()
            }
        } else {
            setItem(45, borderItem)
        }

        if (hasNextPage()) {
            setItem(53, getPageRightItem()) {
                increaseCurrentPage()
                constructGUI()
            }
        } else {
            setItem(53, borderItem)
        }
    }

}