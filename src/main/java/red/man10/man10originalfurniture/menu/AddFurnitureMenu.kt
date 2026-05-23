package red.man10.man10originalfurniture.menu

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import red.man10.man10library.inventory.MInventory
import red.man10.man10originalfurniture.Man10OriginalFurniture.Companion.plugin
import red.man10.man10originalfurniture.UserData
import red.man10.man10originalfurniture.Utility

class AddFurnitureMenu(
    private val target: OfflinePlayer,
    private val startCustomModelData: Int = MIN_CUSTOM_MODEL_DATA
) : MInventory(Component.text("§a§l[MyFurniture]§r${target.name ?: target.uniqueId} 追加"), 6) {

    override fun renderContents() {
        fillBackground()

        val start = startCustomModelData.coerceAtLeast(MIN_CUSTOM_MODEL_DATA)

        for (slot in 0 until 45){
            val cmd = start + slot
            val item = stoneHoe(cmd)

            set(slot,item) {
                onClick {
                    Thread{
                        UserData.addItem(target,item)
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            EditFurnitureMenu.openAsync(player,target)
                            player.sendMessage("§a§l追加しました CMD:${cmd}")
                        })
                    }.start()
                }
            }
        }

        val moves = listOf(-1000,-100,-10,-1,0,1,10,100,1000)
        moves.forEachIndexed { index, move ->
            if (move == 0){
                set(45 + index, currentRangeItem(start)) {
                }
            }else{
                set(45 + index, moveButton(move)) {
                    onClick {
                        AddFurnitureMenu(target,(start + move).coerceAtLeast(MIN_CUSTOM_MODEL_DATA)).open(player)
                    }
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

    private fun stoneHoe(cmd: Int): ItemStack {
        val item = ItemStack(Material.STONE_HOE)
        val meta = item.itemMeta ?: return item

        Utility.setCustomModelData(meta, cmd)
        meta.displayName(Component.text("§e§lCMD: $cmd"))
        meta.lore(listOf(
            Component.text("§7CustomModelData: §f$cmd"),
            Component.text("§eクリックで追加")
        ))
        item.itemMeta = meta

        return item
    }

    private fun moveButton(move: Int): ItemStack {
        val item = ItemStack(if (move < 0) Material.LAPIS_BLOCK else Material.REDSTONE_BLOCK)
        val meta = item.itemMeta ?: return item
        val sign = if (move > 0) "+" else ""
        val color = if (move < 0) "§b§l" else "§c§l"

        meta.displayName(Component.text("$color$sign$move"))
        meta.lore(listOf(Component.text("§eクリックでCMDを${sign}${move}移動")))
        item.itemMeta = meta

        return item
    }

    private fun currentRangeItem(start: Int): ItemStack {
        val item = ItemStack(Material.STONE_HOE)
        val meta = item.itemMeta ?: return item

        Utility.setCustomModelData(meta, start)
        meta.displayName(Component.text("§a§l現在のCMD: $start"))
        meta.lore(listOf(
            Component.text("§7中央ボタン"),
            Component.text("§7左右のボタンでCMDを移動")
        ))
        item.itemMeta = meta

        return item
    }

    companion object{
        private const val MIN_CUSTOM_MODEL_DATA = 2000
    }
}
