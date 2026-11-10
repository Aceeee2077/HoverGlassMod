# 验证记录

## 2026-11-10 · 0.45.0 滑条显示真实数值

- 新增纯 Java `SliderFormat.value(labelKey, normalized, percentage)`：把滑条归一化位置还原成用户实际配置的值。此前面板手感的四个速度/弹簧滑条与预览布局的五个滑条都落到 `value + 0.5` 的兜底格式，界面显示 0.50–1.50，与 tooltip 承诺的 px/s、像素完全不符。
- 修正后的单位：入场/退场速度为 `px/s`；回弹为百分比；停稳弹簧与阻尼为原始强度；预览高度、水平/垂直位置、面板位置为屏幕百分比；配方书偏移为 `px`。既有的容器缩放、镜头距离、时长、倾斜、偏角、头部、粒子与界面缩放格式保持原样，百分比滑条仍由 `percentage` 参数优先。
- 非有限输入回退到 0（最小值），越界值夹紧到 [0,1]；未知键仍使用旧兜底格式，`SettingSlider` 只调用一次辅助类。
- 新增 `testSliderFormat`：覆盖每类单位的两端与中点、百分比参数优先、越界夹紧、NaN/无穷回退、空键与未知键兜底。26.2 `physicsTest` 通过，共 **1,872 条断言**。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认新文本在窄滑条内的截断情况，也未确认拖动时每帧刷新是否带来可感知的额外开销。

## 2026-11-10 · 0.44.0 本页重置按钮

- 导航行插入 **↺** 按钮（宽 20），与上一页按钮并排；点击调用 `PageDefaults.reset(config, page)` 并在成功后重建页面，`PageDefaults.supports(page)` 为假时禁用。下一页按钮相应右移并缩窄，Done 按钮位置不变。
- 两个语言文件新增 `spatialui.settings.reset_page`（均为 ↺）与说明性 tooltip 键；`testLanguageKeys` 自动确认两边的键集合一致。26.2 `physicsTest` 通过，共 **1,830 条断言**（本轮未新增纯逻辑，计数不变）。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认三枚按钮在最小窗口宽度下的排布，也未确认禁用按钮的 tooltip 是否可悬停显示。

## 2026-11-10 · 0.43.0 镜头取景预设

- 新增 `CameraPresets`：`SUBTLE(2.4, .45, 2.0, 0, 0, 300)`、`BALANCED(3.0, .72, 4.0, 0, 0, 420)`、`CINEMATIC(3.8, .90, 7.0, 0, 0, 600)`，`apply()` 一次写入距离、侧移、倾斜、两个角度偏角与过渡时长，`matches()` 以 1e-4 容差比较，`current()` 返回匹配项或 `null`。
- 镜头角度页在两条滑条下方放置三个等宽预设按钮；当前取景匹配的按钮禁用，并在 `extractRenderState` 每帧刷新（拖动滑条后立即重新启用）。`PageDefaults` 第 10 页改为恢复全部六个值，`testPageDefaults` 相应扩展。
- 新增 `testCameraPresets`：新配置匹配均衡预设；三个预设写入后即匹配且通过 `sanitize()` 仍匹配；距离、侧移、倾斜、时长均按轻量→均衡→电影感递增；重复应用幂等；手调距离后不匹配任何预设。26.2 `physicsTest` 通过，共 **1,830 条断言**。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认三个预设的实际取景观感、预设按钮与滑条并存时的布局，以及 Delete 恢复六项后跨页滑条是否即时同步。

## 2026-11-10 · 0.42.0 快捷栏边框匹配物品栏

- `InventoryConfig.matchHotbarRim()` 把 `borderOpacity` 一次性复制到 `hotbarBorderOpacity`，不建立联动；快捷栏页新增同名按钮，点击后重建页面让滑条立即反映新值。
- `testHotbarRim` 补上：匹配后快捷栏等于面板边框、面板边框本身不变、面板边框再次变化后可重新匹配。26.2 `physicsTest` 通过，共 **1,810 条断言**。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认按钮在快捷栏页上的实际点击反馈与重建后的滑条位置。

## 2026-11-10 · 0.41.0 恢复按钮显示快照数量

- `ConfigBackups.snapshotCount(Path)` 统计轮换目录中带前缀的普通文件，目录缺失或不可读时返回 0；`ConfigLoader.snapshotCount()` 封装真实配置路径。兼容性页的恢复按钮标签追加 `(N)`，`N == 0` 时按钮禁用并把 tooltip 换成「还没有快照」。
- `testConfigBackups` 补上：全新安装计数为 0；裁剪后计数等于 `KEEP`；删除轮换目录后再次为 0。26.2 `physicsTest` 通过，共 **1,807 条断言**。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认禁用按钮是否仍显示 tooltip，也未确认标签加计数后在窄窗口下是否截断。

