package com.fast.fastnbt.nbtaccounter;

import net.minecraft.nbt.NbtAccounter;

/**
 * The single accounter handed out for unlimited reads.
 *
 * <p>NeoForge's {@link NbtAccounter#unlimitedHeap()} builds a fresh {@code NbtAccounter(Long.MAX_VALUE, 512)}
 * on every call, which means one allocation per chunk read and per trusted-tag packet. Sharing one instance
 * is only sound because the fields it would otherwise carry state in are gone: the nbtAccounter feature also
 * removes the accounting and the depth counter, so nothing ever reads or writes them. That is why this lives
 * behind that feature and not next to nbtIo, whose fast path deliberately keeps the accounting intact.
 */
public final class UnlimitedAccounter {

    public static final NbtAccounter INSTANCE = new NbtAccounter(Long.MAX_VALUE, 512);

    private UnlimitedAccounter() {
    }
}
