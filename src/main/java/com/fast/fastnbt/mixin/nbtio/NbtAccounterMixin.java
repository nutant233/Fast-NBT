package com.fast.fastnbt.mixin.nbtio;

import net.minecraft.nbt.NbtAccounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@link NbtAccounter#UNLIMITED} only overrides accountBytes, so the inherited readUTF still walks every
 * character of every key and string just to feed a counter whose result is thrown away. Unlimited accounters
 * are what NbtIo.read(DataInput) uses, i.e. every region-file chunk load plus level.dat / playerdata reads.
 */
@Mixin(NbtAccounter.class)
public abstract class NbtAccounterMixin {

    @Shadow
    public abstract void accountBytes(long bytes);

    /**
     * @author nutant233
     * @reason readUTF is a Forge-added method, so it carries no obfuscation mapping and must not be remapped.
     */
    @Overwrite(remap = false)
    public String readUTF(String data) {
        if ((Object) this == NbtAccounter.UNLIMITED) return data;
        accountBytes(2); //Header length
        if (data == null) return data;
        int len = data.length();
        int utflen = 0;
        for (int i = 0; i < len; i++) {
            int c = data.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) utflen += 1;
            else if (c > 0x07FF) utflen += 3;
            else utflen += 2;
        }
        accountBytes(utflen);
        return data;
    }

}
