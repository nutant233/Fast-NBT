package com.fast.fastnbt.blockstatecodec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * Fast decode path for {@link BlockState#CODEC}, the codec behind chunk section palettes
 * (ChunkSerializer's BLOCK_STATE_CODEC -> PalettedContainer.codecRW) as well as data pack / structure use.
 *
 * <p>Vanilla decodes every palette entry through the full DFU chain: KeyDispatchCodec -> registry lookup ->
 * a MapCodec per property, each step allocating DataResult/Either/Pair/Optional objects. The canonical NBT
 * form is small and fully determined by the backing map, so it is resolved directly here.
 *
 * <p>The canonical form is {@code {Name: "<id>", Properties: {<name>: "<value>"}}}. Anything else fails the
 * same way vanilla fails it: {@code NbtOps.getStringValue} only accepts string tags, {@code orCompressed}
 * picks the name codec (not the numeric id one) whenever {@code compressMaps()} is false, and keys that are
 * not properties of the block are never read by the property codec at all. Non-NBT ops and encoding still go
 * through the vanilla codec, so data packs and saved NBT stay unchanged.
 */
public final class FastBlockStateCodec {

    @Nullable
    private static Codec<BlockState> vanilla;

    private FastBlockStateCodec() {
    }

    /**
     * Replaces the given codec with the fast wrapper; called from BlockState's static initializer.
     */
    public static Codec<BlockState> install(Codec<BlockState> original) {
        vanilla = original;

        Encoder<BlockState> encoder = original::encode;
        Decoder<BlockState> decoder = FastBlockStateCodec::decode;
        return Codec.of(encoder, decoder, "fastnbt:block_state");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> DataResult<Pair<BlockState, T>> decode(DynamicOps<T> ops, T input) {
        Codec<BlockState> original = vanilla;
        if (original == null) {
            return DataResult.error(() -> "Fast NBT: BlockState codec was not installed");
        }
        // JSON and any other ops have to keep using the vanilla codec.
        if (ops != NbtOps.INSTANCE) {
            return original.decode(ops, input);
        }
        if (!(input instanceof CompoundTag tag)) {
            return DataResult.error(() -> "Not a compound tag: " + input);
        }

        Tag nameTag = tag.tags.get("Name");
        if (!(nameTag instanceof StringTag name)) {
            return DataResult.error(() -> "Missing or non-string Name in block state: " + tag);
        }
        ResourceLocation id = ResourceLocation.tryParse(name.data);
        if (id == null) {
            return DataResult.error(() -> "Not a valid resource location: " + name.data);
        }

        // An unknown id is not a block. Vanilla's byNameCodec() resolves it to air through the defaulted
        // registry; here it is a decode error, which chunk palette decoding promotes to air on its own.
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        if (block == null) {
            return DataResult.error(() -> "Not a valid block: " + name);
        }
        StateDefinition<Block, BlockState> definition = block.getStateDefinition();
        BlockState state = block.defaultBlockState();

        // StateHolder.codec() uses Codec.unit for blocks without properties, which ignores Properties entirely.
        if (definition.getProperties().isEmpty()) {
            return DataResult.success(Pair.of(state, input));
        }

        Tag propertiesTag = tag.tags.get("Properties");
        if (propertiesTag == null) {
            return DataResult.success(Pair.of(state, input));
        }
        if (!(propertiesTag instanceof CompoundTag properties)) {
            return DataResult.error(() -> "Not a compound tag: " + propertiesTag);
        }

        for (var entry : properties.tags.entrySet()) {
            if (!(entry.getValue() instanceof StringTag value)) {
                return DataResult.error(() -> "Not a string: " + entry.getValue());
            }
            Property<?> property = definition.getProperty(entry.getKey());
            if (property == null) {
                // The vanilla property codec only reads the properties it knows, so unknown keys are ignored.
                continue;
            }
            Optional<?> parsed = property.getValue(value.data);
            if (parsed.isEmpty()) {
                return DataResult.error(() -> "Unable to read property: " + property + " with value: " + value.data + " for block state: " + tag);
            }
            try {
                // Every neighbour state differs from its source in exactly one property and StateDefinition
                // interns one state per property->value map, so the order of application does not matter.
                state = state.setValue((Property) property, (Comparable) parsed.get());
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "Cannot set property " + property + " to " + parsed.get() + " on " + block + ": " + e.getMessage());
            }
        }

        return DataResult.success(Pair.of(state, input));
    }
}
