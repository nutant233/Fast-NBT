package com.fast.fastnbt.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow
    @Nullable
    private CompoundTag tag;

    @Shadow
    public abstract void addTagElement(String key, Tag tag);

    @Shadow(remap = false)
    private CompoundTag capNBT;

    @Shadow
    public abstract Item getItem();

    @Redirect(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;contains(Ljava/lang/String;)Z"))
    private boolean capNBT(CompoundTag tag, String key) {
        return false;
    }

    @Redirect(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;contains(Ljava/lang/String;I)Z"))
    private boolean tag(CompoundTag tag, String key, int tagType) {
        if (tag.tags.get("ForgeCaps") instanceof CompoundTag compoundTag) {
            this.capNBT = compoundTag;
        }
        if (tag.tags.get(key) instanceof CompoundTag compoundTag) {
            this.tag = compoundTag;
            this.getItem().verifyTagAfterLoad(this.tag);
        }
        return false;
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public CompoundTag getOrCreateTagElement(String key) {
        if (this.tag != null && this.tag.tags.get(key) instanceof CompoundTag compoundTag) {
            return compoundTag;
        } else {
            CompoundTag compoundtag = new CompoundTag();
            this.addTagElement(key, compoundtag);
            return compoundtag;
        }
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public CompoundTag getTagElement(String key) {
        if (this.tag != null && this.tag.tags.get(key) instanceof CompoundTag compoundTag) return compoundTag;
        return null;
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public void removeTagKey(String key) {
        if (this.tag != null) {
            this.tag.remove(key);
            if (this.tag.isEmpty()) this.tag = null;
        }
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public boolean isEnchanted() {
        if (this.tag != null && this.tag.tags.get("Enchantments") instanceof ListTag listTag) {
            return !listTag.list.isEmpty();
        }
        return false;
    }
}
