# Fast NBT

NBT performance optimizations for **Minecraft 1.21.1 / NeoForge**.

This mod optimizes various methods in the `CompoundTag` class:

- **All kinds of `get` methods.** In the vanilla game it first calls `contains` to check whether the key
  exists, then invokes `get`, which is two map lookups. This mod merges those two operations into one and
  applies inlining, which greatly improves the lookup speed.
- **Iteration.** Iterating with `keySet` paired with `get` is replaced by an approach that walks keys and
  values simultaneously, avoiding a large number of lookups during iteration.
- **Related classes.** Operations that use those `get` methods in other related classes are optimized too.

Chunk and block state decoding get the same treatment — vanilla's extra lookups and codec overhead are
skipped there as well.

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/fast-nbt)

This is a mixin-based mod: it rewrites vanilla methods. Everything is switchable in `config/fastnbt.toml`.

## Features

| Feature | Default | What it does |
|---|---|---|
| `blockState` | on | `NbtUtils.readBlockState` and `getDataVersion` take `Name`/`Properties` straight from the backing map |
| `blockStateCodec` | on | Chunk section palettes (`BlockState.CODEC`) are decoded from the map instead of walking the DFU codec chain. The largest win |
| `nbtIo` | on | Unlimited NBT readers skip a byte-accounting pass whose result is discarded — every chunk load, level.dat and playerdata |
| `nbtAccounter` | on | Incompatible with Packet Fixer — both mods modify the same NBT size / depth limits. If you use Packet Fixer, disable nbtAccounter (keep nbtIo on). Removes `NbtAccounter`'s byte quota, depth limit and UTF length scan entirely, and reuses one accounter for unlimited reads instead of allocating one per read. Faster than `nbtIo`, but nothing caps the size or nesting of the NBT that is read any more — see the notes |

`CompoundTag.write`, `merge` and the `get*` family are also rewritten to walk the tag map directly, with no
`contains()` + `get()` pair. That mixin has no feature key: only the global switch controls it.

## What it improves

The `CompoundTag` savings above are the same idea; the other paths give up this much per call:

| Path | Vanilla | Here |
|---|---|---|
| `NbtUtils.readBlockState` | `contains` + `getString` per property | one lookup per property |
| chunk section palettes | the full DFU chain per entry: dispatch codec, registry lookup, one `MapCodec` per property, plus `DataResult`/`Either`/`Pair`/`Optional` allocations | `Name`/`Properties` read straight from the map |
| chunk, level.dat and playerdata reads | a byte accounting scan over every key and string | skipped when the accounter is unlimited |

So the gains land where NBT is decoded in bulk: **world and chunk loading**, **server autosaves** (which
re-encode every chunk), and **block entity and entity payloads**. Fewer temporary objects on the palette path
also means less GC pressure.

It is not a general performance mod: it touches nothing outside NBT, and it cannot help a workload that is
bounded by disk or network. The savings scale with how much NBT a pack decodes — smallest in a plain
vanilla world. No benchmark numbers are claimed here; profile your own pack if you want numbers for it.

## Configuration

`config/fastnbt.toml` is regenerated on every launch:

```toml
enabled = true                  # master switch for all of the above

[features.blockStateCodec]
    enabled = true
```

Feature keys are camelCase.

## Notes

- The 1.20.1 branch also has an `itemStack` feature. Item NBT became data components in 1.20.5, so on 1.21.1
  an `ItemStack` has no `tag`/`capNBT` fields and no `ItemStack(CompoundTag)` constructor to optimize, and
  that feature has no counterpart here.
- `nbtAccounter` (on by default) removes the limits vanilla puts on NBT reads: the 2 MB quota on the
  client-bound packet codecs, the 100 MB level.dat quota and the 512-deep nesting limit. It is the fastest
  option, but a server then accepts arbitrarily large tags from clients, and deeply nested tags recurse
  instead of being rejected with a clean exception. Turn it off — with `nbtIo` left on — if that matters.
  While it is on, `nbtIo`'s mixin is skipped, because both replace the same method.
- Only canonical data takes a fast path; malformed input is reported as a decode error instead of being
  guessed at. One deliberate exception to vanilla: an unknown block id is an error here where vanilla resolved
  it to air, so chunk palette decoding promotes it to air itself and logs one line per bad entry.
- `blockStateCodec` never touches encoding, so saved chunks and packets are byte-identical, and `Properties`
  keys the block does not have are ignored, as in vanilla.
- Every rewritten target is an `@Overwrite` or inject with no `.safe.` variant; other NBT-caching mods should
  be tested alongside this one.
- `blockStateCodec` replaces the `BlockState.CODEC` field itself, at the end of `BlockState`'s static
  initializer. A mod that captures that field earlier simply won't get the fast path.

## Building

```bash
./gradlew build       # jar in build/libs/
./gradlew runClient   # dev client, run/client
./gradlew runServer   # dev server (--nogui), run/server - needs run/server/eula.txt
```

Requires Minecraft 1.21.1, NeoForge 21.1.x and Java 21. On startup each applied mixin logs
`ApplyMixin: <class>`; a default config applies 4 of them.

## License

GNU Lesser General Public License v3.0 or later — see [LICENSE.txt](LICENSE.txt).
