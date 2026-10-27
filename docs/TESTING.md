# 验证记录

## 2026-10-27 · 0.6.1 疾跑跨步

- 新增 `AvatarMotionMath.stride` 的纯 Java 断言：零速度/零强度/低速度阈值与 NaN 均为精确空操作，全相位有限且有界，左右腿与左右臂交替、同侧手脚反相、躯干前倾且头部反向补偿、半周期翻转、强度与速度线性、速度上限夹取。26.2 `physicsTest` 通过，共 **1,209 条断言**。
- 26.1.2、26.2、26.3 的 `compileJava --offline` 均通过；疾跑标志通过同步的 `Avatar.isSprinting()` 读取，版本公共字段未新增适配文件。
- 按用户要求未启动 Minecraft 客户端，因此没有实机第三人称观感、远程玩家同步或与 Not Enough Animations 等第三方动作模组叠加的验证；普通走路、攻击和工作台动作的既有断言继续通过。
- 未修改物品栏界面动画、玻璃渲染、镜头、锚点、输入或网络逻辑；未发送自定义包。

## 2026-10-26 · 0.6.0 第三人称人物动作

- 新增纯 Java 断言覆盖挥动起止端点、五种武器族的有界角度、左右手镜像、强度线性缩放、行走时取消额外踏步、工作台循环的有限值/双手抬高/低头/零强度与非法时间，以及配置强度上限和 NaN 回退。26.2 `physicsTest` 通过，共 **1,165 条断言**。
- 26.1.2、26.2、26.3 的 `compileJava --offline` 均通过。26.3 使用 `src/versions/26.3` 的 `AvatarAttackView` 适配 `currentSwing`/`swingAnimation`，共享控制器与 26.1/26.2 保持一致。
- 按用户要求未启动 Minecraft 客户端，因此本轮没有实机截图、多人同步或动画观感验证；已完成的是编译、纯数学/配置回归和实际当前映射下的字段核对。
- 未修改物品栏界面动画、玻璃渲染、镜头、锚点、输入或网络逻辑；工作台动作仅对本地玩家推断，不发送自定义包。第三方自定义玩家/盔甲模型与光影组合未验证。

## 2026-10-18 · 0.5.0 流畅动效 MVP

- 兼容性审计为 Compatible，完整依据与文件清单见 `MOTION-AUDIT.md`。使用当前实际映射检查 26.1 `renderArmWithItem`、26.2 `submitArmWithItem`、26.3 `FirstPersonHandsAndItemsRenderer` 及其状态快照，未降级 Minecraft 或引入动画库。
- 完整命令：`build runClientGameTest collectRelease -Pmc=26.1.2 -I .tools/local-test-assets.init.gradle --offline --console=plain`，`BUILD SUCCESSFUL in 1m 28s`。1032 条数学/配置断言、25 类原有界面检查、新增独立动效客户端场景全部通过。日志 `.tools/motion-final-test.log`。
- 最终资源模板保持有效 JSON，测试辅助层补齐 26.2 HUD/聊天/提示访问后，以 `-PmotionOnly` 执行同一命令复核：`BUILD SUCCESSFUL in 40s`，1032 条断言及完整动效/设置场景通过。日志 `.tools/motion-release-test.log`。已检查最终 Jar 为 `transparent_inventory` / `0.5.0` / MC `26.1.2`，包含动效入口与四个新 Mixin，不包含测试类；legacy Jar 仅保留原入口。
- 已查看并留存真实截图 `motion-item-lowering.png`、`motion-item-raised.png`、`motion-sword-swing.png`、`motion-block-place-pulse.png` 及三页动效设置。新设置同样可使用已有液态玻璃主题。
- 新增场景：数字键/快速切换后实际库存与手持物立即正确；可见模型按时间线完成换物；原版鼠标挥剑且实际玩家角度保持不变；原生疾跑、起跳、普通/较高落地有视图响应；关闭视角摇晃或屏幕效果时偏移为零。真实攻击猪触发原版服务器伤害通知后才追加命中反馈，挥空或攻击不可伤害目标不触发。
- 破坏/放置通过原版游戏模式方法和真实服务器同步，额外粒子确实创建、单次数量受限，最少粒子设置停用新增碎片。地图、饮用、拉弓绕过自定义手部流程。F8 中减少动态、独立命中和破坏开关可操作，并与玻璃配置一同保存。第三人称、旁观者、卸载世界会停用/清理新状态。放置仅为四 tick 的粒子代理，没有模型缩放。
- 26.1、26.1.1、26.2、1.20.1、1.21.11 均通过 `build --offline`；26.2 的 `build compileGametestJava collectRelease` 通过，`BUILD SUCCESSFUL in 18s`。26.3 `assemble compileTestJava --offline` 通过，1032 条纯 Java 断言另以无 Minecraft/native 的 classpath 执行通过；未实机验证 26.3，离线缓存仍缺少其 LWJGL 3.4.3 native 依赖。
- 没有新增 shader 或 framebuffer；没有发送自定义网络包或修改攻击、移动、碰撞、伤害、冷却、放置规则。没有第三方整合包、远程服务器延迟或多硬件 FPS 基准结论。

