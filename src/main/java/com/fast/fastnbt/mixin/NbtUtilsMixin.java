package com.fast.fastnbt.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;

import java.util.Optional;

@Mixin(NbtUtils.class)
public class NbtUtilsMixin {

    @Shadow
    @Final
    private static Logger LOGGER;

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public static BlockState readBlockState(HolderGetter<Block> blockGetter, CompoundTag tag) {
        if (tag.tags.get("Name") instanceof StringTag stringTag) {
            Optional<? extends Holder<Block>> optional = blockGetter.get(ResourceKey.create(Registries.BLOCK, new ResourceLocation(stringTag.data)));
            if (optional.isPresent()) {
                Block block = optional.get().value();
                BlockState blockstate = block.defaultBlockState();
                if (tag.tags.get("Properties") instanceof CompoundTag compoundtag) {
                    var statedefinition = block.getStateDefinition();
                    for (var e : compoundtag.tags.entrySet()) {
                        if (e.getValue() instanceof StringTag string) {
                            var property = statedefinition.getProperty(e.getKey());
                            if (property != null) {
                                blockstate = fastnbt$setValueHelper(blockstate, property, e.getKey(), string.data, tag);
                            }
                        }
                    }
                }
                return blockstate;
            }
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Unique
    private static <S extends StateHolder<?, S>, T extends Comparable<T>> S fastnbt$setValueHelper(S stateHolder, Property<T> property, String propertyName, String properties, CompoundTag blockStateTag) {
        Optional<T> optional = property.getValue(properties);
        if (optional.isPresent()) {
            return stateHolder.setValue(property, optional.get());
        } else {
            LOGGER.warn("Unable to read property: {} with value: {} for blockstate: {}", propertyName, properties, blockStateTag.toString());
            return stateHolder;
        }
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public static int getDataVersion(CompoundTag tag, int defaultValue) {
        if (tag.tags.get("DataVersion") instanceof NumericTag numericTag) return numericTag.getAsInt();
        return defaultValue;
    }
}
