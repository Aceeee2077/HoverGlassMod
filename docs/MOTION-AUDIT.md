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

## Delivered Phase 1 — 0.5.0

| Feature | Status on tested 26.1.2 client |
| --- | --- |
| Smooth held-item switching | Working — separate visual cache per hand; actual slot/item changes immediately |
| Sword/tool attack motion | Working — additional render transform using vanilla swing progress |
| Sprint camera motion | Working — speed envelope and small render-pose sway |
| Jump feedback | Working — upward transition impulse |
| Landing / hard landing | Working — velocity-scaled spring response |
| Confirmed melee hit | Working — matching existing server damage event; whiffs/rejected hits excluded |
| Block break feedback | Working — native after-break event with extra bounded material fragments |
| Block placement animation | Partial — 200 ms material-particle proxy; actual chunk block is not scaled |

The mod remains client-only. All original attack, cooldown, inventory, collision, fall damage, movement and placement logic executes normally. No custom packets, protocol, tick freezing, FOV changes, shader, framebuffer, fullscreen pass or new third-party dependency was added. The controller reuses transform matrices; item copies happen on visual item changes. Particle objects are allocated only at interactions. Particle caps are 16 per burst / 48 owned / at least 40 ms between bursts. No hardware FPS/allocation benchmark was performed.

### New source files

Under `src/main/java/dev/transparentinventory/client/`:

- `anim/Easing.java`, `anim/DampedSpring.java`, `anim/ItemSwapTimeline.java` — engine-free shared math and timing.
- `motion/MotionClient.java`, `motion/FirstPersonMotion.java`, `motion/VisualMotionController.java` — event registration, visual held-item state and camera response.
- `effects/BlockFeedback.java` — bounded material-particle feedback and cleanup.
- `mixin/FirstPersonMotionMixin.java`, `mixin/MotionViewMixin.java`, `mixin/MotionDamageMixin.java`, `mixin/BlockPlaceFeedbackMixin.java` — the four additional mixin entries.
- `src/versions/26.1/java/dev/transparentinventory/client/mixin/FirstPersonMotionMixin.java` — 26.1 render-method adapter.
- `src/versions/26.3/java/dev/transparentinventory/client/mixin/FirstPersonMotionMixin.java`, `MotionViewMixin.java` — extracted hand/model-state adapter. Models are resolved again only when the visual item differs from the prepared vanilla model.
- `src/gametest/java/dev/transparentinventory/gametest/MotionGameTest.java` — disposable-world runtime tests.

### Existing files modified for this task

- `InventoryConfig.java`: nested, sanitized motion options in the existing Gson file.
- `screen/SpatialSettingsScreen.java`: three more pages with master/accessibility, first-person, camera and world controls.
- `mixin/CameraMixin.java`, `mixin/MinecraftMixin.java`: frame sampling and lifecycle cleanup. Existing anchor/interaction hooks remain unchanged.
- `build.gradle`, `gradle.properties`, `src/main/resources/fabric.mod.json`: conditional spatial client initializer, valid source JSON, version 0.5.0 and feature description; loader/game/Java/API versions unchanged.
- `transparent_inventory.client.mixins.json`, both translation files: four new hooks and translated settings.
- `InventoryRegressionTest.java`: 30 additional assertions for spring frame partitioning, timeline endpoints/rapid selection/dropped frames and config migration/clamping.
- Test metadata, `SpatialUiGameTest.java`, and both `TestWorldAccess.java` versions: register motion tests, support a focused run, and adapt chat/toasts/HUD visibility for 26.2 without changing test semantics on 26.1.
- `README.md`, `README.zh-CN.md`, `docs/ARCHITECTURE.md`, `docs/TESTING.md`, this audit and selected real-client screenshots document implementation and limits.

Existing uncommitted glass-menu and hotbar work from before this task was retained.

### Verification scope

26.1.2: full vanilla/Fabric client and integrated-server test run passed, including all 25 original UI/container checks, glass menu/HUD checks and the new motion scenarios; 1032 plain-Java assertions passed. Tests cover rapid switching and unchanged real inventory, native sword swings, sprinting, bobbing off, screen effects off, normal/hard landing, confirmed/rejected attacks, actual server block break/place, minimal particles, map/drink/bow bypass, F8 per-feature/reduced-motion persistence, third person, spectator and world unload. Screenshots were inspected. See `TESTING.md` for exact final commands/results.

Build checks passed for 26.1, 26.1.1, 26.2, 1.20.1 and 1.21.11. 26.2 also compiles both client test suites. 26.3 compiles/assembles and passes the 1032 math assertions on a plain JVM; its real client was not run because the offline environment lacks its LWJGL 3.4.3 native dependencies. Third-party modpacks/camera or hand-rendering mod combinations and separate remote-server latency conditions were not tested. Hit matching intentionally tracks only the most recent melee intent for one second.

## Delivered Phase 2 — 0.6.0 avatar motion (2026-10-26)

Scope: extend the 0.5.0 first-person layer with **third-person player-body motion** only. Inventory chrome, glass rendering, camera anchors, input mapping and the existing first-person item timeline were not changed.

### What was added

