# Fast Minecarts

Server-side Fabric mod for Minecraft **26.2**. A minecart with a player inside accelerates to a configurable
top speed and knocks living entities off the rails. Vanilla clients connect without installing anything.

## Features

- Rideable minecart speeds up while a player is riding it, up to `maxSpeedBlocksPerSecond` (default 40 b/s).
  The boost only works while the cart is on rails — leave the track and vanilla physics take over.
- With `maintainHighSpeedOnSparsePoweredRails` (default) the cart is held at top speed on every straight rail,
  powered or not. Turn it off and only powered rails put it back to top speed — one instant push per powered
  rail — and the cart coasts down between them.
- Anything living in front of a moving cart is thrown aside (knockback, configurable strength, switchable
  with `runoverKnockbackEnabled`). Dropped items, arrows and other minecarts are left alone; creative and
  spectator players are never hit.
- Nothing is registered — no items, blocks, entities, packets or GUI. Only two mixins: `AbstractMinecart` and
  `NewMinecartBehavior`.

## Install

1. Fabric Loader ≥ 0.20.1 and Fabric API on the **server**. `fabric.mod.json` declares
   `"environment": "server"`, so the loader does not even load this mod in a single-player client — a
   dedicated server (or a LAN server with the jar on the host machine) is required.
2. Put `fastminecarts-1.2.1.jar` into `mods/`.
3. Config is generated at `config/fastminecarts.json` on first start.

## Config (main knobs)

| key | default | what |
|---|---|---|
| `enabled` | `true` | master switch |
| `maxSpeedBlocksPerSecond` | `40.0` | target top speed |
| `hardMaxSpeedBlocksPerSecond` | `48.0` | ceiling the config clamps `maxSpeedBlocksPerSecond` to |
| `requirePlayerPassenger` | `true` | only accelerate with a player inside |
| `onlyVanillaMinecart` | `true` | skip modded minecart types |
| `maintainHighSpeedOnSparsePoweredRails` | `true` | keep top speed between powered rails |
| `runoverKnockbackEnabled` | `true` | throw living entities off the rails |
| `runoverKnockbackStrength` | `2.5` | horizontal knockback |
| `runoverKnockbackVertical` | `0.4` | vertical knockback |

Knockback has floors built in: horizontal never drops below `1.5` and vertical below `0.6`, so small
values thin the push out instead of turning it off — use `runoverKnockbackEnabled` for that.

## Build

```
./gradlew build
```

26.2 ships unobfuscated, so Loom needs no mappings: the project uses `net.fabricmc.fabric-loom`,
`implementation` instead of `modImplementation`, and `release = 25` (JDK 25).

## License

MIT.
