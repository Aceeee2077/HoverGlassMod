# Hoverglass visual/motion audit — 2026-10-18

Decision: **Compatible**. Inspection precedes implementation. Preserve the local 0.4.1 menu/hotbar work; no reset, namespace change, mod-ID change or replacement renderer is needed.

## Actual repository

`dev.transparentinventory.client` contains `InventoryConfig`/`ConfigLoader` (Gson JSON, atomic save), `ModClient` (only client entrypoint), `anim` (pure Java slide/spring/timeline helpers), `anchor`, `camera`, `compat`, `input`, `layout`, `look`, `render`, `screen`, and `mixin`. Resources contain translations and mixin/mod metadata. There are no Kotlin files, custom shaders, shader programs, post-processing chains, custom framebuffers or custom fullscreen GPU passes. The glass effect is translucent GUI geometry, rims and gradients; pause blur invokes Minecraft's own blur.

Main target: MC 26.2, Fabric Loader 0.19.5, API 0.159.0+26.2, Loom 1.17.20, Java target 25. The user's 26.1.2 target uses API 0.155.3+26.1.2. `gradle/minecraft-targets.json` selects separate jars for 1.20.1–26.3; legacy 1.20/1.21 builds use Java 17/21 and their separate inventory-only implementation. Official Mojang mappings are used for legacy; 26.x is unobfuscated. The wrapper uses Gradle 9.7.1. Dependencies are Minecraft, Fabric Loader/API, and legacy Mixin/MixinExtras; Gson/JOML/SLF4J come from the game/toolchain. No animation library is installed. `settings.gradle` uses Foojay toolchain resolution. Tests are isolated Fabric client game tests plus a plain Java regression main.

## Existing hooks and protected systems

- `ScreenMixin`, container/inventory/creative hooks, GUI extractor/state mixins, picture-in-picture/accessors: extract vanilla screens under one affine transform, tint chrome, preserve items/tooltips and map input with the same inverse matrix. `SpatialScreenManager` retains real menus during F8 overlays and owns closing transitions. Preserve all of this.
- `LiquidGlassRenderer`, button/slider/pause mixins: optional menu theme; `GlassInventoryRenderer`: inventory chrome; `GlassHotbarRenderer`/`HotbarMixin`: HUD chrome only (`Gui` on 26.1, `Hud` from 26.2). Preserve their original alpha scopes and vanilla item drawing.
- `CameraMixin`/`SpatialCameraController`: third-person inventory boom, collision rays and shoulder offset; never player rotation. `AvatarRendererMixin` changes only extracted local avatar head pose during PLAYER inventory sessions. `InventoryLookController` handles smoothing/reset. New camera motion must be a render-pose addition, suspended during screens and third-person transitions.
- `MouseHandler`/`KeyboardHandler`: GUI inverse mapping, modal dispatch and F8. No new input capture is needed.
- `InteractionMixin` observes vanilla `MultiPlayerGameMode` block/entity interactions for anchors; it remains unchanged. `MinecraftMixin` resets sessions on world unload and is extended narrowly for motion cleanup.
- No existing hooks target `ItemInHandRenderer`, `GameRenderer`, `LevelRenderer`, `LivingEntityRenderer`, `ClientPacketListener`, or `LocalPlayer`. No production packet creation/registration, custom damage/cooldown/mining/movement or inventory protocol exists. Calls to original menu/interaction methods retain vanilla networking. Server commands and synthetic input exist only in the test source set.

## Safe Phase 1 plan

1. Add small pure-Java easing, analytic damped spring and reversible item-swap timeline utilities beside existing `anim`; leave proven UI trajectories unchanged.
2. Add an isolated `motion` controller and first-person adapter. Wrap `renderArmWithItem` with a balanced pose scope and temporary visual item cache. Do not touch inventory, equip tick logic, attack logic or packets; bypass maps/use-item animations where vanilla hand coordination matters.
3. Sample motion once per `Camera.update`; apply the resulting small transforms after `GameRenderer.bobView`. Never modify FOV, entity angles, movement or the inventory camera boom. Suspend/reset for world/player changes, menus, pause, spectator, sleeping, riding, flying and third person; honor vanilla bobbing/screen-effect/damage-tilt settings and an explicit reduce-motion option.
4. Observe an outgoing vanilla attack intent, then trigger hit feedback only from its matching existing server damage notification. No new packets or thread freezes.
5. Observe successful local block destruction and `BlockItem.placeBlock`. Emit bounded short-lived vanilla material particles. Placement uses a 200 ms particle proxy, not scaling real chunk geometry; this avoids touching chunk compilation/world rendering. Respect vanilla particle settings and distance/burst limits.
6. Reuse the existing JSON and F8 UI for master/feature/accessibility toggles and intensity. Add focused math/engine regressions, run the complete existing UI/HUD suite, inspect screenshots, and build version adapters without downgrading.

The exact cached Fabric 26.1.2 API was inspected (`fabric-rendering-v1` 23.3.1 and `fabric-events-interaction-v0` 5.2.3). `MotionClient` registers `ClientPlayerBlockBreakEvents.AFTER` and `AttackEntityCallback` (always PASS), avoiding extra game-mode mixins. Build resource expansion adds this client initializer only for spatial targets. Existing anchor hooks remain unchanged.

Fabric's HUD/level-render/item-use extension points do not expose replacement of just the held-item equip parameter, the existing bob-view pose, or the successful block-item placement result. `ItemEvents.USE_ON` is a pre-use callback, not a successful placement notification. Replacing complete renderers via an event would duplicate vanilla logic. Narrow non-overwriting mixins preserve those methods; frame sampling/cleanup stays in existing camera/unload hooks, so no extra tick registration is necessary.

## Compatibility boundaries

New Phase 1 runtime integration is for the existing 26.x spatial branch. Legacy source filtering keeps new Minecraft-dependent packages/mixins out of 1.20/1.21; their existing features remain unchanged. Actual 26.1.2/26.2/26.3 method descriptors must be checked before use, with RenderPearl adapters where required. Other hand/camera mods may append transforms to the same methods; toggles provide a fallback, but compatibility cannot be claimed without testing those combinations. Multiplayer visuals consume existing vanilla state; new server mechanics, trails, explosion shake and model-scale block animation are outside this MVP.
