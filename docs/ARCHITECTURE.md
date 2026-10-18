# 空间界面架构

## 0.3.3 容器可读性与人物视线

非 PLAYER 容器的 `containerScale` 默认为 1.35，范围 0.75–2.0；与 `guiScale` 及 profile 缩放组合后按窗口四周各 10 GUI 像素留白裁定最终大小。世界锚点继续决定面板中心，不再通过深度缩小此类容器。输入仍使用同一份最终变换，因此放大不改变槽位命中逻辑。

`AvatarRendererMixin` 在玩家模型提取完成后调用 `InventoryLookController`，仅对本地玩家及 PLAYER 类物品栏生效。鼠标读取未映射的窗口坐标，再经最近呈现变换反算到原版面板坐标，`InventoryLookAngles` 生成有限幅目标，`LookTracker` 按单调时钟平滑。控制器只更改当帧 `AvatarRenderState.yRot/xRot`；不写玩家实体、身体或相机角度，不发送数据包。设置覆盖层打开时冻结上一视线，关闭物品栏或关闭功能后原版下一帧自然恢复；卸载世界显式清理状态。没有外部人物构图的玻璃背包仍调用原版内嵌预览的鼠标跟随。

## 数据流

```text
原版 Screen
  ↓ ScreenMixin：包裹 extractRenderStateWithTooltipAndSubtitles
SpatialScreenRegistry：分类、父类匹配、排除
  ↓ SpatialScreenProfile：锚点偏好、缩放、偏移
SpatialAnchorResolver：消费最近一次可靠交互
  ↓ SpatialScreenManager：每个界面的会话与动画
SpatialTransform：本帧 billboard 投影快照
  ↓ SpatialGuiRenderer：包裹原版 GUI 提取矩阵
原版 Screen / GuiRenderState / 原版 GUI 渲染器
  ↑ SpatialInputMapper：同一快照的逆变换
原版鼠标回调与菜单操作
```

菜单、槽位、合成、交易与网络包均由 Minecraft 负责。Mixin 不修改 `Slot` 的位置，也不复制任何容器 GUI。

## 版本与拦截点

目标为 Minecraft 26.2 的实际未混淆类：`AbstractContainerScreen`、`Screen.extractRenderStateWithTooltipAndSubtitles`、`GuiGraphicsExtractor`、`net.minecraft.client.renderer.state.gui.GuiRenderState`。

26.2 的 Screen 绘制分成背景、内容与延迟元素提取。包裹最外层共同入口，确保按钮、配方书、侧栏、拖拽物品、文字、剪裁和工具提示一起变换。配方书内部再次提取背景时不重新推进动画。原版 `leftPos/topPos` 保持不变。

`GuiGraphicsExtractor` 中保存的鼠标坐标也在提取范围内更新、结束后恢复，使文本控件与传给 Screen 的坐标一致。

## 注册表与分类

选择顺序：先检查排除列表，然后是精确类、最近的已注册父类、通用容器。未匹配的普通 Screen 只有显式出现在 `additionalScreens` 中才使用屏幕相对 profile。

- `InventoryScreen` / `CreativeModeInventoryScreen` → PLAYER。
- `MerchantScreen` / `HorseInventoryScreen` → ENTITY。
- `AbstractContainerScreen` → 通用交互来源，优先最近一次方块或实体交互。
- 未知来源 → SCREEN_SPACE。

类匹配结果缓存；注册新 profile 时清缓存。配置开关、排除列表在解析时检查，不被缓存固化。扩展可调用 `SpatialScreenRegistry.INSTANCE.register(ScreenClass, profile)`。

## 坐标系统与逆变换

1. **原始窗口坐标**：鼠标回调给出的窗口客户区像素；考虑 `getScreenWidth/getScreenHeight`，不假定 DPI、分辨率或 GUI 缩放。
2. **GUI 窗口坐标**：原始坐标乘 `guiScaledWidth / screenWidth` 等比例得到。
3. **原版 GUI 坐标**：Screen 原本收到的坐标。槽位相对 `leftPos/topPos` 布局，控件可使用原版窗口坐标。
4. **世界坐标**：方块中心、实体上方或玩家相对位置，单位为方块。
5. **投影坐标**：游戏相机的视图投影矩阵把世界点转成 NDC，再转成 GUI 窗口坐标。

