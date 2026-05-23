package red.man10.man10originalfurniture.menu

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import red.man10.man10library.inventory.MInventory
import red.man10.man10originalfurniture.Man10OriginalFurniture.Companion.plugin
import red.man10.man10originalfurniture.UserData
import red.man10.man10originalfurniture.Utility

class EditFurnitureMenu(
    private val target: OfflinePlayer,
    private val furnitureItems: List<ItemStack>,
    private val page: Int = 0
) : MInventory(Component.text("§a§l[MyFurniture]§r${target.name ?: target.uniqueId} 編集"), 6) {

    private val maxPage: Int
        get() = ((furnitureItems.size - 1) / PAGE_SIZE).coerceAtLeast(0)

    private val currentPage: Int
        get() = page.coerceIn(0, maxPage)

    override fun renderContents() {
        fillBackground()
        set(4, playerHead()) {
        }

        furnitureItems.sortedWith(compareBy<ItemStack> { it.type.name }.thenBy { customModelDataOf(it) })
            .drop(currentPage * PAGE_SIZE)
            .take(PAGE_SIZE)
            .forEachIndexed { index, item ->
                val furniture = editItem(item)
                set(index + 9, furniture) {
                    onClick {
                        if (inventoryClickEvent.click != ClickType.SHIFT_LEFT)return@onClick

                        Thread{
                            UserData.removeItem(target,item.type,customModelDataOf(item))
                            Bukkit.getScheduler().runTask(plugin, Runnable {
                                openAsync(player,target,currentPage)
                                player.sendMessage("§a§l削除しました")
                            })
                        }.start()
                    }
                }
            }

        set(45, pageButton("§6§l前ページ", currentPage > 0)) {
            onClick {
                if (currentPage > 0) {
                    openAsync(player,target,currentPage - 1)
                }
            }
        }

        set(49, Material.EMERALD_BLOCK) {
            customName = Component.text("§a§l家具を追加")
            lore = listOf(Component.text("§eクリックで追加GUIを開く"))
            onClick {
                AddFurnitureMenu(target).open(player)
            }
        }

        set(53, pageButton("§6§l次ページ", currentPage < maxPage)) {
            onClick {
                if (currentPage < maxPage) {
                    openAsync(player,target,currentPage + 1)
                }
            }
        }
    }

    private fun fillBackground() {
        set(0 until 54, Material.BLACK_STAINED_GLASS_PANE) {
            customName = Component.empty()
            hideTooltip = true
        }
    }

    private fun editItem(item: ItemStack): ItemStack {
        val clone = item.clone()
        val meta = clone.itemMeta ?: return clone
        val lore = meta.lore()?.toMutableList() ?: mutableListOf()
        val customModelData = customModelDataOf(clone)

        Utility.setCustomModelData(meta, customModelData)
        lore.add(Component.text("§cシフト左クリックで削除"))
        meta.lore(lore)
        clone.itemMeta = meta

        return clone
    }

    private fun playerHead(): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta

        meta.owningPlayer = target
        meta.displayName(Component.text("§e§l${target.name ?: target.uniqueId}"))
        item.itemMeta = meta

        return item
    }

    private fun pageButton(name: String, enabled: Boolean): ItemStack {
        val item = ItemStack(if (enabled) Material.ARROW else Material.GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta ?: return item
        meta.displayName(Component.text(name))
        meta.lore(listOf(Component.text("§e${currentPage + 1} / ${maxPage + 1}")))
        item.itemMeta = meta
        return item
    }

    companion object{
        private const val PAGE_SIZE = 36

        fun openAsync(player: Player,targetName:String){
            openAsync(player,Bukkit.getOfflinePlayer(targetName))
        }

        fun openAsync(player: Player,target: OfflinePlayer,page:Int = 0){
            Thread{
                val items = UserData.loadItems(target)

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    EditFurnitureMenu(target,items,page).open(player)
                })
            }.start()
        }

        fun customModelDataOf(item: ItemStack): Int {
            val meta = item.itemMeta ?: return 0
            return if (meta.hasCustomModelData()) meta.customModelData else 0
        }
    }
}
