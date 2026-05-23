package red.man10.man10originalfurniture

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.util.io.BukkitObjectInputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.bukkit.util.io.BukkitObjectOutputStream as BukkitObjectOutputStream1

object Utility {

    fun setCustomModelData(meta: ItemMeta, customModelData: Int) {
        meta.setCustomModelData(customModelData)

        try {
            val getComponent = meta.javaClass.methods.firstOrNull {
                it.name == "getCustomModelDataComponent" && it.parameterCount == 0
            } ?: return
            val component = getComponent.invoke(meta)
            val setFloats = component.javaClass.methods.firstOrNull {
                it.name == "setFloats" && it.parameterCount == 1
            } ?: return
            val setComponent = meta.javaClass.methods.firstOrNull {
                it.name == "setCustomModelDataComponent" && it.parameterCount == 1
            } ?: return

            setFloats.invoke(component, listOf(customModelData.toFloat()))
            setComponent.invoke(meta, component)
        } catch (_: Throwable) {
        }
    }

    ///////////////////////////////
    //base 64
    //////////////////////////////
    fun itemFromBase64(data: String): ItemStack? = try {
        val inputStream = ByteArrayInputStream(Base64Coder.decodeLines(data))
        val dataInput = BukkitObjectInputStream(inputStream)
        val items = arrayOfNulls<ItemStack>(dataInput.readInt())

        // Read the serialized inventory
        for (i in items.indices) {
            items[i] = dataInput.readObject() as ItemStack
        }

        dataInput.close()
        items[0]
    } catch (e: Exception) {
        null
    }

    @Throws(IllegalStateException::class)
    fun itemToBase64(item: ItemStack): String {
        try {
            val outputStream = ByteArrayOutputStream()
            val dataOutput = BukkitObjectOutputStream1(outputStream)
            val items = arrayOfNulls<ItemStack>(1)
            items[0] = item
            dataOutput.writeInt(items.size)

            for (i in items.indices) {
                dataOutput.writeObject(items[i])
            }

            dataOutput.close()

            return Base64Coder.encodeLines(outputStream.toByteArray())

        } catch (e: Exception) {
            throw IllegalStateException("Unable to save item stacks.", e)
        }
    }

}