当前面板使用二维仿射投影，常规容器为平移与统一缩放，第一人称背包额外使用旋转、剪切及横向压缩。无需创建每个鼠标事件的射线对象，使用完整二阶矩阵逆变换。未来加入真实透视偏转时，需要单应矩阵或射线/平面求交。

设原版内容中心为 `o`，投影面板中心为 `c`，矩阵 `M = R(tilt) × shearX × scale(s × widthRatio, s)`：

```text
窗口点 = c + M × (原版点 - o)
原版点 = o + inverse(M) × (窗口点 - c)
原版拖拽增量 = inverse(M) × 窗口增量
```

`SpatialTransform` 是不可变快照。输入使用**最近一次已呈现帧**的数据；下一帧开始时生成新快照，并同时用于本帧绘制和悬停。`MouseHandlerMixin` 拦截实例版 scaled getters，不修改负责原始像素换算的静态工具方法。拖拽增量单独反算，滚轮只转换其定位坐标，滚动量保留原值。

invalid/NaN/零缩放不会进入除法；输入退回原坐标。过小的窗口和过远的锚点会限制缩放以保留可读性。配方书打开时，其宽度计入内容范围。

鼠标 X/Y 的映射必须同时读取两个原始分量，避免旋转时交叉项丢失。实例 getter 的另一分量使用未修改的静态换算方法，避免递归。剪裁区域使用四角变换后的最大边界，剪裁命中检测也转换到窗口坐标。

`InventoryEntrance` 将背包停留中心设在窗口左侧，并为右侧人物留出空间；宽度包含已展开的配方书。动画位移使用 ease-out-back，透明度使用单调 smoothstep，避免回弹时 alpha 超过范围。关闭开始时分别捕获位移与透明度，保证入场中途关闭不会跳变。会话在切换相机前保存是否由第一人称打开。

## 透明度

JSON `panelOpacity` 表示不透明度；UI 透明度滑块展示 `1 - panelOpacity`。`screenOpacities` 以完整 Screen 类名覆盖全局值，删除键即恢复继承。范围均为 0–1。

`GuiRenderStateMixin` 在**提取时**复制 `BlitRenderState` 和 `ColoredRectangleRenderState` 并乘原有 alpha，不使用跨帧 GPU 全局状态。数量、文字和物品状态不改色。延迟工具提示、物品装饰（耐久/冷却/数量）和原版槽位高亮使用不透明范围，世界遮罩另行控制。

0.3.1 的生存背包可启用 `glassInventory`：`InventoryScreenMixin` 仅替换背景与标签外观，`GlassInventoryRenderer` 按原版 Slot 坐标绘制玻璃底、槽位描边、装备轮廓与扫光。菜单内容、点击逻辑、配方书和槽位仍由原版提取。自绘 chrome 在局部 deferred 范围内自行提交 alpha，避免重复乘透明度；玻璃底和槽位填充随透明度变化，边框与空装备提示保持可读。禁用玻璃时恢复原版背景及内嵌玩家模型；没有电影镜头时保留内嵌预览。

入场使用归一化 ease-out-back，起点为 0、终点为 1，约 10% 过冲。闪光起点由第一次越过停留位置的解析时间决定，因此漏帧不会推迟闪光；关闭和零时长动画不会触发入场闪光。人物右侧构图由 `inventoryCameraSide` 控制，范围 0–0.95；原有对角碰撞射线覆盖全部侧移。

0.3.2 将创造物品栏注册为 PLAYER profile。创造模式按键直开、以及已经切换镜头的生存背包重定向到创造背包，都保留第一人称入场意图。布局额外计入上下分类标签与 Fabric 翻页按钮所占区域。`CreativeInventoryScreenMixin` 仅包裹原版背景、标签及滚动条的贴图提交，保留 vanilla/Fabric 的分类图标、分页、搜索、提示及槽位流程；玩家背包标签采用原版可见槽位布局，屏幕外的隐藏槽位不绘制描边。电影镜头下不再绘制重复的小人物。

`creativeGlassDefaultsApplied` 记录创造玻璃默认值是否已迁移：首次清理配置时仅对缺少独立设置的创造界面添加 0.16 不透明度，后续选择“跟随全局”不会被保存时重新插入。预设会同时设置生存和创造物品栏，不改变其他容器覆盖值。设置标题包含实际加载的模组版本，便于核对 jar。