## 2026-11-10 · 0.40.0 移除已失效的配置键

- 删除四个已无引用的字段及其清洗行：`lookDistance`、`maxBodyYaw`、`bodyYawRatio`、`playerSlideSmoothing`。全仓库检索确认删除前没有任何渲染、布局或动画代码读取它们；`playerSlideRatio`、`playerPreview` 仍被 1.21.x 分支使用，因此保留。
- `testConfigTransfer` 新增兼容性检查：包含 `maxBodyYaw`、`lookDistance`、`playerSlideSmoothing` 等已移除键的 JSON 仍能解析，且同一对象中的 `panelOpacity` 正常读取（Gson 忽略未知字段）。26.2 `physicsTest` 通过，共 **1,804 条断言**。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端；未在真实旧配置上确认下一次保存后这四个键确实消失，但 Gson 序列化只写当前字段，行为由实现直接保证。

## 2026-11-10 · 0.39.0 页面记忆跨重启保留

- `InventoryConfig` 新增 `settingsPage`（默认 0），`sanitize()` 夹紧到 0–14；`SettingsPages.remember()` 现在同时写入会话静态值与 `InventoryConfig.get().settingsPage`，`remembered()` 在本次会话尚未翻页时回退到配置值，因此重启后能恢复上次页面。
- 本轮尚未翻页时读取配置值；一旦翻页，会话值优先，避免旧配置把当前浏览位置拉回去。越界的配置值仍由 `clamp()` 回落到第一页。
- `testSettingsPages` 补上：翻页会同步写入配置、全新会话恢复配置中的第 9 页、越界配置回落第一页。26.2 `physicsTest` 通过，共 **1,802 条断言**。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过；`InventoryConfig` 属共享类，1.21.x 分支只是多存一个无副作用的字段。按用户要求未启动 Minecraft 客户端，未实机确认退出设置时的保存顺序，也未确认会话内「打开设置但不翻页就关闭」是否会保留配置中的旧页面。

## 2026-11-10 · 0.38.0 清空已添加的界面

- `InventoryConfig` 新增 `clearScreenOptIns()`，清空 `additionalScreens` 的全部条目（含通配规则），与兼容性页的清空按钮保持同样的直接语义；内置容器处理与 `defaultSpatialHandledScreens` 不受影响。
- 界面支持页在设置快捷键下方新增 **清空已添加的界面** 按钮，点击后重建页面；tooltip 说明会一并清除通配规则。
- `testScreenOptIn` 补上：同时存在精确条目与通配规则时清空后列表为空、两类规则都不再命中、清空后仍能重新添加界面。26.2 `physicsTest` 通过，共 **1,799 条断言**。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认清空后正在打开的模组界面是否会立即回退，也未确认按钮在窄窗口下的布局。

## 2026-11-10 · 0.37.0 恢复最新快照

- `ConfigBackups.newest(Path)` 按零填充文件名的字典序取最新快照；目录缺失、不可读或没有匹配前缀时返回 `null`，无关文件被忽略。`ConfigLoader.loadNewestSnapshot()` 复用与 `.bak` 恢复相同的解析与 `sanitize()` 路径。
- 兼容性页把复制/粘贴改为同一行左右两半，空出的一行新增 **恢复最新快照**；没有快照时提示 `spatialui.transfer.snapshot.none`，成功后立即保存并重建页面。
- `testConfigBackups` 补上：快照存在时 `newest()` 指向最后写入的一份；目录中的 `notes.txt` 不会被当快照；不存在的轮换目录返回 `null`；清理目录后再次返回 `null`。26.2 `physicsTest` 通过，共 **1,795 条断言**。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认按钮与轮换快照的实际读写顺序，也未确认恢复后再次点击会像 `.bak` 恢复一样来回切换。

## 2026-11-10 · 0.36.0 面板手感设置页

- `SettingsPages` 扩到 15 页，新增 `PANEL_FEEL = 14` 与标题键；`PageDefaults` 支持第 15 页，重置 `slideSpeed`、`bounce`、`closeSpeed`、`spring`、`damping` 五项。
- 新页面五个滑条覆盖清洗范围：入场速度 600–6000、回弹 0–0.6、退场速度 600–6000、弹簧强度 40–900、阻尼 5–90；每项带 tooltip 解释手感。
- `testSettingsPages` 更新：15 页、第 15 页索引与标题、预览布局之后是面板手感、末尾回到第一页；`testPageDefaults` 新增第 15 页五项复位断言。26.2 `physicsTest` 通过，共 **1,791 条断言**。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认极低阻尼或极高弹簧强度下的面板观感与帧率稳定度。