## 2026-10-18 · 0.4.1 玻璃 HUD 快捷栏（26.1.2）

- `build runClientGameTest collectRelease -Pmc=26.1.2 --offline`：`BUILD SUCCESSFUL in 1m 10s`，1002 条纯 Java 断言及原有 25 类界面回归通过，同时执行新增 HUD 专项检查。
- 最终对惯用手设置补齐原版客户端设置广播，等待玩家实际主手同步后再拍两侧副手槽；`build runClientGameTest collectRelease -Pmc=26.1.2 -PcreativeOnly --offline` 再次通过，`BUILD SUCCESSFUL in 32s`。26.2 的 `build collectRelease --offline` 通过；26.3 仅验证编译及 assemble。
- 通过原版数字键选中第一/第九格、滚轮从第九格回到第一格，检查真实选中槽与物品数量；实际切换左右惯用手，检查副手两侧截图；F1 隐藏和恢复；背包打开时 HUD 与空间玻璃背包同时正常绘制。
- 通过 F8 实际翻页、关闭快捷栏玻璃并返回原版底板；重新启用，把独立透明度滑块拖到 100%，检查 `hotbarOpacity=0` 并确认现有 JSON 保存开关与透明度。设置覆盖层保留同一个菜单对象。
- 已目视检查 `docs/screenshots/hotbar-glass.png`、`hotbar-with-inventory.png`；物品图标和数量清晰，空槽显示世界背景，选中框与副手槽保留描边。原有液态玻璃菜单及全部容器测试继续通过。没有验证用户截图中的第三方整合包组合。

## 2026-10-18 · 0.4.0 液态玻璃菜单（26.1.2）

- `build runClientGameTest collectRelease -Pmc=26.1.2 --offline` 通过，1 分 9 秒；1002 条纯 Java 断言与 25 类实际客户端界面检查通过。最终仅降低玻璃填充不透明度后，再运行同一构建及 `-PcreativeOnly`：26 秒通过，覆盖创造背包与新增菜单检查。
- 通过原版 Esc 打开暂停菜单，F8 打开设置首页，实际点击液态玻璃开关。检查设置保持暂停语义、返回同一个暂停 Screen、保存 JSON 中的 `liquidGlassUi=true`，并拖动玻璃滑块检查数值改变。实际点击选项按钮进入 OptionsScreen，再返回设置关闭风格，确认开关关闭并可点击原版返回游戏按钮。
- 已目视检查最终暂停菜单截图，留存 `docs/screenshots/pause-liquid-glass.png`；按钮文字、默认布局及点击继续沿用原版。原 23 类背包/容器测试全部保留。
- 26.1、26.1.1、26.2 的 `build collectRelease --offline` 通过；未运行这些版本的实际客户端。26.3 为 RenderPearl 管线提供独立适配，Java 编译及 assemble 通过；完整 check 因离线缓存缺少 LWJGL 3.4.3 native 依赖未完成，未验证 26.3 实际客户端。
- 主题默认关闭，独立于玻璃背包；关闭后原版 sprite 调用直接执行。实现采用 GUI 半透明填充和高光，暂停背景复用原版模糊，没有实现真实光学折射。未对第三方自绘控件或多硬件性能作验证。

