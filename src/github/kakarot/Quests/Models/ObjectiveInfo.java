package github.kakarot.Quests.Models;

import lombok.Getter;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;

@Getter
public class ObjectiveInfo {
    private final String target;
    private String title;
    private String placeholderName;
    private NBTTagCompound parsedNbt;
    public ObjectiveInfo(String target, String title, String nbt, String placeholderName) throws NBTException {
        this.target = target;
        this.title = title;
        if(nbt != null && !nbt.isEmpty()) this.parsedNbt = (NBTTagCompound) JsonToNBT.func_150315_a(nbt);
        else this.parsedNbt = null;
        this.placeholderName = placeholderName;
    }
    public ObjectiveInfo(String target, String title, String nbt) throws NBTException {
        this.target = target;
        this.title = title;
        if(nbt != null && !nbt.isEmpty()) this.parsedNbt = (NBTTagCompound) JsonToNBT.func_150315_a(nbt);
        else this.parsedNbt = null;
    }
    public ObjectiveInfo(String target) {
        this.target = target;
    }
    public ObjectiveInfo(String target, String title) {
        this.target = target;
        this.title = title;
    }
    public int getItemId() {
        if(!target.contains(":")) return Integer.parseInt(target);
        return Integer.parseInt(target.split(":")[0]);
    }
    public byte getDataValue() {
        if(!target.contains(":")) return 0;
        return Byte.parseByte(target.split(":")[1]);
    }
    public boolean hasNbt() {
        return this.parsedNbt != null;
    }
}
