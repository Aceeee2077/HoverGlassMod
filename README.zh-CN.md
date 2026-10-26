# Hoverglass

<p align="center">
  <img src="docs/screenshots/creative-glass.png" width="49%" alt="Hoverglass 0.3.2 —— 创造模式玻璃物品栏与右侧人物">
  <img src="docs/screenshots/inventory-recipe-book.png" width="49%" alt="Hoverglass —— 配方书与合成界面呈现在玻璃面板上">
</p>

[English](README.md) | **简体中文**

> 把原版容器界面变成可透视的玻璃面板，并让它在镜头下拉时从侧面滑入。

**26.x · 0.6.0 新增人物动作**：所有可见玩家的剑、斧、长矛与工具挥动都会带动全身，工作台打开时本地玩家会做出双手伏案的合成动作。姿态叠加在原版动画之后，行走、潜行、骑乘、使用物品和原版手臂挥动均保持原样；独立的盔甲模型也会同步姿态，避免盔甲与身体脱节。工作台动作只作用于本地玩家，因为纯客户端模组无法读取其他玩家打开的菜单。按 **F8** 翻页至 **人物动作设置**，可分别开关总功能、攻击动作和合成动作，并调整默认 80% 的动作强度。详见 [动效集成审计](docs/MOTION-AUDIT.md)。

**26.x · 0.5.0 新增流畅动效 MVP**：手持物品按时间平滑降下、替换、抬起与轻微回弹；增强剑/工具挥动；增加轻微疾跑、起跳、落地及已确认命中的视图反馈。方块破坏追加少量材质碎片，放置采用约 200 ms 的材质粒子脉冲代理，不缩放真实区块。全部只改变客户端显示。

按 **F8** 翻页到 **流畅动画设置 → 视角动效设置 → 世界反馈设置 → 人物动作设置**，可分别开关和调整强度。新增动效默认开启，手部强度 65%、视角强度 50%；**减少动态效果** 会停用新增动效。关闭原版视角摇晃或把屏幕效果强度设为 0，会停用新增相机运动；命中反馈也尊重原版受伤倾斜强度。打开菜单、第三人称、旁观者、飞行或乘坐时不追加第一人称视角动效；地图、饮用和拉弓继续使用原版手部流程。原有玻璃背包、液态玻璃菜单和透明快捷栏保持独立设置。

架构审计、注入点、文件清单和验证范围见 [动效集成审计](docs/MOTION-AUDIT.md)。新运行时仅集成到 26.x 分支；legacy 保留原功能。

**26.x · 0.4.1 新增**：底部九格快捷栏和副手槽默认采用与背包一致的透明玻璃底板及槽位描边，选中格保持明亮边框。物品、数量、耐久和攻击指示保持原版。F8 翻页至 **快捷栏设置** 可单独关闭或调整透明度，默认透明度 84%，不跟随容器入场动画或透明度覆盖。

**26.x · 0.4.0 新增**：按 **F8 → 液态玻璃 UI** 随时开关菜单风格。默认关闭；开启后，暂停菜单、选项与 Mod 设置中的常规按钮和滑块使用圆角半透明玻璃、边缘高光与鼠标反光，暂停界面使用冷色背景渐变并保留原版模糊。即时生效，F8 / Esc / 完成保存，下次启动保留选择。此开关独立于「透明玻璃背包」和背包预设；不改变按钮布局或原版输入。采用轻量 GUI 绘制模拟玻璃质感，不提供真实光学折射；第三方自绘控件不保证套用主题。该功能仅在提供 F8 设置的 26.x 分支实现。

**26.1.2 · 0.3.3 更新**：箱子等容器默认放大至 135%，不再随世界锚点距离缩小，并自动适应窗口。生存及创造背包的人物头部会平滑跟随鼠标，原有第三人称也支持。打开 **F8 → 空间与镜头设置** 可调整容器大小；继续切到 **人物头部跟随设置** 可调整开关和左右、上下幅度。头部跟随仅修改显示姿态，不改变实际朝向、镜头或服务器数据。创造模式玻璃界面与原有透明度设置继续保留。

Hoverglass 是一个 Fabric **客户端**模组，纯客户端生效，**服务器不需要安装**。它不替换原版界面逻辑，只改变呈现方式：槽位、配方书、交易、物品搬运和网络协议全部保持原版。

同一套源码支持 **Minecraft 1.20.1 – 26.3**，按版本分成两条实现分支，构建时按目标版本生成各自的 jar。