## 2026-10-17 · 0.3.3 容器放大与头部跟随（26.1.2）

- 最终 `build runClientGameTest collectRelease --offline` 通过，`BUILD SUCCESSFUL in 1m 6s`；纯 Java 回归 1002 条断言、实际客户端 23 类界面通过。发布包 `hoverglass-mc26.1.2-0.3.3.jar` 已核对版本、MC 目标和头部跟随 mixin，未包含测试模块。
- 生存、创造和原本第三人称三种背包状态：鼠标分别移动到左上及右下，读取实际玩家渲染状态验证 head yaw/pitch 发生明显变化，身体角度保持不变。原始客户端实体的 yaw/pitch/head/body 与服务端 yaw/pitch 全部保持原值；F8 开关可禁用跟随并立即恢复原版姿态。
- 箱子验证：默认槽位不再小于原生 GUI 比例；把镜头距离由 3 改为 6，槽位缩放保持不变；通过真实 F8 滑块调到 75%，实际面板随之缩小并能正确取放。6 行大箱子占用超过窗口 85% 高度，边界仍完整位于窗口内，变换后的悬停正常。
- 继续覆盖取放、Shift 转移、拖拽、配方书、文本输入、附魔、交易、创造分类/搜索/滚动条/垃圾槽、关闭过渡、镜头碰撞及世界卸载。已查看并留存 `large-chest-readable.png`、`head-follow-upper-left.png`、`head-follow-lower-right.png`。
- 本轮验证为原版/Fabric 26.1.2 客户端环境；未将第三方侧栏、光影包或服务器整合包兼容性作为已验证结论。

## 2026-10-17 · 0.3.2 创造物品栏修复（26.1.2）

- 复现根因：0.3.1 仅为 `InventoryScreen` 绘制玻璃；`CreativeModeInventoryScreen` 继承通用 BLOCK profile，未触发背包电影镜头。此前的 21 类界面检查未包含创造物品栏。
- 修复后完整 `build runClientGameTest --offline` 通过，`BUILD SUCCESSFUL in 57s`，22 类界面通过。纯 Java 回归为 965 条断言，新增创造配置迁移、原有透明度保留、恢复全局继承以及预设同时覆盖创造背包的检查。
- 新增创造模式实际客户端专项检查：第一人称按 E 打开、左滑及倾角、真实玩家投影位于右侧、上方标签/翻页区域留白、默认 84% 透明度、分类取物放入真实快捷栏、滚轮、滚动条拖动、原生键盘输入搜索 diamond、玩家背包标签、垃圾槽删除、F8 覆盖层保留菜单及关闭恢复视角。
- 生存背包打开期间将游戏模式改为创造，验证原版界面重定向仍保留首次第一人称入场意图。专项测试支持 JVM 属性 `-Dhoverglass.testScope=creative`，避免开发迭代时重复其他容器测试。
- 目视检查并保留实际客户端截图：`docs/screenshots/creative-glass.png`、`creative-search.png`、`creative-personal.png`。使用原版/Fabric 测试环境；没有声称验证用户整合包内的第三方侧栏或全部模组分类页。
- 原有容器用例结束后，测试夹具通过服务端完整 inventory snapshot 清空用于创造取放的目标快捷槽，避免客户端残留状态影响检查。该同步只在测试模块存在。
- 最终为透明玩家背包页补上垃圾槽图标后，重新执行 `build runClientGameTest collectRelease -PcreativeOnly`，965 条数学回归与创造模式专项再次通过，`BUILD SUCCESSFUL in 23s`。发布 jar 为 `hoverglass-mc26.1.2-0.3.2.jar`，确认元数据为 26.1.2 / 0.3.2，包含创造玻璃 mixin 且不包含测试类。

## 2026-10-17 · 0.3.1 玻璃背包（26.1.2）

