package com.fast.fastnbt.mixin.nbtio;

import net.minecraft.nbt.NbtAccounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * {@link NbtAccounter#unlimitedHeap()} is what NbtIo.read(DataInput) and the trusted-tag network codecs use,
 * and it hands out a fresh accounter with a Long.MAX_VALUE quota on every call. The inherited readUTF still
 * walks every character of every key and string just to feed a counter whose result is discarded, so that
 * walk is skipped when the quota cannot be reached.
 */
@Mixin(NbtAccounter.class)
public abstract class NbtAccounterMixin {

    @Shadow
    @Final
    private long quota;

    @Shadow
    public abstract void accountBytes(long bytes);

    /**
     * @author nutant233
     * @reason skip the byte-accounting scan that unlimited readers throw away
     */
    @Overwrite
    public String readUTF(String data) {
        if (quota == Long.MAX_VALUE) return data;
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
