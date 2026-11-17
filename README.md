# Fast Minecarts

Server-side Fabric mod for Minecraft **26.2**. A minecart with a player inside accelerates to a configurable
top speed and knocks living entities off the rails. Vanilla clients connect without installing anything.

## Features

- Rideable minecart speeds up while a player is riding it, up to `maxSpeedBlocksPerSecond` (default 40 b/s).
  The boost only works while the cart is on rails — leave the track and vanilla physics take over.
- Powered rails give a configurable impulse; with `maintainHighSpeedOnSparsePoweredRails` roughly one powered
  rail every 10 blocks is enough to stay at top speed.
- Anything living in front of a moving cart is thrown aside (knockback, configurable strength). Dropped items,
  arrows and other minecarts are left alone; creative and spectator players are never hit.
- Nothing is registered — no items, blocks, entities, packets or GUI. Only two mixins: `AbstractMinecart` and
  `NewMinecartBehavior`.

## Install

1. Fabric Loader ≥ 0.20.1 and Fabric API on the **server** (single-player worlds work too).
2. Put `fastminecarts-1.2.0.jar` into `mods/`.
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
| `maintainHighSpeedOnSparsePoweredRails` | `true` | keep top speed between powered rails |
| `runoverKnockbackStrength` | `2.5` | horizontal knockback |
| `runoverKnockbackVertical` | `0.4` | vertical knockback |

## Build

```
./gradlew build
```

26.2 ships unobfuscated, so Loom needs no mappings: the project uses `net.fabricmc.fabric-loom`,
`implementation` instead of `modImplementation`, and `release = 25` (JDK 25).

## License

MIT.
