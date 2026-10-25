package com.fast.fastnbt.mixin.blockstate;

import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;

import java.util.Optional;

/**
 * 1.19.2 signature: {@code readBlockState(CompoundTag)} resolves the block through {@link Registry#BLOCK}.
 * There is no {@code getDataVersion} helper on this version, so only the block state path is replaced.
 */
@Mixin(value = NbtUtils.class, priority = 100000)
public class NbtUtilsMixin {

    @Shadow
    @Final
    private static Logger LOGGER;

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public static BlockState readBlockState(CompoundTag tag) {
        if (tag.tags.get("Name") instanceof StringTag stringTag) {
            Block block = Registry.BLOCK.get(new ResourceLocation(stringTag.data));
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
}
