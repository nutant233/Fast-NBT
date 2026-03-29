package com.fast.fastnbt.mixin;

import net.minecraft.nbt.*;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

@Mixin(CompoundTag.class)
public abstract class CompoundTagMixin {

    @Final
    @Shadow
    public Map<String, Tag> tags;

    @Shadow
    private static void writeNamedTag(String name, Tag tag, DataOutput output) {
    }

    /**
     * @author nutant233
     * @reason forEach accelerate iteration
     */
    @Overwrite
    public void write(DataOutput output) throws IOException {
        this.tags.forEach((tagName, tag) -> writeNamedTag(tagName, tag, output));
        output.writeByte(0);
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public CompoundTag merge(CompoundTag other) {
        other.tags.forEach((s, tag) -> {
            if (tag instanceof CompoundTag compoundTag) {
                tags.compute(s, (k, v) -> {
                    if (v instanceof CompoundTag c) {
                        c.merge(compoundTag);
                        return c;
                    }
                    return compoundTag.copy();
                });
            } else {
                tags.put(s, tag.copy());
            }
        });
        return (CompoundTag) (Object) this;
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public byte getByte(String key) {
        if (tags.get(key) instanceof NumericTag numericTag) {
            return numericTag.getAsByte();
        }
        return 0;
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public short getShort(String key) {
        if (tags.get(key) instanceof NumericTag numericTag) {
            return numericTag.getAsShort();
        }
        return 0;
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public int getInt(String key) {
        if (tags.get(key) instanceof NumericTag numericTag) {
            return numericTag.getAsInt();
        }
        return 0;
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public long getLong(String key) {
        if (tags.get(key) instanceof NumericTag numericTag) {
            return numericTag.getAsLong();
        }
        return 0L;
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public float getFloat(String key) {
        if (tags.get(key) instanceof NumericTag numericTag) {
            return numericTag.getAsFloat();
        }
        return 0.0F;
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public double getDouble(String key) {
        if (tags.get(key) instanceof NumericTag numericTag) {
            return numericTag.getAsDouble();
        }
        return 0.0;
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public String getString(String key) {
        var tag = tags.get(key);
        if (tag instanceof StringTag stringTag) {
            return stringTag.data;
        }
        return "";
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public byte[] getByteArray(String key) {
        if (tags.get(key) instanceof ByteArrayTag byteTags) {
            return byteTags.data;
        }
        return ArrayUtils.EMPTY_BYTE_ARRAY;
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public int[] getIntArray(String key) {
        if (tags.get(key) instanceof IntArrayTag intTags) {
            return intTags.data;
        }
        return ArrayUtils.EMPTY_INT_ARRAY;
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public long[] getLongArray(String key) {
        if (tags.get(key) instanceof LongArrayTag longTags) {
            return longTags.data;
        }
        return ArrayUtils.EMPTY_LONG_ARRAY;
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public CompoundTag getCompound(String key) {
        if (tags.get(key) instanceof CompoundTag compoundTag) {
            return compoundTag;
        }
        return new CompoundTag();
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public ListTag getList(String key, int tagType) {
        if (tags.get(key) instanceof ListTag listTag) {
            if (listTag.type != tagType && !listTag.list.isEmpty()) {
                return new ListTag();
            }
            return listTag;
        }
        return new ListTag();
    }

    /**
     * @author nutant233
     * @reason optimize
     */
    @Overwrite
    public boolean getBoolean(String key) {
        if (tags.get(key) instanceof NumericTag numericTag) {
            return numericTag.getAsByte() != 0;
        }
        return false;
    }
}