| 分支 | 覆盖版本 | 能力 |
| --- | --- | --- |
| 玻璃背包（legacy） | 1.20.1 – 1.21.11 | 玩家背包（`InventoryScreen`）玻璃面板化：带过冲与回弹地从左侧滑入，玩家预览从右侧滑入，落位时有一道白光闪光。 |
| 空间容器（spatial） | 26.1 – 26.3 | 在上面基础上扩展到**全部容器界面**，并加入平面空间投影、方块/实体世界锚点、第一人称 ↔ 第三人称镜头过渡、鼠标输入的完整逆变换，以及游戏内 **F8** 设置界面。 |

> 1.20.1 – 1.21.11 只处理玩家背包；从 26.1 起才覆盖箱子、木桶、工作台、熔炉等全部容器，并提供游戏内设置。

## 下载

从 [**Releases 页面**](../../releases) 下载与你游戏版本一致的 jar，文件名见下方版本对照表。不要使用 `-sources.jar`，也不要把不同 MC 版本的 jar 混放。

[**→ 下载最新版本**](../../releases/latest)

## 安装

1. 安装对应版本的 **Fabric Loader 0.19.5 或以上**。
2. 安装对应版本的 **Fabric API**（最低版本见下表）。
3. 从 [Releases 页面](../../releases) 下载与你游戏版本一致的那个 jar，放进实例的 `mods` 文件夹（自行构建时产物在本地 `dist/` 目录，该目录不入版本库）。
4. 确保 Java 版本匹配下表：1.20.1 – 1.20.4 需要 Java 17，1.20.5 – 1.21.11 需要 Java 21，26.x 需要 Java 25。

### 版本对照

| Minecraft | Jar 文件 | 分支 | Java | Fabric API ≥ |
| --- | --- | --- | --- | --- |
| 1.20.1 | `hoverglass-mc1.20.1-0.3.0.jar` | 玻璃背包 | 17 | 0.92.12+1.20.1 |
| 1.20.2 | `hoverglass-mc1.20.2-0.3.0.jar` | 玻璃背包 | 17 | 0.91.6+1.20.2 |
| 1.20.3 | `hoverglass-mc1.20.3-0.3.0.jar` | 玻璃背包 | 17 | 0.91.1+1.20.3 |
| 1.20.4 | `hoverglass-mc1.20.4-0.3.0.jar` | 玻璃背包 | 17 | 0.97.3+1.20.4 |
| 1.20.5 | `hoverglass-mc1.20.5-0.3.0.jar` | 玻璃背包 | 21 | 0.97.8+1.20.5 |
| 1.20.6 | `hoverglass-mc1.20.6-0.3.0.jar` | 玻璃背包 | 21 | 0.100.8+1.20.6 |
| 1.21 | `hoverglass-mc1.21-0.3.0.jar` | 玻璃背包 | 21 | 0.102.0+1.21 |
| 1.21.1 | `hoverglass-mc1.21.1-0.3.0.jar` | 玻璃背包 | 21 | 0.116.17+1.21.1 |
| 1.21.2 | `hoverglass-mc1.21.2-0.3.0.jar` | 玻璃背包 | 21 | 0.106.1+1.21.2 |
| 1.21.3 | `hoverglass-mc1.21.3-0.3.0.jar` | 玻璃背包 | 21 | 0.114.1+1.21.3 |
| 1.21.4 | `hoverglass-mc1.21.4-0.3.0.jar` | 玻璃背包 | 21 | 0.119.4+1.21.4 |
| 1.21.5 | `hoverglass-mc1.21.5-0.3.0.jar` | 玻璃背包 | 21 | 0.128.2+1.21.5 |
| 1.21.6 | `hoverglass-mc1.21.6-0.3.0.jar` | 玻璃背包 | 21 | 0.128.2+1.21.6 |
| 1.21.7 | `hoverglass-mc1.21.7-0.3.0.jar` | 玻璃背包 | 21 | 0.129.0+1.21.7 |
| 1.21.8 | `hoverglass-mc1.21.8-0.3.0.jar` | 玻璃背包 | 21 | 0.136.1+1.21.8 |
| 1.21.9 | `hoverglass-mc1.21.9-0.3.0.jar` | 玻璃背包 | 21 | 0.134.1+1.21.9 |
| 1.21.10 | `hoverglass-mc1.21.10-0.3.0.jar` | 玻璃背包 | 21 | 0.138.4+1.21.10 |
| 1.21.11 | `hoverglass-mc1.21.11-0.3.0.jar` | 玻璃背包 | 21 | 0.141.6+1.21.11 |
| 26.1 | `hoverglass-mc26.1-0.3.0.jar` | 空间容器 | 25 | 0.145.1+26.1 |
| 26.1.1 | `hoverglass-mc26.1.1-0.3.0.jar` | 空间容器 | 25 | 0.145.4+26.1.1 |
| 26.1.2 | `hoverglass-mc26.1.2-0.3.3.jar` | 空间容器 | 25 | 0.155.3+26.1.2 |
| 26.2 | `hoverglass-mc26.2-0.3.0.jar` | 空间容器 | 25 | 0.159.0+26.2 |
| 26.3 | `hoverglass-mc26.3-0.3.0.jar` | 空间容器 | 25 | 0.163.0+26.3 |

