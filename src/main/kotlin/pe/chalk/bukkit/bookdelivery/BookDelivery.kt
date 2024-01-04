package pe.chalk.bukkit.bookdelivery

import org.bstats.bukkit.Metrics
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Container
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.plugin.java.JavaPlugin

class BookDelivery: JavaPlugin(), Listener {
    private val books = arrayOf(
        Material.WRITABLE_BOOK,
        Material.WRITTEN_BOOK
    )

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        Metrics(this, 20645)
    }

    @EventHandler
    fun onPlayerPickupItem(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity !is Player) return

        val item = event.item
        if (checkAlreadyHaveItem(entity, item.itemStack)) return

        val bookMeta = findBookMetaFromPlayer(entity) ?: return
        val locations = getLocationsFromPages(entity, bookMeta.pages)

        if (locations.isEmpty()) return
        for (location in locations) {
            if (moveToChest(location, item.itemStack)) {
                item.remove()
                event.isCancelled = true
                entity.playSound(entity.location, Sound.ENTITY_ARROW_HIT_PLAYER, 0.2f, 0.5f)
                return
            }
        }

        entity.playSound(entity.location, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.2f, 0.5f)
    }

    private fun canBeAdded(a: ItemStack?, b: ItemStack): Boolean {
        return a != null && a.isSimilar(b) && (a.amount + b.amount) <= a.maxStackSize
    }

    private fun checkAlreadyHaveItem(player: Player, itemStack: ItemStack): Boolean {
        return player.inventory.any { canBeAdded(it, itemStack) }
    }

    private fun findBookMetaFromPlayer(player: Player): BookMeta? {
        val hotbar = player.inventory.take(9)
        val book = hotbar.find { isWrittenBook(it) } ?: return null

        val meta = book.itemMeta ?: return null
        return if (meta is BookMeta) meta else null
    }

    private fun isWrittenBook(stack: ItemStack?): Boolean {
        if (stack == null || !books.contains(stack.type)) return false

        val itemMeta = stack.itemMeta
        return itemMeta is BookMeta && itemMeta.hasPages()
    }

    private fun getLocationsFromPages(player: Player, pages: List<String>): List<Location> {
        val content = pages.joinToString("").replace(Regex("[ \n]"), "")

        return content.split("/")
            .map { coord -> coord.split(",").map { it.toDoubleOrNull() ?: 0.0 }.toTypedArray() }
            .map { Location(player.world, it[0], it[1], it[2]) }
    }

    private fun moveToChest(location: Location, itemStack: ItemStack): Boolean {
        val block = location.world?.getBlockAt(location) ?: return false

        val state = block.state
        return state is Container && addItemToInventory(state.inventory, itemStack)
    }

    private fun addItemToInventory(inventory: Inventory, itemStack: ItemStack): Boolean {
        val contents = inventory.contents.filterNotNull()
        val slot = contents.find { canBeAdded(it, itemStack) }

        if (slot != null) {
            slot.amount += itemStack.amount
            inventory.contents = contents.toTypedArray()
            return true
        }

        if (contents.size < inventory.size) {
            inventory.contents = (contents + itemStack).toTypedArray()
            return true
        }

        return false
    }
}