## 2026-11-10 · 0.35.0 预览布局设置页

- `SettingsPages` 扩到 14 页，新增 `PREVIEW_LAYOUT = 13` 与标题键；`PageDefaults` 支持第 14 页，重置 `playerHeightRatio`、`playerCenterXRatio`、`playerCenterYRatio`、`panelCenterRatio`、`recipeBookOffset` 五项。
- 新页面五个滑条分别映射到清洗后的完整范围：人物高度 0.25–0.70、人物水平 0.50–0.95、人物垂直 0.30–0.80、面板水平 0.20–0.65、配方书偏移 0–160；每项带说明性 tooltip。
- `testSettingsPages` 更新：14 页、第 14 页索引与标题、界面支持之后是预览布局、首尾循环回到预览布局；`testPageDefaults` 新增第 14 页五项复位断言。26.2 `physicsTest` 通过，共 **1,775 条断言**。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认极值下滑条摆放人物与面板的实际观感，也未确认配方书打开时偏移与面板动画的配合。

## 2026-11-10 · 0.34.0 滚轮翻页

- `SettingsPages` 新增 `shift(page, steps)`（用 `Math.floorMod` 双向循环）与 `scrollStep(scrollY)`（向上为 -1、向下为 +1，`|scrollY| < 0.5` 或非有限值返回 0）。`SpatialSettingsScreen.mouseScrolled` 在步长为 0 时交回父类，否则 `goTo(shift(page, step))` 并吞掉本次滚动。
- `testSettingsPages` 补上：向前/向后单步、首尾越界、正反整圈回到原页；滚轮方向、零值、±0.49 亚格量、0.5 边界、NaN 与正负无穷。26.2 `physicsTest` 通过，共 **1,759 条断言**。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认滚轮在滑条上滚动时不会被吞掉后被误认为滑条失灵，也未确认快速滚轮是否会连续翻过多页。

## 2026-11-10 · 0.33.0 遮罩随面板淡入淡出

- `Opacity` 新增纯函数 `fade(alpha, visibility)`：两者都夹紧到 0–1，任一为非有限值时返回 0（宁可隐藏也不闪出全黑），成品交给原有 `multiply()` 转成 ARGB。`ScreenMixin.spatial$dim` 改为用 `SpatialScreenManager.INSTANCE.visibility()` 调制 `worldDimOpacity`。
- 新增 `testOpacityFade`：稳定态保持原强度、半进度为一半、进度为 0 时遮罩消失、超范围与负值被夹紧、NaN/无穷进度或强度返回 0、20 步采样全程单调不减；并确认 `multiply()` 在 0.5 进度得到 `0x80000000`、进度 0 得到全透明、进度 1 得到不透明黑，非有限不透明度仍保留原 alpha。26.2 `physicsTest` 通过，共 **1,747 条断言**。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认遮罩与面板滑入曲线在视觉上是否同步，也未确认快速开关容器时淡出是否被「快速重开」路径打断得自然。

## 2026-11-10 · 0.32.0 记住上次设置页

- `SettingsPages` 新增会话内页面记忆 `remembered()` / `remember(int)` 与 `last()`；非法值统一夹紧到第一页。`testSettingsPages` 补上：End 指向最后一页、首次打开为第一页、记住第 8 页、非法值回落到第一页，并在结束前把记忆复位为 0。
- 设置界面的所有翻页入口（上一页/下一页按钮、PgUp/PgDn、Home 新增 End）统一走 `goTo()`：写入记忆后重建控件；`init()` 从记忆恢复页码。26.2 `physicsTest` 通过，共 **1,714 条断言**；26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。
- 按用户要求未启动 Minecraft 客户端，未实机确认「上次页面」与实际进出容器的配合，以及 End 键是否会被其他模组拦截。

## 2026-11-10 · 0.31.0 配置快照轮换

- `ConfigBackups` 新增 `rotate()`：保存时在同级 `transparent_inventory-backups/` 写入 19 位零填充时间戳快照，并按字典序裁剪到最新 `KEEP = 5` 份；任何文件操作失败都返回 -1，不影响 `.bak` 与本次保存。
- `testConfigBackups` 扩展：确认快照目录名；缺失配置返回 -1；正常保存会留下至少一份快照；用固定时间戳连写 7 份后恰好剩 5 份、最新一份内容为最后写入值、最旧的时间戳已被裁剪；临时目录清理会先删除快照再删目录。26.2 `physicsTest` 通过，共 **1,710 条断言**。
- `ConfigBackups` 位于 `legacyKeep`，因此 26.x 与 1.21.x 分支都会获得快照；26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未在真实游戏目录确认快照写入与权限失败时的回退表现。

