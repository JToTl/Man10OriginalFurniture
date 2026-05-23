package red.man10.man10originalfurniture

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import red.man10.man10originalfurniture.Man10OriginalFurniture.Companion.exchangeItem
import red.man10.man10originalfurniture.Man10OriginalFurniture.Companion.plugin
import java.util.concurrent.ConcurrentHashMap

class UserData {

    var itemDictionary = ConcurrentHashMap<Pair<Material,Int>,ItemStack>()
    lateinit var player : Player
    private val namePrefix="§f[§ePvt家具§r§f]§l"

    private fun checkItem(item:ItemStack):Boolean{

        if (!item.hasItemMeta())return false

        val material = item.type
        val cmd = item.itemMeta.customModelData

        if (itemDictionary[Pair(material,cmd)] != null)return true

        return false

    }

    fun setNewItem(item: ItemStack){

        if (!checkItem(item)){
            player.sendMessage("§c§lこのアイテムはあなたの作ったアイテムとして登録されていません！")
            return
        }

        val mysql = MySQLManager(plugin,"UserData")

        mysql.execute("UPDATE user_data SET created_item='${Utility.itemToBase64(item)}' " +
                "WHERE uuid='${player.uniqueId}' and material='${item.type}' and custom_model_data=${item.itemMeta.customModelData}")

        itemDictionary[Pair(item.type,item.itemMeta.customModelData)] = item

        player.sendMessage("§a§l登録完了")
    }

    fun rename(name:String,item:ItemStack){
        if (!checkItem(item)){
            player.sendMessage("§c§lこのアイテムはあなたの作ったアイテムとして登録されていません！")
            return
        }

        val meta = item.itemMeta
        meta.displayName(Component.text(namePrefix+name))
        item.itemMeta = meta

        player.sendMessage("§a§l設定完了")
    }

    fun relore(lore: MutableList<String>,item: ItemStack){
        if (!checkItem(item)){
            player.sendMessage("§c§lこのアイテムはあなたの作ったアイテムとして登録されていません！")
            return
        }

        val meta = item.itemMeta
        for (i in 0 until lore.size){
            lore[i] = "§f"+lore[i]
        }
        lore.add("§ecreated by ${player.name}")
        meta.lore = lore
        item.itemMeta = meta

        player.sendMessage("§a§l設定完了")

    }

    fun buyItem(mock: ItemStack){
        if (!checkItem(mock))return

        val rawItem = itemDictionary[Pair(mock.type,mock.itemMeta.customModelData)]?:return
        val item=ItemStack(rawItem.type)
        item.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        val meta=item.itemMeta
        meta.displayName(Component.text(namePrefix))
        meta.lore(listOf(Component.text("§ecreated by ${player.name}")))
        meta.setCustomModelData(rawItem.itemMeta.customModelData)
        item.itemMeta=meta

        val hand = player.inventory.itemInMainHand

        if (!hand.isSimilar(exchangeItem) || hand.amount < exchangeItem.amount){
            player.sendMessage("§c§l利き手に投票パールを${exchangeItem.amount}個以上持ってください！")
            return
        }

        hand.amount = hand.amount - exchangeItem.amount

        MySQLManager.mysqlQueue.add("INSERT INTO log (player, uuid, material, custom_model_data, display_name, date) " +
                "VALUES ('${player.name}', '${player.uniqueId}', '${item.type.name}', ${item.itemMeta.customModelData}, '${MySQLManager.escapeStringForMySQL(item.itemMeta.displayName)}', DEFAULT)")

        player.inventory.addItem(item.clone())

        player.sendMessage("§e§l購入しました")

    }

    companion object{

        var userData = ConcurrentHashMap<Player,UserData>()

        fun loadData(p: Player):UserData?{
            val data = UserData()

            val mysql = MySQLManager(plugin,"UserData")

            val rs = mysql.query("select created_item from user_data where uuid='${p.uniqueId}'")
            if (rs == null){
                mysql.close()
                return null
            }

            val dic = ConcurrentHashMap<Pair<Material,Int>,ItemStack>()

            while (rs.next()){
                val item = Utility.itemFromBase64(rs.getString("created_item"))!!

                dic[Pair(item.type,item.itemMeta.customModelData)] = item
            }

            rs.close()
            mysql.close()

            if (dic.isEmpty()){
                userData.remove(p)
                return null
            }

            data.itemDictionary = dic
            data.player = p


            userData[p] = data

            return data
        }

        fun addItem(mcid:String,item:ItemStack){

            val p = Bukkit.getOfflinePlayer(mcid)

            addItem(p,item)
        }

        fun addItem(p: OfflinePlayer,item:ItemStack){

            val mysql = MySQLManager(plugin,"UserData")

            mysql.execute("DELETE FROM user_data WHERE uuid='${p.uniqueId}' and material='${item.type.name}' and custom_model_data=${item.itemMeta.customModelData}")

            mysql.execute("INSERT INTO user_data (player, uuid, material, custom_model_data,  created_item) " +
                    "VALUES ('${p.name}', '${p.uniqueId}', '${item.type.name}', ${item.itemMeta.customModelData},  '${Utility.itemToBase64(item)}')")

            if (p.isOnline){
                loadData(p.player!!)
            }
        }

        fun removeItem(material: Material,cmd:Int){

            val mysql = MySQLManager(plugin,"UserData")

            mysql.execute("DELETE FROM user_data WHERE material='${material.name}' AND custom_model_data=${cmd}")

        }

        fun removeItem(p: OfflinePlayer,material: Material,cmd:Int){

            val mysql = MySQLManager(plugin,"UserData")

            mysql.execute("DELETE FROM user_data WHERE uuid='${p.uniqueId}' AND material='${material.name}' AND custom_model_data=${cmd}")

            if (p.isOnline){
                loadData(p.player!!)
            }

        }

        fun loadItems(p: OfflinePlayer):MutableList<ItemStack>{

            val mysql = MySQLManager(plugin,"UserData")

            val rs = mysql.query("select created_item from user_data where uuid='${p.uniqueId}'")
            if (rs == null){
                mysql.close()
                return mutableListOf()
            }

            val list = mutableListOf<ItemStack>()

            while (rs.next()){
                val item = Utility.itemFromBase64(rs.getString("created_item")) ?: continue

                list.add(item)
            }

            rs.close()
            mysql.close()

            return list
        }
    }



}
