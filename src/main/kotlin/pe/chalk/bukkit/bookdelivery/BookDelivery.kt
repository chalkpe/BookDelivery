package pe.chalk.bukkit.bookdelivery

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
    }

    @EventHandler
    fun onPlayerPickupItem(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity !is Player) return

        val book = entity.inventory.find { isWrittenBook(it) } ?: return
        val locations = getLocationsFromPages(entity, (book.itemMeta as BookMeta).pages)

        for (location in locations) {
            println(location)
            if (moveToChest(location, event.item.itemStack)) {
                event.item.remove()
                event.isCancelled = true
                entity.playSound(entity.location, Sound.ENTITY_ARROW_HIT_PLAYER, 0.2f, 0.5f)
                break
            }
        }

        if (!event.isCancelled) {
            entity.playSound(entity.location, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.2f, 0.5f)
        }
    }

    private fun isWrittenBook(stack: ItemStack?): Boolean {
        if (stack == null || !books.contains(stack.type)) return false

        val itemMeta = stack.itemMeta
        if (itemMeta !is BookMeta) return false

        return itemMeta.hasPages()
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
        if (state !is Container) return false

        return addItemToInventory(state.inventory, itemStack)
    }

    private fun canBeAdded(a: ItemStack, b: ItemStack): Boolean {
        return a.isSimilar(b) && (a.amount + b.amount) <= a.maxStackSize
    }

    private fun addItemToInventory(inventory: Inventory, itemStack: ItemStack): Boolean {
        val contents = inventory.contents.filterNotNull()
        val slot = contents.find { canBeAdded(it, itemStack) }

        if (contents.size == inventory.size) {
            if (slot == null) {
                return false
            } else {
                slot.amount += itemStack.amount
                inventory.contents = contents.toTypedArray()
                return true
            }
        } else {
            if (slot == null) {
                inventory.contents = (contents + itemStack).toTypedArray()
            } else {
                slot.amount += itemStack.amount
                inventory.contents = contents.toTypedArray()
            }
            return true
        }
    }
}