- `build runClientGameTest collectRelease --offline` 通过，最终 `BUILD SUCCESSFUL in 53s`。本机通过 `.tools/local-test-assets.init.gradle` 复用现有资源，并补齐官方 26.1.2 字体、图形和中文语言文件；客户端测试关闭声音，未下载缺失的音频及其他语言。该脚本不进入发布包。
- 纯 Java 回归 960 条断言通过，新增归一化入场起终点、10% 回弹、漏帧下闪光时机、零时长不闪光、中途关闭连续性、倾斜动画坐标反算与预设覆盖范围。
- 实际 26.1.2 客户端的 21 类界面检查通过，包括背包取放、Shift 转移、拖拽数量守恒、配方书、窗口缩放、F8 覆盖层与透明度端点、JSON 保存、镜头碰撞、关闭视角恢复和卸载清理。
- 新增实际点击“应用参考图玻璃效果”按钮，确认背包不透明度为 0.16；生成玻璃开关对照、84% 透明度与设置截图，并目视检查。留存截图：`docs/screenshots/inventory-glass.png`。
- 增强侧移后发现后墙缩短后退距离时，侧移仍保持全长；现将侧移按实际后退距离同比缩短，随后完整回归通过。后墙和侧墙检查均保留。
- 本次打包仅针对用户使用的 26.1.2；其他 Minecraft 版本未在本轮重新打包或验证。参考图的夕阳、景深和光照取决于游戏场景与光影包；模组实现的是玻璃界面、入场反光与镜头构图。

日期：2026-10-15。目标 Minecraft 26.2，Fabric Loader 0.19.5，Fabric API 0.159.0+26.2，Loom 1.17.20。使用本机 JDK 26.0.2，编译目标 Java 25。

## 构建与回归

- `build --offline`：通过。纯 Java 回归 **7,674 条断言通过**，覆盖完整仿射坐标反算、旋转后的拖拽交叉项、不同窗口和配方书宽度的左侧布局、回弹与中途关闭连续性、透明度端点与继承、无效配置，以及保留的旧布局/物理数学。
- `runClient`：此前 0.1.0 实际启动，手动确认过生存背包、F8 设置覆盖层、独立透明度保存与第一人称恢复。本次 0.2.0 使用下面的隔离引擎测试验证。
- `runClientGameTest --offline`：独立实际客户端和集成服务器回归，**21 类界面**通过基础渲染与交互检查。使用临时测试世界、真实菜单同步及原版鼠标/键盘回调，不依赖操作用户的游戏窗口。

本机标准 wrapper 的 services.gradle.org 分发下载超时，因此验证时直接调用了本机已缓存的 Gradle 9.7.1：

```powershell
& 'C:\Program Files\Java\jdk-26.0.2\bin\java.exe' `
  -classpath 'C:\Users\StevenWu\.gradle\wrapper\dists\gradle-9.7.1-bin\brhrg0o7dy6fa4wpk3ipunw7x\gradle-9.7.1\lib\*' `
  org.gradle.launcher.GradleMain build runClientGameTest --offline --console=plain
```

正常联网环境使用 README 中的 wrapper 命令即可。没有改动 wrapper 以依赖本机绝对路径。

## 已实际检查的界面

每个界面都检查：通过原版流程打开、空间会话有效、锚点类别正确、变换有效、物品悬停命中原版槽位、截图可生成、关闭后恢复原视角。

