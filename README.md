# Fast Minecarts

Server-side Fabric mod for Minecraft **26.2**. A minecart with a player inside accelerates to a configurable
top speed and runs over whatever is on the rails. Vanilla clients connect without installing anything.

## Features

- Rideable minecart speeds up while a player is riding it, up to `maxSpeedBlocksPerSecond` (default 40 b/s).
- Powered rails give a configurable impulse; with `maintainHighSpeedOnSparsePoweredRails` roughly one powered
  rail every 10 blocks is enough to stay at top speed.
- Run-over damage: scales with speed (`SCALING`) or fixed (`FLAT`), with knockback, a per-entity cooldown and
  filters for players, hostile/passive/tamed mobs, armour stands, item frames, other vehicles and so on.
- Nothing is registered — no items, blocks, entities, packets or GUI. Only a `@Unique` cooldown field mixed
  into `LivingEntity`, in memory only.

## Install

1. Fabric Loader ≥ 0.20.1 and Fabric API on the **server** (single-player worlds work too).
2. Put `fastminecarts-1.1.0.jar` into `mods/`.
3. Config is generated at `config/fastminecarts.json` on first start.

## Config (main knobs)

| key | default | what |
|---|---|---|
| `enabled` | `true` | master switch |
| `maxSpeedBlocksPerSecond` | `40.0` | target top speed |
| `hardMaxSpeedBlocksPerSecond` | `48.0` | hard clamp |
| `requirePlayerPassenger` | `true` | only accelerate with a player inside |
| `onlyVanillaMinecart` | `true` | skip modded minecart types |
| `poweredRailBoostMultiplier` | `4.0` | powered rail impulse |
| `accelerationMultiplier` | `1.5` | general acceleration |
| `runoverDamageEnabled` | `true` | damage entities on the rails |
| `runoverDamageMinSpeed` | `20.0` | below this speed nothing is hit |
| `runoverDamageMode` | `SCALING` | `SCALING` or `FLAT` |
| `runoverDamageBase` / `PerSpeed` / `Max` | `4.0` / `0.8` / `20.0` | damage formula |
| `runoverKnockbackStrength` | `2.5` | knockback |
| `damagePlayers` / `damageHostileMobs` / `damagePassiveMobs` / `damageTamedMobs` | `true` | who gets hit |

## Build

```
./gradlew build
```

26.2 ships unobfuscated, so Loom needs no mappings: the project uses `net.fabricmc.fabric-loom`,
`implementation` instead of `modImplementation`, and `release = 25` (JDK 25).

## License

MIT.