## 玻璃背包分支（1.20.1 – 1.21.11）

- 打开背包时，面板从左侧带过冲（约 10%）与回弹地滑入，玩家预览从右侧滑入，落位时有一道白光闪光。
- 玻璃面板自绘：半透明背景、四边描边、全部槽位边框，透明度由配置控制。
- 玩家预览可以关闭（`playerPreview`），位置与高度由比例参数控制。
- 没有游戏内设置界面，所有参数通过 `config/transparent_inventory.json` 手动调整，重启或重新打开背包后生效。
- 动画时长由 `openingAnimationDurationMs` 控制，源码默认 480 ms。

## 空间容器分支（26.1 – 26.3）

### 第一人称背包入场

从第一人称按 **E** 打开背包时，镜头向左后方平滑拉开，让真实人物出现在画面右侧；背包从左侧滑入，轻微越过目标后回弹，最终保留默认 **4°** 的斜面构图。关闭时面板向左退出，镜头回到原视角。玩家的实际位置和朝向不会被修改。

倾斜后的槽位、拖拽物品、配方书和人物预览使用同一变换，鼠标坐标按完整逆矩阵换算。镜头后方和侧方均有墙体碰撞检查。原本已处于第三人称时保留常规容器布局。

### 透明度

按 **F8** 打开设置。容器打开时，设置以覆盖层显示，原容器不会被关闭。

- **全局界面透明度**：所有没有独立配置的界面共用，范围 0–100%。0% 表示完全不透明，100% 表示背景完全透明。
- **当前界面透明度**：只对当前界面类型保存。例如箱子和铁砧可以使用不同透明度，同一类型的箱子共用设置。
- **当前界面跟随全局**：删除该类型的独立透明度。
- **世界背景遮罩强度**：单独控制世界变暗程度；与面板透明度无关。
- **应用参考图玻璃效果**：将生存及创造背包设为 84% 透明度、明亮描边、左滑扫光与右侧人物构图，保留其他容器的独立透明度。更改镜头与入场设置后，关闭并重新打开背包查看完整动画。
- **空间与镜头设置**：调整界面大小、镜头距离、镜头过渡时间，以及是否启用镜头和世界锚点。
- **箱子等容器显示大小**：75–200%，默认 135%；影响箱子、木桶、工作台等，独立于背包构图。长容器按窗口可用空间限制放大。
- **背包入场动画**：可切换玻璃材质、调整人物向右让位幅度、停留倾角和动画时间。
- **人物头部跟随设置**：默认开启。人物头部跟随背包内鼠标平滑左右、上下转动；可分别调整幅度，打开设置期间保持上一视线，关闭背包后恢复原版表现。

物品、数量、耐久条、文字、工具提示和槽位悬停高亮保持可读。玻璃背包的透明度只影响玻璃底和槽位填充，边框及装备提示仍可见；其他容器仍调整原版面板贴图透明度。**Esc / F8 / 完成** 保存并返回原界面。

设置界面支持中文和英文。F8 为固定快捷键；无需 Mod Menu 或 Cloth Config。

## 配置文件

位置：游戏实例的 `config/transparent_inventory.json`，开发客户端对应 `run/config/transparent_inventory.json`。

界面滑块使用“透明度”，JSON 使用“不透明度”：`panelOpacity: 0.65` 对应滑块透明度 35%。文件缺失时生成默认配置，字段缺失使用默认值，非法数值会被夹取到安全范围内。损坏的 JSON 在启动时保留原文件并在内存中使用默认设置；保存时先写临时文件，再替换原文件。

常用字段与示例：

```json
{
  "enabled": true,
  "playerPreview": true,
  "closeAnimation": true,
  "panelOpacity": 0.18,
  "worldDimOpacity": 0.0,
  "screenOpacities": {
    "net.minecraft.client.gui.screens.inventory.ContainerScreen": 0.45,
    "net.minecraft.client.gui.screens.inventory.AnvilScreen": 0.8
  },
  "excludedScreens": [],
  "additionalScreens": [],
  "openingAnimationDurationMs": 480,

  "defaultSpatialHandledScreens": true,
  "worldAnchoringEnabled": true,
  "fallbackToScreenSpace": true,
  "cameraEnabled": true,
  "thirdPersonOnOpen": true,
  "cinematicInventory": true,
  "glassInventory": true,
  "inventoryCameraSide": 0.72,
  "inventoryTiltDegrees": 4.0,
  "cameraDistance": 3.0,
  "cameraYawOffset": 0.0,
  "cameraPitchOffset": 0.0,
  "cameraTransitionDurationMs": 420,
  "guiScale": 1.0,
  "containerScale": 1.35,
  "playerLookAtInventory": true,
  "guiDistance": 1.5,
  "guiVerticalOffset": 0.3
}
```