## 2026-11-10 · 0.30.0 待机摇摆

- 新增 `testIdleSway`：静止角色会产生姿态且所有旋转不超过 0.05 弧度；双臂外张方向相反、双腿保持为零；不同时间点姿态不同；行走（`walkAnimationSpeed = .2`）、零强度、负/NaN 时间、NaN 移动速度都不产生姿态；强度线性缩放。`testPageDefaults` 补上第 9 页重置恢复 `idleSway` 的断言。26.2 `physicsTest` 通过，共 **1,702 条断言**。
- 人物动作页把空中姿态与待机摇摆并排放置；空中开关标签改为 **Airborne poses / 空中姿态**，原「跳跃、下落与攀爬姿态」的说明保留在 README 对应版本中。26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。
- 按用户要求未启动 Minecraft 客户端，未实机确认待机摇摆与潜行、举盾、拉弓等原版姿态叠加时的观感，也未确认 0.03 弧度在实际角色尺寸下是否足够可见。

## 2026-11-10 · 0.29.0 可改的设置快捷键

- 新增 `testSettingsKey`：默认 F8；循环顺序 F8→F9→F10→F6→F7→F8；普通游戏键（`key.keyboard.q`）、空值与列表外名称在 `sanitize()` 后回退 F8；带空格与大小写的名称会被规范化；从非法值开始循环会先回落到 F8 再前进。`testPageDefaults` 补上第 13 页重置恢复 F8 的断言。26.2 `physicsTest` 通过，共 **1,685 条断言**。
- `KeyboardHandlerMixin` 改为读取 `settingsKey`，并用「名称变化才重新解析」的静态缓存避免每次按键都查表；解析失败回退 F8。界面支持页新增 **设置快捷键** 按钮，底部提示去掉写死的 F8。26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。
- 按用户要求未启动 Minecraft 客户端，未实机确认改键后按键事件是否被正确吞掉，也未确认与其他模组同时监听功能键时的顺序。

## 2026-11-10 · 0.28.0 界面支持设置页

- 新增第十三页 **界面支持设置**（索引 12），提供 `defaultSpatialHandledScreens`、`fallbackToScreenSpace` 两个开关，以及把打开设置时所处界面加入/移出 `additionalScreens` 的按钮；精确条目可增删，通配规则只提示不改写。`SettingsPages` 更新为 13 页，`testSettingsPages` 覆盖新索引与首尾循环。
- 新增 `testScreenOptIn`：默认不加入任何界面；加入后 `hasExactOptIn`/`isScreenOptedIn` 为真，再次点击移除；空白名被拒绝；通配规则可命中但不会被按钮删除；名称会去掉首尾空格，且经 `sanitize()` 后仍保留。`testPageDefaults` 补上第 13 页的开关恢复断言。26.2 `physicsTest` 通过，共 **1,671 条断言**。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过；`testLanguageKeys` 自动校验新增 11 个键的中英一致性。按用户要求未启动 Minecraft 客户端，未实机确认从模组菜单按 F8 后按钮状态，以及关闭「已识别的容器使用玻璃」后的实际行为。

## 2026-11-10 · 0.27.0 关闭动画开关

- 入场页第一行拆成两个半宽开关：**第一人称侧移入场** 与新增的 **关闭动画**（`closeAnimation`）。关闭后 `SpatialScreenManager.requestClose` 直接返回 false，由原版立即关闭界面，不再等待滑出动画。
- `testPageDefaults` 补上入场页的 `closeAnimation` 恢复断言与出厂默认开启断言；`testLanguageKeys` 自动校验 3 个新键的中英一致性。26.2 `physicsTest` 通过，共 **1,643 条断言**；26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。
- 按用户要求未启动 Minecraft 客户端，未实机确认关闭动画关闭后镜头回位与快速重开窗口的配合表现。

## 2026-11-10 · 0.26.0 单页重置

- 新增 `testPageDefaults`：逐页把该页控件改成非默认值后调用重置，断言每个字段都回到 `new InventoryConfig()` 的取值；兼容性页返回 false 且不改动配置；越界页与 null 配置也被拒绝；在全新配置上依次重置所有支持页后仍与出厂值一致（防止重置写入与出厂值不同的值）；材质页重置后落在「磨砂」预设。26.2 `physicsTest` 通过，共 **1,641 条断言**。
- F8 设置页新增 **Delete** 快捷键，调用 `PageDefaults` 并重建控件；底部提示改为「PgUp/PgDn · Del 重置本页 · F8/Esc 保存」。26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。
- 按用户要求未启动 Minecraft 客户端，未实机确认 Delete 键在容器界面上层（overlay 模式）时的事件传递，也未确认底部提示在窄窗口下是否换行。

