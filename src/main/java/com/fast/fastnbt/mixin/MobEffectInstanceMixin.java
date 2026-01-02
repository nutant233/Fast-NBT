package com.fast.fastnbt.mixin;

import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin {

    @Shadow
    private static MobEffectInstance readCurativeItems(MobEffectInstance effect, CompoundTag nbt) {
        return null;
    }

    @Shadow
    @Final
    private static Logger LOGGER;

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    private static MobEffectInstance loadSpecifiedEffect(MobEffect effect, CompoundTag nbt) {
        int i = nbt.getByte("Amplifier");
        int j = nbt.getInt("Duration");
        boolean flag = nbt.getBoolean("Ambient");
        boolean flag1 = true;
        if (nbt.tags.get("ShowParticles") instanceof ByteTag byteTag) {
            flag1 = byteTag.getAsByte() != 0;
        }

        boolean flag2 = flag1;
        if (nbt.tags.get("ShowIcon") instanceof ByteTag byteTag) {
            flag2 = byteTag.getAsByte() != 0;
        }

        MobEffectInstance mobeffectinstance = null;
        if (nbt.tags.get("HiddenEffect") instanceof CompoundTag compoundTag) {
            mobeffectinstance = loadSpecifiedEffect(effect, compoundTag);
        }

        Optional<MobEffectInstance.FactorData> optional;
        if (nbt.tags.get("FactorCalculationData") instanceof CompoundTag compoundTag) {
            optional = MobEffectInstance.FactorData.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, compoundTag)).resultOrPartial(LOGGER::error);
        } else {
            optional = Optional.empty();
        }

        return readCurativeItems(new MobEffectInstance(effect, j, Math.max(0, i), flag, flag1, flag2, mobeffectinstance, optional), nbt);
    }
}
