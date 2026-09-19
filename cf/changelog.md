## 1.20-26.9.1-forge

### Fixed

- **NBT data loss in `CompoundTag.merge`.** The merge of a nested compound mutated the target and then
  replaced it with a copy of the incoming tag, so every key that existed only in the target disappeared.
  This affects anything that merges NBT: `/data merge`, the loot table NBT functions (`set_nbt`,
  `set_contents`), placing a block with a `BlockEntityTag`, spawn eggs carrying an `EntityTag`, and every mod
  that merges into its own NBT or saved data. This is the only data-loss fix since 1.2 — see
  [#4](https://github.com/nutant233/Fast-NBT/issues/4).

### Added

- **Configuration.** `config/fastnbt.toml` is generated on first launch and gives every optimization its own
  switch, plus a global `enabled`. Injection is decided by the mixin config plugin, so turning a feature off
  puts the vanilla method back instead of just skipping it at runtime.
- **`blockStateCodec`** — chunk section palettes resolve the canonical `{Name, Properties}` tag directly
  instead of walking the full DFU codec chain (dispatch codec, registry lookup, one `MapCodec` per property,
  plus `DataResult` / `Either` / `Pair` / `Optional` allocations) on every chunk load and save. Decoding only:
  encoding is untouched, so saved chunks and network packets stay byte-identical.
- **`nbtIo`** — unlimited NBT readers skip a byte-accounting pass whose result is discarded. That covers every
  chunk load, plus level.dat and playerdata.
- **`itemStack` and `blockState`** — the existing ItemStack and block-state NBT paths are now switchable
  features instead of always-on.

### Changed

- The `ItemStack#isEnchanted` replacement was removed; vanilla's implementation is used again.
- The `CompoundTag` rewrites have no separate switch — the global `enabled` flag controls them.
- All mixins now carry `priority = 100000`.

### Notes

- Only canonical data takes a fast path. Non-canonical data is reported as a decode error rather than guessed
  at, with one deliberate exception to vanilla: an unknown block id is an error here where vanilla resolved it
  to air, so chunk palette decoding promotes it to air itself and logs one line per entry.
- Full description, configuration and compatibility notes:
  [README](https://github.com/nutant233/Fast-NBT#readme).