## 2026-11-10 · 0.25.0 落地姿态

- 新增 `testLandingPose`：真实下落会产生姿态且全部旋转有界；双膝对称前收、双臂外展、躯干前折、头部反向保持水平；第 6 tick 起完全结束；中段幅度小于起始、强度线性缩放；轻落步、零强度、负/NaN 时间和 NaN 冲击都不产生姿态；重落比轻落吸收更深且不超过 0.4 rad。26.2 `physicsTest` 通过，共 **1,575 条断言**。
- 控制器新增按实体记录的落地历史：腾空时采样下降速度，落地后回放 6 tick；映射上限 256 条、切换世界时清空，避免多人服务器无限增长。姿态由已有的 **落地反馈**（`landingMotion`）开关控制，中文标签由「落地视角反馈」改为「落地反馈」以覆盖镜头与姿态。26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。
- 按用户要求未启动 Minecraft 客户端，未实机确认落地姿态与走路循环叠加时的观感，也未确认远端角色在延迟下的落地时序。

## 2026-11-10 · 0.24.0 材质预设

- 新增 `testGlassMaterialPresets`：默认配置与「磨砂」预设完全一致；三个预设套用后 `matches()` / `current()` 都能识别；五个通道在每套预设中都落在 [0, 1] 且「选中」始终强于「悬停」；清透 → 磨砂 → 鲜明的每个通道都严格递增；手工微调后不匹配任何预设；重复套用保持幂等。26.2 `physicsTest` 通过，共 **1,552 条断言**。
- F8 材质页底部新增清透 / 磨砂 / 鲜明三个按钮，当前生效的预设按钮置灰。`testLanguageKeys` 自动校验 6 个新键的中英一致性。26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。
- 预设按钮的可用状态在 `extractRenderState` 中逐帧刷新，拖动滑条离开预设后按钮立即恢复可点；此行为属于画面逻辑，按用户要求未启动 Minecraft 客户端实机确认。

## 2026-11-10 · 0.23.0 玻璃材质设置页

- `testSettingsPages` 更新为 12 页，新增 `GLASS_MATERIAL = 11` 与首尾循环断言；`testLanguageKeys` 自动检查新页与 5 个材质键的中英一致性。
- 新增 `testGlassMaterials`：五个默认值符合参考玻璃；选中高亮默认强于悬停；越界值夹到 0/1，NaN 槽位描边回落到 0。26.2 `physicsTest` 通过，共 **1,504 条断言**。
- F8 新页提供 5 个百分比滑条，直接读写 `slotOpacity`、`borderOpacity`、`slotBorderOpacity`、`hoverOpacity`、`selectedOpacity`。26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。
- 按用户要求未启动 Minecraft 客户端，未实机确认 5 个材质滑条对面板观感的组合效果。

## 2026-11-10 · 0.22.0 键盘翻页

- 设置界面覆写 `keyPressed(KeyEvent)`：Page Up / Page Down 复用 `SettingsPages.previous` / `next` 并重建控件，Home 回到第一页，其余按键交给原版处理；断言数不变（沿用既有翻页断言）。26.2 `physicsTest` 通过，共 **1,484 条断言**。
- 底部提示更新为「PgUp/PgDn · 实时预览 · F8/Esc 保存」，英中同步；提示仍与页码前缀共用一行。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认按键事件在设置覆盖层中的分发顺序。

## 2026-11-10 · 0.21.0 镜头角度

- `testSettingsPages` 更新为 11 页：兼容性仍在索引 9，新页 `CAMERA_ANGLES = 10`；越界与循环断言随页数更新。`testLanguageKeys` 自动覆盖新页标题。
- 新增 `testCameraOffsets`：默认偏角为 0；越界值夹到 ±60 / ±45；NaN 偏角回落到下限。26.2 `physicsTest` 通过，共 **1,484 条断言**。
- F8 新页提供左右与上下偏角滑条，映射范围分别为 ±60° 与 ±45°；英中语言键同步新增 `spatialui.settings.camera_angles` 与两个偏角键。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认偏角方向是否符合直觉。

## 2026-11-10 · 0.20.0 页面标题

- 新增 `testLanguageKeys` 断言：从测试类路径读取 `en_us` 与 `zh_cn`，两者键集合完全相同；10 个设置页标题键均有翻译；页码与剪贴板文案键存在。26.2 `physicsTest` 通过，共 **1,469 条断言**。
- 设置页标题行在透明度页继续显示当前容器的透明度来源，其他页改为显示 `SettingsPages.titleKey(page)`，因此从 HUD 打开设置时也能确认所在页面。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机查看标题行文案长度。

