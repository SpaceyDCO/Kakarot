package github.kakarot.Tools;

import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class NbtHandler {
    private final ItemStack item;
    @Getter
    NBTTagCompound compound;
    public NbtHandler(org.bukkit.inventory.ItemStack item) {
        ItemStack nmsStack = (ItemStack) toNMSItem(item);
        this.item = nmsStack;
        if(nmsStack != null && nmsStack.hasTagCompound()) this.compound = nmsStack.getTagCompound();
        else this.compound = null;
    }
    public boolean hasNBT() {
        return item != null && item.hasTagCompound();
    }
    public boolean isEmpty() {
        return compound == null || compound.hasNoTags();
    }
    public void setCompoundFromString(String comp) {
        try {
            NBTTagCompound nbt = (NBTTagCompound) JsonToNBT.func_150315_a(comp);
            item.setTagCompound(nbt);
        }catch(Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error while trying to apply NBT data ", e);
            Bukkit.getLogger().severe("Maybe the item's lore contains illegal characters like ''");
        }
    }
    public void setString(String key, String value) {
        ensureCompound();
        compound.setString(key, value);
        item.setTagCompound(compound);
    }
    public void setInteger(String key, int value) {
        ensureCompound();
        compound.setInteger(key, value);
        item.setTagCompound(compound);
    }
    public void setBoolean(String key, boolean value) {
        ensureCompound();
        compound.setBoolean(key, value);
        item.setTagCompound(compound);
    }
    public void setShort(String key, short value) {
        ensureCompound();
        compound.setShort(key, value);
        item.setTagCompound(compound);
    }
    public void addCompound(String key) {
        NBTTagCompound newComp = new NBTTagCompound();
        setCompound(key, newComp);
    }
    public void setCompound(String key, NBTTagCompound compound) {
        ensureCompound();
        this.compound.setTag(key, compound);
        item.setTagCompound(this.compound);
    }
    public void changeDamage(int damage) {
        ensureCompound();
        NBTTagList modifiers = new NBTTagList();
        NBTTagCompound damageTag = new NBTTagCompound();
        damageTag.setString("AttributeName", "generic.attackDamage");
        damageTag.setString("Name", "generic.attackDamage");
        damageTag.setInteger("Amount", damage);
        damageTag.setInteger("Operation", 0);
        damageTag.setLong("UUIDMost", item.hashCode());
        damageTag.setLong("UUIDLeast", item.hashCode());
        damageTag.setString("Slot", "mainhand");
        modifiers.appendTag(damageTag);
        this.compound.setTag("AttributeModifiers", modifiers);
        item.setTagCompound(this.compound);
    }
    public String getString(String key) {
        return compound != null ? compound.getString(key) : "";
    }
    public int getInteger(String key) {
        return compound != null ? compound.getInteger(key) : 0;
    }
    public boolean getBoolean(String key) {
        return compound != null && compound.getBoolean(key);
    }
    public NBTTagCompound getCompound(String key) {
        return compound != null ? compound.getCompoundTag(key) : null;
    }
    public boolean containsCompound(String key) {
        return compound != null && compound.hasKey(key, 10);
    }
    public org.bukkit.inventory.ItemStack getItemStack() {
        return (org.bukkit.inventory.ItemStack) toBukkitItem(this.item);
    }
    private void ensureCompound() {
        if (compound == null) {
            compound = new NBTTagCompound();
        }
    }
    //STATIC METHODS
    private static Object toNMSItem(org.bukkit.inventory.ItemStack bukkitItem) {
        try {
            Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack");
            return craftItemStack.getMethod("asNMSCopy", org.bukkit.inventory.ItemStack.class).invoke(null, bukkitItem);
        }catch(Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to convert NMS -> Bukkit itemStack", e);
            return null;
        }
    }
    private static Object toBukkitItem(ItemStack nmsItem) {
        try {
            Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack");
            return craftItemStack.getMethod("asBukkitCopy", ItemStack.class).invoke(null, nmsItem);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to convert NMS → Bukkit ItemStack", e);
            return null;
        }
    }
    //STATIC METHODS
}
