package com.fast.fastnbt.mixin.nbtio;

import net.minecraft.nbt.NbtAccounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@link NbtAccounter#UNLIMITED} only overrides accountBytes, so the inherited readUTF still walks every
 * character of every key and string just to feed a counter whose result is thrown away. Unlimited accounters
 * are what NbtIo.read(DataInput) uses, i.e. every region-file chunk load plus level.dat / playerdata reads.
 */
@Mixin(NbtAccounter.class)
public class NbtAccounterMixin {

    // readUTF is a Forge-added method, so it carries no obfuscation mapping and must not be remapped.
    @Inject(method = "readUTF", at = @At("HEAD"), cancellable = true, remap = false)
    private void fastnbt$skipUtfAccounting(String data, CallbackInfoReturnable<String> cir) {
        if ((Object) this == NbtAccounter.UNLIMITED) {
            cir.setReturnValue(data);
        }
    }
}