## 2026-11-10 · 0.19.0 攀爬姿态

- 新增 `AvatarMotionMath.climb` 断言：正弦峰值时双臂保持抬起且右臂与左腿同向伸出（对角步态）；下降时方向镜像；双臂基础角在上升/下降之间保持一致；强度 0.5 时角度为满强度一半；静止、微小移动、0 强度与 NaN 时钟均返回单位姿态。26.2 `physicsTest` 通过，共 **1,455 条断言**。
- `PlayerAnimationController` 通过 `Avatar.onClimbable()` 识别梯子/藤蔓，读取实体垂直速度；优先级为工作台 > 攻击 > 攀爬 > 空中 > 跨步。复用既有 `motion.airborneMotion` 开关，语言文案改为「跳跃、下落与攀爬姿态」。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认攀爬时的手臂方向与观感。

## 2026-11-10 · 0.18.0 快速重开窗口

- `testQuickReopen` 新增断言：默认 400 ms 对应的滑条比例为 400/1500；比例 1 → 1500 ms、0 → 0 ms；越界比例 2 与 -1 分别夹到 1500 与 0；比例 0.5 → 750 ms 且读取回来仍为 0.5。26.2 `physicsTest` 通过，共 **1,444 条断言**。
- `InventoryConfig.reopenWindowFraction()` / `setReopenWindowFraction()` 与 F8 入场页滑条共用同一映射；滑条标签按 1500 ms 显示，并带说明提示（0 关闭捷径）。英中语言键同步新增 `spatialui.inventory.reopen` 与 `.hint`。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机拖动滑条确认提示换行。

## 2026-11-10 · 0.17.0 快捷栏描边

- 新增 `testHotbarRim` 断言：`hotbarBorderOpacity` 默认 0.72；高于 1 与低于 0 均被夹紧；单独修改快捷栏描边不影响 `borderOpacity`，反之亦然。26.2 `physicsTest` 通过，共 **1,438 条断言**。
- `GlassHotbarRenderer` 的底板边框与顶部高光改用 `hotbarBorderOpacity`；槽位描边继续使用全局 `slotBorderOpacity`，保持既有观感。F8 快捷栏页新增滑条，英中语言键同步新增 `spatialui.hotbar.border`。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机对比两处描边的相对亮度。

## 2026-11-10 · 0.16.0 头部跟随速度

- `testLookTracker` 新增断言：同一目标下 40 的平滑速度在 10 帧后比 1 更接近目标；两者都不会越过 40°；新建跟踪器从 0 开始。26.2 `physicsTest` 通过，共 **1,433 条断言**。
- 头部跟随页新增滑条，映射 `lookSmoothing` 1–40（0–100%）；百分比格式沿用滑条标签的 `smoothing` 分支。英中语言键同步新增 `spatialui.look.smoothing`。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机调整滑条观察跟随手感。

## 2026-11-10 · 0.15.0 配置备份

- 新增 `ConfigBackups` 断言（使用系统临时目录，结束后清理）：备份路径为同目录 `.bak`；源文件不存在时不复制也不报告备份；首次复制成功后 `.bak` 内容等于上一版本；再次保存刷新为最新上一版本；删除主文件后 `.bak` 仍然可读。26.2 `physicsTest` 通过，共 **1,430 条断言**。
- `ConfigLoader.save()` 在写入前调用 `ConfigBackups.copy()`，失败只影响备份、不影响保存；新增 `loadBackup()` 解析并清理备份，读取失败返回 null。`ConfigBackups` 已加入 legacy 共享白名单。
- 兼容性页新增「恢复上一次的设置」，读取失败与成功均有聊天提示；英中语言键同步新增 3 个。26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。
- 按用户要求未启动 Minecraft 客户端，未实机验证恢复按钮的两次互换行为。

## 2026-11-10 · 0.14.0 跳跃与下落姿态

- 新增 `AvatarMotionMath.airborne` 纯 Java 断言：上升姿态双腿等值前收、双臂 roll 镜像、躯干后仰；下落姿态双腿等值后摆、双臂 roll 镜像且展开幅度大于腿部；0 速度、低于 0.08 m/tick 的小台阶、NaN 速度与 0 强度均返回单位姿态；强度 0.5 时角度为满强度的一半；5 m/tick 被夹到 0.6 m/tick 的同一姿态（改用 `equals` 比较记录值）。26.2 `physicsTest` 通过，共 **1,421 条断言**。
- `PlayerAnimationController` 通过 `ClientLevel.getEntity(state.id)` 判断离地、水中、岩浆与攀爬状态，优先级为工作台 > 攻击 > 空中 > 跨步；`motion.airborneMotion` 默认 true。
- F8 人物动作页在强度滑块下方新增「跳跃与下落姿态」开关；英中语言键同步新增。26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。
- 按用户要求未启动 Minecraft 客户端，未实机确认手臂 roll 方向与动作观感。