渲染范围和矩阵都使用 `try/finally` 恢复，避免下一帧、HUD 或设置界面继承面板透明度。

## 原版 3D 预览

人物、附魔书、锻造预览和织布机旗帜等使用原版画中画状态，默认 `pose()` 为单位矩阵，不会自动跟随 `GuiGraphicsExtractor.pose()`。

`PictureInPictureMixin` 为五种原版画中画记录附加本帧空间矩阵，并提供变换后的 bounds。原版画中画 renderer 继续按原尺寸渲染其纹理，最终 blit 使用附加矩阵，避免动画过程中反复改变画中画纹理大小。未另建全屏帧缓冲。

## 锚点生命周期

`InteractionMixin` 在 `MultiPlayerGameMode.useItemOn` / `interact` 成功返回后保存方块坐标或实体 ID，与当前 ClientLevel 绑定。失败交互清理候选。

候选只能消费一次，1.5 秒后过期，距离超过 10 方块无效。打开玩家背包会丢弃旧候选。方块不要求有 BlockEntity，工作台等也能提供锚点。

已绑定锚点随会话存活；实体每帧重新取位置。方块消失、实体移除、世界改变或目标进入相机背面时，使用屏幕相对回退。退出世界清理候选和镜头，避免实体 ID 或旧坐标串到另一世界。

## 镜头

`SpatialCameraController` 保存原 CameraType，使用单独的可反向过渡控制器插值距离和 yaw/pitch 偏移，不写玩家朝向。首次从第一人称打开时临时选第三人称背面；原本第三人称则保留原方向。关闭完成、卸载世界时恢复保存值。打开期间显式切换视角会被尊重。

调整发生在 `Camera.alignWithEntity` 内部的第三人称移动之前，修改传给原版 `getMaxZoom` 的请求距离。原版八条碰撞射线继续缩短距离；相机的裁剪视锥与后续投影在调整后建立。

第一人称背包还沿相机左方平移，延后于后退距离推进，形成角色逐渐移向画面右侧的构图。`CameraMixin` 对完整对角移动追加八条体积偏移射线，在后方和侧方墙体前同时缩短位移；不直接修改玩家或渲染假人物。侧向偏移随镜头关闭过渡恢复。

正常断开连接与 `clearClientLevel` 是不同路径，因此清理统一放在二者都会调用的 `Minecraft.updateLevelInEngines(ClientLevel, boolean)`。在传入 null、且实际世界引用已经清除后恢复镜头、保存并清理覆盖层、刷新会话，避免卸载过程中重新开启镜头。

## 设置覆盖层和关闭

容器内 F8 创建独立的 `SpatialSettingsScreen` 对象，但不把它交给 `Gui.setScreen`；原 Screen/menu 实例保持不变，也不会触发 `menu.removed`。原版容器继续提取，在恢复矩阵后绘制设置覆盖层。

覆盖层打开期间，鼠标和键盘通过同一分发入口全部转交设置对象。Esc/F8 保存并关闭覆盖层。正常关闭容器时，先运行反向插值，期间屏蔽物品输入；结束后调用原版 `onClose`。服务器替换 Screen、断线和死亡不被延迟。

## 回退与性能

- 主菜单、聊天和暂停默认原样绘制；无界面时无 GUI 变换或容器扫描。
- 排除列表优先于所有 profile。
- 投影计算异常使用 session-local circuit breaker，后续按原版绘制，不反复报错。
- 不吞掉原版 Screen 自身的异常，不在已经提交一半的同一 GUI 状态上重试绘制。
- 未知画中画状态触发兼容回退；保留当前已呈现帧的输入快照，下一帧切回原版时同步恢复原始输入。
- profile 类匹配缓存；每帧只处理当前界面。世界坐标投影与标准不可变 GUI 状态会有少量分配，没有每帧反射和全屏纹理分配。
- 第三方自定义画中画或直接使用原始鼠标的界面需要兼容适配；可立即通过排除列表恢复原版。

发布 jar 中没有游戏测试入口、测试键盘/鼠标 Invoker 或测试世界数据。
