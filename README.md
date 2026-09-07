# Mianbao - Sable Explosion Compatibility 1.2.0

NeoForge 1.21.1 add-on, built against NeoForge 21.1.248, Sable 2.0.5,
and Mianbao's NewModernWarfare jar 2.4.10 (internal mod version 2.4.5-neoforge).

## What is bridged

- Mianbao's direct explosion block destruction, including calls outside
  Minecraft's Explosion implementation.
- Cross-space block queries: a world-space sample can find a Sable block,
  and a plot-local sample can find terrain or another construction.
- Fire and material-matched scorching; controller block states and ordnance
  are not copied into the other coordinate space.
- Plot-local searches for entities affected by the selected explosion
  procedures are centered at the actual world position.
- Terrain chunks touched by block or entity queries are loaded with temporary
  tickets that expire after 100 ticks. The covered chunk rectangle follows the
  actual queried destruction area, including negative and boundary coordinates.
- Rotation, translation, rotation point and nonuniform scale via Sable's pose
  matrix. A source pose is retained during nested calls even if the explosion
  removes the source construction.
- Delayed Mianbao callbacks retain the construction and physical blast origin.
  A removed plot cannot later damage a new construction that reuses its storage.
- Nuclear secondary explosions run at the physical blast location. Nuclear
  particles preserve world-up, so a rotated construction cannot tip the cloud.

The original Level.explode call is moved from Sable's storage plot to physical
space and still runs exactly once. The add-on never replays a whole explosion
procedure and never creates a second vanilla explosion, sound, particle burst
or damage event. Plot-local Mianbao `forceload` commands are suppressed because
the add-on loads the exact physical chunk area with temporary tickets.
It does not alter projectile flight, launchers, unrelated TNT or the flight
and chunk-transit mods. It does not continuously load chunks around projectiles.

## Damage and performance boundaries

Custom block loops keep their original sample geometry and conditions.
Projected destination **block centers** are sampled, rather than damaging
everything in a construction's bounding box. This is Minecraft voxel sampling,
not a continuous mesh/armor simulation. The original source operation retains
Mianbao's semantics. Additional targets honor unbreakable blocks and Mianbao's
`no_block` tag; direct destroyBlock also follows ExplosionBlockGuard's
hardness/fluid exclusions. Scorching is copied only onto matching materials.
This does not add shielding/pressure physics missing from Mianbao's own loops.

Only physically intersecting constructions are queried. Repeated queries of
one cell reuse its projected addresses for the duration of the procedure;
block states are always reread, and writes invalidate the address cache.
There is no every-tick scan, persistent chunk ticket or projectile loader.
Explosion tickets are added only for terrain chunks the current blast touches
and expire automatically after five seconds. Loading previously unloaded terrain
can cause a short pause for large explosions. Do not expect large Mianbao nuclear
explosions to be free of tick cost: this add-on does not replace their original
work loops.

## Installation and configuration

Install `mianbao-sable-explosion-compat-1.2.0.jar` in the instance's mods
directory and restart Minecraft/server. It contains no new blocks or recipes.
The server config is created in:

`<world>/serverconfig/mianbao_sable_explosion_compat-server.toml`

Options (all true by default): `enabled`, `thermalEffects`,
`entityQueries`, `loadExplosionChunks`. No existing mod config is modified.

## Verification

Run with Java 21:

```
gradlew.bat build -PsableJar=<path-to-sable-2.0.5.jar> -PmianbaoJar=<path-to-mianbaos_modernwarfare-2.4.10-neoforge.jar>
```

The build checks coordinate projection (negative/world/plot coordinates,
rotation, scale and adjacent-cell ownership) and checks every selected Mianbao
procedure hook against the actual jar 2.4.10 bytecode. This does not substitute for
a transformed-mod in-game integration test.

In a disposable test world, compare:

1. The same bomb on terrain and on a Sable platform beside a stone wall.
2. A terrain explosion beside a platform, including a rotated platform.
3. Two nearby constructions, with a blast originating on one of them.
4. Fire/scorching across the boundary, with bedrock/protected blocks nearby.
5. A moving platform and a rack chain reaction (no duplicate salvo/effects).

No destructive in-game test is performed automatically in the user's world.