| 界面 | 额外完成的交互验证 |
| --- | --- |
| 玩家背包 | 左滑入场、保留 4° 倾角、真实玩家投影位于右侧；斜面状态下左键取放、右键放单个、Shift 转移、拖拽分配且物品数量守恒、配方书开关、改变窗口大小后悬停、工具提示截图 |
| 箱子 | 服务器物品同步、左键取出 32 个钻石、右键放单个、Shift 转移且鼠标不持物 |
| 木桶 | 基础检查 |
| 工作台 | 基础检查 |
| 熔炉 | 基础检查 |
| 铁砧 | 物品放入、变换后的文本框聚焦、Ctrl+A、输入名称、输出名称与取出结果 |
| 附魔台 | 剑与青金石放入、点击可用附魔选项、得到附魔物品、青金石消耗、原版书模型和提示截图 |
| 高炉 | 基础检查 |
| 烟熏炉 | 基础检查 |
| 酿造台 | 基础检查 |
| 锻造台 | 基础检查 |
| 织布机 | 基础检查 |
| 切石机 | 基础检查 |
| 砂轮 | 基础检查 |
| 制图台 | 基础检查 |
| 信标 | 基础检查；没有验证激活增益 |
| 漏斗 | 基础检查 |
| 发射器 | 基础检查 |
| 投掷器 | 基础检查 |
| 村民交易 | 原版实体交互打开、选择交易、绿宝石输入/面包输出、取出并放入背包、滚轮列表 |
| 马匹背包 | 驯服马匹的实体交互打开、实体锚点、原版马匹模型与槽位提示 |

“基础检查”不代表熔炼、酿造、全部合成、全部升级材料、所有红石或所有交易类型均完成玩法测试。

## 设置、镜头和兼容规则

自动化额外覆盖：

- F8 在容器内打开覆盖层，Esc 只关闭设置，原 Screen/menu 对象保持不变。
- 当前界面透明度达到 100%；全局透明度达到 0%；独立值不被全局值覆盖；设置真实保存到 JSON。
- 配方书打开、窗口调整到 1280×720 后，坐标反算和悬停仍正确。
- 从第一人称打开背包时，早期面板中心位于停留位置左侧；动画完成保留 4° 倾角，面板中心位于窗口左侧 40% 内，真实玩家眼睛的屏幕投影位于右侧。
- 1,000 ms 面板动画和 500 ms 镜头过渡可正常完成并恢复第一人称；原本已处于第三人称时不启用背包斜面布局。
- 第三人称镜头使用 250 ms 过渡；背后放置实心墙会缩短距离，移除墙体恢复配置距离；侧方墙体会阻止对角侧移穿墙。
- 180 ms 关闭动画期间连续关闭请求保持幂等，原版鼠标回调不能取走物品。
- 未知容器继承通用 profile；注册父类 profile 能匹配子类；父类/精确类排除优先；非容器默认不处理，可显式加入。
- 在背包设置仍打开时直接卸载世界，确认原始视角恢复、覆盖层清除。该测试曾发现断开连接路径未覆盖的问题，修复后最终整轮回归通过。

Fabric API 0.159 的 `TestInput` 生成部分事件时不填充 modifier 位；因此 Ctrl+A 和 Shift 单击使用测试专用 Invoker 调用相同原版回调，显式传入修饰键位，并检查 Shift 转移后鼠标没有持物。这些 Invoker 仅存在于测试模块。

0.2.0 最终命令结果：`BUILD SUCCESSFUL in 47s`，14 个任务，其中 7 个执行、7 个已是最新；客户端输出 `[spatialGameTest] 21 screens passed rendering/input smoke checks`。

原版显示、附魔书、人物与马匹模型、工具提示此前已查看截图。本次查看了斜背包停留及配方书展开后的实际截图；保留在 `docs/screenshots`。完整截图在 `build/run/clientGameTest/screenshots`，该目录会保留旧一轮的编号文件。

## 验证边界

- 未验证第三方整合包、JEI/REI/EMI、控制器、VR 或其他修改相机/GUI 的模组。
- 当前平面面板使用世界锚点的投影；背包用仿射矩阵产生斜面效果，不提供世界几何深度遮挡或真实透视梯形。
- 新增动画设置的控件实现已编译；引擎回归主要验证动画行为，未逐项自动操作倾角、开关时间滑块。
- 性能尚未做多硬件定量基准，未声称具体 FPS 增益或损失。
- 测试日志中出现开发离线账号的 Realms/证书 401、Windows 性能计数器警告，以及 Fabric 测试默认选项的各向异性设置警告；它们没有阻止最终测试完成。最终检查不应包含本模组 Mixin 应用失败、空间投影禁用或测试断言失败。
