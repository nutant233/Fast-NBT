package com.fast.fastnbt.mixin.nbtaccounter;

import net.minecraft.nbt.NbtAccounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Removes NbtAccounter's accounting entirely: no byte quota, no depth limit and no UTF length scan.
 *
 * <p>Vanilla uses this class for two different jobs. NbtIo and the trusted network codecs read through
 * unlimitedHeap(), where the accounting is pure overhead, but the client-bound packet codecs
 * (ByteBufCodecs.TAG / COMPOUND_TAG, FriendlyByteBuf.readNbt) and level.dat use real quotas - 2 MB and
 * 100 MB - because that NBT arrives from a client. The depth limit (512, applied per nested compound/list)
 * is what turns hostile deeply nested data into a clean NbtAccounterException instead of a recursion into
 * StackOverflowError.
 *
 * <p>With this mixin applied, nothing caps the size or the nesting of NBT that is read. On a server that
 * means a modified client can hand the server an arbitrarily large tag, and deeply nested tags recurse.
 * Turn the feature off (or use the nbtIo feature, which only skips the UTF scan for unlimited readers) if
 * that matters for the deployment.
 */
@Mixin(NbtAccounter.class)
public abstract class NbtAccounterMixin {

    @Overwrite
    public void accountBytes(long bytes) {
    }

    @Overwrite
    public void accountBytes(long bytes, long count) {
    }

    @Overwrite
    public void pushDepth() {
    }

    @Overwrite
    public void popDepth() {
    }

    @Overwrite
    public String readUTF(String data) {
        return data;
    }
}
