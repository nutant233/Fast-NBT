# Fast NBT

NBT performance optimizations for **Minecraft 1.20.1 / Forge**.

This mod optimizes various methods in the `CompoundTag` class:

- **All kinds of `get` methods.** In the vanilla game it first calls `contains` to check whether the key
  exists, then invokes `get`, which is two map lookups. This mod merges those two operations into one and
  applies inlining, which greatly improves the lookup speed.
- **Iteration.** Iterating with `keySet` paired with `get` is replaced by an approach that walks keys and
  values simultaneously, avoiding a large number of lookups during iteration.
- **Related classes.** Operations that use those `get` methods in other related classes are optimized too.

Chunk, block state and `ItemStack` decoding get the same treatment — vanilla's extra lookups and codec
overhead are skipped there as well.

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/fast-nbt)

This is a mixin-based mod: it rewrites vanilla methods. Everything is switchable in `config/fastnbt.toml`.

## Features

| Feature | Default | What it does |
|---|---|---|
| `itemStack` | on | `ItemStack(CompoundTag)` reads `tag`/`ForgeCaps` from the backing map instead of probing with two `contains()` calls; `getOrCreateTagElement`, `getTagElement` and `removeTagKey` resolve in one lookup |
| `blockState` | on | `NbtUtils.readBlockState` and `getDataVersion` take `Name`/`Properties` straight from the backing map |
| `blockStateCodec` | on | Chunk section palettes (`BlockState.CODEC`) are decoded from the map instead of walking the DFU codec chain. The largest win |
| `nbtIo` | on | Unlimited NBT readers skip a byte-accounting pass whose result is discarded — every chunk load, level.dat and playerdata |

`CompoundTag.write`, `merge` and the `get*` family are also rewritten to walk the tag map directly, with no
`contains()` + `get()` pair. That mixin has no feature key: only the global switch controls it.

## What it improves

The `CompoundTag` savings above are the same idea; the other paths give up this much per call:

| Path | Vanilla | Here |
|---|---|---|
| `ItemStack(CompoundTag)` | two `contains(key, type)` probes for `tag`/`ForgeCaps` | direct map reads, no probe |
| `NbtUtils.readBlockState` | `contains` + `getString` per property | one lookup per property |
| chunk section palettes | the full DFU chain per entry: dispatch codec, registry lookup, one `MapCodec` per property, plus `DataResult`/`Either`/`Pair`/`Optional` allocations | `Name`/`Properties` read straight from the map |
| chunk, level.dat and playerdata reads | a byte accounting scan over every key and string | skipped when the accounter is unlimited |

So the gains land where NBT is decoded in bulk: **chunk and world loading**, **server autosaves** (which
re-encode every chunk), and **inventory, block entity and entity payloads** that carry tagged `ItemStack`s.
Fewer temporary objects on the palette path also means less GC pressure.

It is not a general performance mod: it touches nothing outside NBT, and it cannot help a workload that is
bounded by disk or network. The savings scale with how much NBT a pack decodes — smallest in a plain
vanilla world. No benchmark numbers are claimed here; profile your own pack if you want numbers for it.

## Configuration

`config/fastnbt.toml` is regenerated on every launch:

```toml
enabled = true                  # master switch for all of the above

[features.itemStack]
    enabled = true
```

Feature keys are camelCase.

## Notes

- Only canonical data takes a fast path; malformed input is reported as a decode error instead of being
  guessed at. One deliberate exception to vanilla: an unknown block id is an error here where vanilla resolved
  it to air, so chunk palette decoding promotes it to air itself and logs one line per bad entry.
- `blockStateCodec` never touches encoding, so saved chunks and packets are byte-identical, and `Properties`
  keys the block does not have are ignored, as in vanilla.
- Every rewritten target is an `@Overwrite`, redirect or inject with no `.safe.` variant; other NBT-caching
  mods should be tested alongside this one.
- `blockStateCodec` replaces the `BlockState.CODEC` field itself, at the end of `BlockState`'s static
  initializer. A mod that captures that field earlier simply won't get the fast path.

## Building

```bash
./gradlew build      # jar in build/libs/
./gradlew runClient  # dev client, shared run/client directory
```

Requires Minecraft 1.20.1, Forge 47.x and Java 17. On startup each applied mixin logs `ApplyMixin: <class>`;
a default config applies 5 of them.

## License

GNU Lesser General Public License v3.0 or later — see [LICENSE.txt](LICENSE.txt).
