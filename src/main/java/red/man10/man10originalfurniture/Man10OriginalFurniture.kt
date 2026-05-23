package red.man10.man10originalfurniture

import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10library.MJavaPlugin
import red.man10.man10originalfurniture.menu.Event
import red.man10.man10originalfurniture.menu.EditFurnitureMenu
import red.man10.man10originalfurniture.menu.MainMenu

class Man10OriginalFurniture : MJavaPlugin() , Listener{

    companion object{
        const val OP = "myfurniture.op"
        const val USER = "myfurniture.user"
        const val EDIT = "myfurniture.edit"
        const val prefix = "§a§l[MyFurniture]"

        lateinit var plugin : JavaPlugin

        lateinit var exchangeItem : ItemStack
    }

    override fun onEnable() {
        super.onEnable()
        // Plugin startup logic

        plugin = this

        server.pluginManager.registerEvents(Event,this)

        saveDefaultConfig()
        exchangeItem = config.getItemStack("exchange")?: ItemStack(Material.STONE)

        MySQLManager.mysqlQueue(this,"Man10OriginalFurniture")
    }

    override fun onDisable() {
        super.onDisable()
        // Plugin shutdown logic
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return true

        if (args.isNotEmpty() && args[0] == "edit"){
            if (!sender.hasPermission(EDIT))return true

            if (args.size!=2){
                sender.sendMessage("§e§l/mf edit <player>")
                return true
            }

            EditFurnitureMenu.openAsync(sender,args[1])
            return true
        }

        if (!sender.hasPermission(USER))return true

        if (args.isEmpty()){

            MainMenu(sender).open()

            return true
        }

        when(args[0]){

            "exchangeItem" ->{

                exchangeItem = sender.inventory.itemInMainHand
                config.set("exchange", exchangeItem.clone())
                saveConfig()
                sender.sendMessage("§e§l設定完了")
                return true
            }

            "add" ->{//mf add <mcid> <Material> <CustomModelData>
                if (!sender.hasPermission(OP))return true

                if (args.size!=2){
                    sender.sendMessage("§e§l/mf add <mcid> 手に持っているアイテムを登録します")
                    return true
                }

                val item = sender.inventory.itemInMainHand

                if (!item.hasItemMeta()){
                    sender.sendMessage("§e§lアイテムのCustomModelDataが設定されていない可能性があります")
                    return true
                }

                //家具以外を弾くための臨時措置
                if(item.type!=Material.STONE_HOE){
                    sender.sendMessage("§e§l石のクワ以外のアイテムを登録することはできません")
                    return true
                }

                Thread{
                    UserData.addItem(args[1],item)
                    sender.sendMessage("§e§l登録完了 mcid:${args[1]} CMD:${item.itemMeta.customModelData}")
                }.start()
                return true
            }

            "remove" ->{
                if (!sender.hasPermission(OP))return true

                if (args.size!=1){
                    sender.sendMessage("§e§l/mf remove 手に持ってるアイテムをDBから削除します")
                    return true
                }

                val item = sender.inventory.itemInMainHand

                if (!item.hasItemMeta()){
                    sender.sendMessage("§e§lアイテムのCustomModelDataが設定されていない可能性があります")
                    return true
                }

                Thread{

                    UserData.removeItem(item.type,item.itemMeta.customModelData)
                    sender.sendMessage("§e§l削除完了 CMD:${item.itemMeta.customModelData}")
                }.start()

                return true
            }

            "help" ->{
                sender.sendMessage("§e§l/mf rename <名前>             ... 持ってるアイテムの名前を設定する")
                sender.sendMessage("§e§l/mf relore <説明1行目;説明2行目 ... 持ってるアイテムの説明を設定する")
                sender.sendMessage("§e§l/mf set                      ... 設定したアイテムを登録する")

                return true
            }

            "rename" ->{

                val data = UserData.userData[sender] ?: return true

                val str = StringBuilder()

                for (i in 1 until args.size){
                    str.append(args[i])
                }

                data.rename(str.toString(),sender.inventory.itemInMainHand)

                return true
            }

            "relore" ->{

                val data = UserData.userData[sender] ?: return true

                val str = StringBuilder()

                for (i in 1 until args.size){
                    str.append(args[i])
                }

                val strList = str.split(";").toMutableList()

                data.relore(strList,sender.inventory.itemInMainHand)

                return true
            }

            "set" ->{

                val data = UserData.userData[sender] ?: return true

                val item=sender.inventory.itemInMainHand


                if(item.type!=Material.STONE_HOE){
                    sender.sendMessage("§e§l石のクワ以外のアイテムを登録することはできません")
                    return true
                }

                Thread{
                    data.setNewItem(item)
                }.start()
            }

        }

        return false
    }
}