| Feature | Behaviour |
| --- | --- |
| Full-body weapon swing | Small additive torso twist/lean, head counter-rotation and off-hand/step offsets layered over vanilla `attackTime`. Sword, axe, spear, pickaxe/shovel/hoe and unarmed families use different budgets. Every visible player is covered because `attackTime`/`SwingDescription` is replicated. |
| Workbench crafting loop | Local player only: while `Minecraft.player.containerMenu instanceof CraftingMenu`, both arms work at bench height, the head looks down and the torso leans in a reversible sine loop. No recipe or menu state is written. |
| Armor follow | The hook is on `HumanoidModel.setupAnim(HumanoidRenderState)`, not only `PlayerModel`, so the equipment layer's separate armor models receive the same additive pose. The callback ignores non-`AvatarRenderState` humanoids. |
| Configuration | New `playerAnimations`, `playerAttackAnimation`, `craftingAnimation` and `playerIntensity` (0–1, default 0.8) in the existing JSON. F8 gained a ninth page, **Avatar motion**; the master motion switch and **Reduce motion** still gate everything. |
| Accessibility / safety | Skips spectator, invisible, dead, elytra, swimming, passenger, spin-attack, item-use and sleeping states. Values are finite and bounded; 0 intensity returns the exact identity pose. |

### Hook and version boundary

`PlayerModel.setupAnim(AvatarRenderState)` already delegates to `HumanoidModel.setupAnim(HumanoidRenderState)`. The shared hook injects at the tail of the latter, which covers both the player model and armor models without replacing vanilla walking/riding/use branches. `Model.setupAnim` resets poses before each call, so repeated layer/render setup cannot accumulate the additive rotation.

26.1.x/26.2 expose `attackTime`, `attackArm` and `swingAnimationType`. 26.3 replaced them with `currentSwing` (`LivingEntity.SwingDescription`) and a 0–1 `swingAnimation`; `src/versions/26.3/.../AvatarAttackView.java` adapts those fields and clamps the one-frame 1→0 wrap. No runtime packet, renderer replacement or model state write was added.

Crafting motion is intentionally local-only. A client-only mod has no visibility into another player's open menu, so remote crafting would require custom networking, which remains out of scope. The existing third-person container camera makes the local bench loop visible during normal crafting.

### New source files

- `motion/AvatarMotionMath.java` — pure-Java attack/crafting poses and intensity clamps.
- `motion/AvatarWeaponStyle.java` — replicated swing type + item tags to motion family.
- `motion/AvatarAttackView.java` — 26.1/26.2 swing fields; overridden in `src/versions/26.3/java` for 26.3.
- `motion/PlayerAnimationController.java` — state gating and additive `ModelPart` application.
- `mixin/PlayerAnimationMixin.java` — tail hook on `HumanoidModel.setupAnim`.

Modified: `InventoryConfig`, `SpatialSettingsScreen`, the spatial mixin config, both language files, `fabric.mod.json`, `gradle.properties` (0.6.0), READMEs, architecture/testing docs and the plain-Java regression suite.

### Verification scope

Compiled for 26.1.2, 26.2 and 26.3. `physicsTest` on 26.2 passed **1,165 assertions**, including attack endpoints, finite/bounded poses, left/right symmetry, linear intensity, idle-step suppression, crafting loop continuity, invalid time and config clamping. No Minecraft client was launched for this phase, per the user's no-game-launch constraint; screenshots and live gameplay validation remain outstanding. Third-party custom player/armor models, multiplayer latency and shader combinations were not tested.

## Delivered Phase 3 — 0.6.1 sprint stride (2026-10-27)

Scope: one additive layer on the 0.6.0 avatar hook. Inventory chrome, glass rendering, camera, anchors, input and the existing attack/crafting poses were not changed.

### What was added

| Feature | Behaviour |
| --- | --- |
| Sprint stride | While an avatar is sprinting, legs open wider in phase with the vanilla walk cycle, arms pump in counter-phase with a slight elbow-out roll, the torso leans forward and the head counter-rotates to keep the gaze level. |
| All visible players | Sprint is a replicated entity flag; the controller resolves it from `ClientLevel.getEntity(state.id).isSprinting()` because `AvatarRenderState` has no sprint field. |
| Running attacks | The attack pose owns the upper body, but the stride's leg offsets continue underneath, so spam-clicking while sprinting no longer drops the footwork back to vanilla. |
| Configuration | `motion.sprintStride` (default true) on the F8 **Avatar motion** page. It shares `playerIntensity` (default 0.8); the master motion switch, avatar master switch and **Reduce motion** still gate it. |
| Safety | Fades out below a small `walkAnimationSpeed` threshold; NaN phase/speed, zero speed or zero intensity return the exact identity pose. Swimming, riding, elytra, item use and sleep remain excluded by the existing gate. |

### Implementation notes

`AvatarMotionMath.stride(walkPos, walkSpeed, intensity)` is pure Java and uses the same `walkPos * 0.6662` phase as vanilla, so the added rotations reinforce the existing step rather than cancelling it. Amplitudes are bounded (legs 0.45, arms 0.28, torso lean 0.08 rad at full strength) and scale linearly with both walk speed and intensity. The controller priority is crafting > attack > stride; a sprinting attack applies the attack pose plus the stride legs only.

No new mixin, packet, renderer replacement or entity write was added. The existing `HumanoidModel` hook and armor-model coverage are unchanged.

### Files

- `motion/AvatarMotionMath.java` — added `stride`.
- `motion/PlayerAnimationController.java` — sprint lookup, priority and legs-only blending during attacks.
- `InventoryConfig`, `SpatialSettingsScreen`, both language files, `gradle.properties` (0.6.1), READMEs, architecture/testing docs and the regression suite.

### Verification scope

26.2 `physicsTest` passed **1,209 assertions** (44 new stride checks). 26.1.2, 26.2 and 26.3 `compileJava --offline` passed. No Minecraft client was launched, per the user's no-game-launch constraint; third-person feel, remote-player sync and stacking with third-party animation mods remain untested. Ordinary walking, attacks and crafting keep their existing coverage.