- `panelOpacity`（0–1）：面板背景不透明度，0 = 全透明，1 = 不透明；未单独配置的界面共用它。
- `screenOpacities`：按界面完整类名覆盖 `panelOpacity`。
- `excludedScreens`：使用完整类名，也能排除某个父类及其所有子类。`additionalScreens` 可显式加入非容器界面；默认不会处理聊天、暂停、主菜单。
- `worldDimOpacity`（0–1）：世界背景变暗强度。
- `playerPreview` / `playerSlideRatio` / `playerCenterXRatio` / `playerCenterYRatio` / `playerHeightRatio`：玩家预览的开关、滑入距离与位置（玻璃背包分支）。
- `inventoryTiltDegrees`（−12–12）：背包停留倾角。
- `cameraEnabled` / `cameraDistance` / `cameraTransitionDurationMs`：镜头开关、距离与过渡时间。
- `openingAnimationDurationMs` / `closeAnimation`：开关动画时长与关闭动画开关。
- 文件中还保留了若干槽位描边透明度、滑入物理（`slideSpeed` / `slideBrake` / `bounce`）和视觉比例的调参字段，均会被夹取到安全范围，可按需修改。

未知模组容器默认使用通用 profile；没有可靠锚点时回退到屏幕相对位置。不同槽位数的箱子和木桶可能使用同一个原版 Screen 类，因此它们的独立透明度会共用。

## 从源码构建

```powershell
.\gradlew.bat '-Pmc=1.21.11' collectRelease   # 构建单个版本到 dist/
.\gradlew.bat '-Pmc=1.21.11' build            # 单版本编译 + 回归
```

`-Pmc=<版本>` 选择要编译的目标 Minecraft 版本（取值见 [`gradle/minecraft-targets.json`](gradle/minecraft-targets.json)），每次只编译一个目标。产物由 `collectRelease` 统一复制到根目录 `dist/`（已在 `.gitignore` 中忽略，不会进入版本库），文件名形如 `hoverglass-mc<版本>-<mod 版本>.jar`。

构建使用 Loom **1.17.20**。26.x 已不再混淆，使用 `net.fabricmc.fabric-loom` 且不需要映射；1.20.1 – 1.21.11 使用 `fabric-loom` 并套用 Mojang 官方映射。版本号统一在 [`gradle.properties`](gradle.properties) 中维护。

空间容器分支的 `build` 会附带一个纯 Java 回归（配置、输入反算、动画数学）；客户端界面验证记录见 [测试记录](docs/TESTING.md)。

## 架构

`SpatialScreenRegistry` 选择 profile；`SpatialScreenManager` 持有会话、动画和锚点；`SpatialGuiRenderer` 包裹原版提取流程；`SpatialInputMapper` 用同一份变换反算输入；`SpatialCameraController` 管理镜头和恢复。透明度在原版不可变渲染状态提交时处理，不改变物品或菜单内容。玻璃背包分支则通过一个 `InventoryScreen` mixin 直接自绘面板并复用同一套时间轴动画控制器。

详见 [架构文档](docs/ARCHITECTURE.md)。

## 已知限制

- 空间容器分支使用平面投影，倾斜通过二维旋转、剪切和横向压缩实现；没有世界深度遮挡、真实透视梯形或曲面 GUI。
- 玻璃背包分支只覆盖玩家背包，且没有游戏内设置界面。
- 对使用自定义渲染管线、特殊画中画状态或自行读取原始鼠标坐标的第三方界面，需要适配或加入排除列表。尚未验证 JEI/REI/EMI、控制器或 VR。
- 收集到的 jar 全部要求 Fabric API（含玻璃背包分支，当前版本对 1.20.1 – 1.21.11 亦声明该依赖）。

## 许可

本项目以 **MIT License** 发布，见 [LICENSE](LICENSE)。

## 致谢

实现前参考研究：[Spatial GUI](https://github.com/tastytrash/Spatial-GUI)、[Fabric 26.2 开发说明](https://fabricmc.net/2026/06/15/262.html)、[Fabric 客户端游戏测试](https://docs.fabricmc.net/develop/automatic-testing)。

Hoverglass 为独立实现，**不包含、也未复制 Spatial GUI 的任何源代码**；两者仅在“把容器界面透明化 / 空间化”这一概念方向上有交集。