## 2026-11-10 · 0.13.0 设置剪贴板

- 新增 `ConfigTransfer` 纯 Java 断言：导出为格式化 JSON 对象；导出→解析往返保留透明度、容器大小、时长、镜头侧移、动效开关与粒子倍率、精确与通配界面透明度、精确与通配排除规则；`null` / 空白 / 任意文本 / `null` 字面量 / 数组均被拒绝；`{}` 解析为默认配置；越界透明度被夹到 1、负窗口归零、排除规则去空白与去重；超过 `MAX_LENGTH` 的文本被拒绝。26.2 `physicsTest` 通过，共 **1,405 条断言**。
- 兼容性页新增「复制设置到剪贴板 / 从剪贴板导入设置」，使用 `minecraft.keyboardHandler` 读写剪贴板（`org.lwjgl.glfw` 在 26.3 编译类路径中不可用，因此改用游戏自带接口）；导入成功立即 `ConfigLoader.save()` 并重建控件，结果通过聊天消息提示。
- 英中语言键同步新增 5 个 `spatialui.transfer.*`。26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。
- 按用户要求未启动 Minecraft 客户端，未实机验证剪贴板写入与聊天提示。

## 2026-11-10 · 0.12.0 翻页导航

- 新增 `SettingsPages` 纯 Java 断言：共 10 页且兼容性页为最后一页；0–9 的索引原样保留；`next` / `previous` 均正确前进后退并在两端循环；每页标题键以 `spatialui.settings.` 开头；下一页/上一页按钮文案分别等于目标页标题；越界索引回落到第一页；非法索引取标题时同样回落到第一页。26.2 `physicsTest` 通过，共 **1,382 条断言**。
- F8 底部改成「上一页 / 下一页 / 完成」三个按钮，上一页按钮宽 20 像素、下一页相应缩短，两者均带目标页提示；底部提示改为 `第 n / 10 页 · 实时预览…`。
- 英中语言键同步新增 `spatialui.settings.page`、`.previous`、`.next`。26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。
- 按用户要求未启动 Minecraft 客户端，未实机确认窄窗口下的按钮宽度与提示换行。

## 2026-11-10 · 0.11.0 兼容性设置页

- 新增 `testCompatibilityActions` 断言：界面默认不排除；按钮写入去空格后的精确条目并可再次移除；空名与 `null` 不改变状态；通配规则排除的界面显示为“规则排除”而非精确条目，点击不会写入冗余条目也不会覆盖规则；清除操作同时移除精确与通配排除项；清空独立透明度后回落到全局值。26.2 `physicsTest` 通过，共 **1,314 条断言**。
- F8 第十页按状态显示「排除当前容器 / 重新启用当前容器 / 已被界面规则排除」，规则排除时按钮禁用；两个清除按钮重建控件以立即刷新状态。英中语言键同步新增。
- 26.2 `build` 通过；26.3 与 1.21.11 `compileJava` 通过。
- 按用户要求未启动 Minecraft 客户端，未实机点击按钮确认排除后的原版界面。

## 2026-11-10 · 0.10.1 界面规则状态

- 新增 `ScreenRules.Source` 断言：精确条目报告 `EXACT`，仅通配命中报告 `RULE`，都未命中报告 `GLOBAL`；配置层 `opacitySource` 与 `opacityFor` 结果一致。26.2 `physicsTest` 通过，共 **1,300 条断言**。
- F8 透明度页标题按来源显示独立 / 通配规则 / 全局三种文案；「当前界面跟随全局」仅在存在精确条目时可用，点击清除后立即变灰，滑块写入独立值时立即恢复可用。
- 英中语言键同步新增 `spatialui.settings.rule`。26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。
- 按用户要求未启动 Minecraft 客户端，未实机查看三种标题与按钮灰显效果。

## 2026-11-10 · 0.10.0 快速重新打开

- 新增 `EntranceTiming` 纯 Java 断言：首次打开不瞬切；关闭后 0 ms 与窗口最后一纳秒内瞬切；恰好到达窗口边界恢复动画；窗口为 0 停用；时钟回退不瞬切；`expired` 与窗口一致；`reset` 清除记录；默认窗口 400 ms；配置上限 1500、负值归零；减少动态仍让每次打开瞬切。26.2 `physicsTest` 通过，共 **1,297 条断言**。
- `SpatialScreenManager` 记录面板关闭时刻，新会话在窗口内把入场时长置 0；创世背包交接沿用原动画并保留本次会话的瞬切状态，避免时长切换造成跳变。`SpatialCameraController` 在同一会话内同样瞬切。
- legacy `LegacyScreenManager` 记录 `reset()` 时刻并读取同一配置；`EntranceTiming` 位于共享 `anim` 包。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。按用户要求未启动 Minecraft 客户端，未实机确认连续开关 E 的观感。

