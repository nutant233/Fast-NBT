package com.fast.fastnbt.mixin.blockstatecodec;

import com.fast.fastnbt.blockstatecodec.FastBlockStateCodec;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Swaps in the fast decode path at the end of BlockState's own static initializer. This has to happen here
 * rather than lazily: other classes (ChunkSerializer's BLOCK_STATE_CODEC, PalettedContainer.codecRW) capture
 * this field into their own static finals as soon as they are initialised, which is always after BlockState.
 */
@Mixin(BlockState.class)
public class BlockStateCodecMixin {

    @Mutable
    @Final
    @Shadow
    public static Codec<BlockState> CODEC;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void fastnbt$installFastCodec(CallbackInfo ci) {
        CODEC = FastBlockStateCodec.install(CODEC);
    }
}