## 2026-11-10 · 0.9.0 容器镜头开关

- 新增 `CameraPolicy` 纯 Java 断言：默认同时允许物品栏与容器；关闭 `containerCameraEnabled` 只停用容器、物品栏入口镜头保留；`cameraEnabled` 总开关优先于两者；重新开启恢复容器镜头；null 配置关闭镜头。26.2 `physicsTest` 通过，共 **1,284 条断言**。
- `SpatialCameraController` 通过 `SpatialScreenManager.playerAnchored()` 区分玩家背包与世界容器；关闭时不再切换第三人称、不叠加距离/角度/侧移，玻璃面板与世界锚点渲染路径不变。
- F8 镜头页新增全宽「容器镜头」按钮与悬停说明；英中语言键同步新增。26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。
- 按用户要求未启动 Minecraft 客户端，因此没有实机确认容器在第一人称下的面板透视与构图；相关坐标仍沿用既有投影路径。

## 2026-11-10 · 0.8.1 损坏配置恢复

- 新增 `ConfigFileRecovery` 纯 Java 断言：备份名确定且 Windows 安全（无 `:`），UTC 时间戳精确到秒；不同时间生成不同名称；路径前缀被剥离；null 文件名回退为 `config`；`a:b?.json` 等保留字符被移除；超长文件名截断到 128 字符。26.2 `physicsTest` 通过，共 **1,276 条断言**。
- 解析失败时 `ConfigLoader` 先改名再使用默认值；改名抛错时只记录警告、原文件保持不动，本次会话使用内存默认值。保存仍走临时文件 + 原子替换。
- 26.2 `build` 通过；26.3 与 1.21.11 的 `compileJava` 通过（`ConfigFileRecovery` 已加入 legacy 共享白名单）。
- 按用户要求未启动 Minecraft 客户端；文件系统失败路径仅通过代码审查确认，未在真实实例中制造损坏文件验证。

## 2026-11-10 · 0.7.0 界面匹配规则

- 新增 `ScreenRules` 纯 Java 断言：`*` 可匹配空/多字符、`?` 恰好一个字符、包名前缀与后缀通配、区分大小写、空值与 null 不匹配、病态 `*a*a...` 规则线性终止；精确条目优先于所有通配，字面字符更多者优先，同等具体度保留先写入项；未命中回退全局值；非法/空白/超长规则在 `sanitize()` 中丢弃，列表去重去空格，通配透明度值同样夹取到 0–1。26.2 `physicsTest` 通过，共 **1,245 条断言**。
- 26.2 `build` 通过；26.3 `compileJava` 通过。legacy（1.21.11）`compileJava` 通过：`ScreenRules` 已加入共享类白名单，旧分支的 `InventoryConfig` 继续编译。
- 按用户要求未启动 Minecraft 客户端；本轮只改变配置解析与界面分类判定，未改动渲染、输入、镜头或网络逻辑。未验证第三方界面类名实际经过通配规则时的观感。

## 2026-11-10 · 0.8.0 减少动态覆盖界面

- 新增纯 Java 断言：开启减少动态后开关时长与镜头过渡时长均为 0，关闭或缺失 `motion` 时恢复 480/420 ms；零时长入场立即位于停留位姿、完全可见且不带落地闪光；零时长关闭立即完成且透明度为 0；零时长镜头过渡在同一帧到达目标，重新打开同样瞬切；非零时长仍按 smoothstep 在 210 ms 到达 50%，关闭从 1 开始；减少动态的入场变换与普通入场结束时的中心、高度、缩放与倾角完全一致，普通入场中段仍位于停留中心左侧。26.2 `physicsTest` 通过，共 **1,270 条断言**。
- 26.2 `build`、26.3 与 1.21.11 `compileJava` 通过。`SpatialScreenManager`、`SpatialCameraController` 与 legacy `LegacyScreenManager` 统一读取生效时长；F8 中保存的原始时长不被改写。
- `CameraTransition` 修复零时长首帧返回旧值的延迟：现在设置 0 ms 时打开/关闭都在当帧完成。
- 按用户要求未启动 Minecraft 客户端，因此没有实机观感验证；减少动态仍保留第三人称构图本身，只是取消过渡与面板动画。

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
