# AutoClicker 项目约定

## 原生 SDK 特例

- 本工程是用户明确指定的特例：只使用 Android 原生 SDK，直接实现连点器功能，不使用任何应用开发框架。
- 页面直接继承 `android.app.Activity`，使用原生 XML 布局、控件和 `findViewById`；不使用 MVVM、DataBinding、ViewBinding、Compose、AndroidX 或 Material Components。
- 不接入、不创建 `lib_common`，不依赖 `JHGK_LIBS`，不引用其他工程的公共基础类或共享模块。
- 不引入 ViewModel、MutableStateFlow、LiveData、协程、依赖注入等架构或状态管理依赖。业务代码使用 Kotlin 和 Android SDK 自带 API，允许 Kotlin 标准库；利用 Kotlin 的空安全、`when`、作用域函数等语法简化实现。
- 上述规则覆盖全局约定中关于 Android 页面必须使用 MVVM + DataBinding、`lib_common` 基类及相关框架组件的要求。其他全局约定继续生效。

## 按功能分包

- 参考同级 AndroidCarPlay 的 `modules.<功能>` 结构，同一功能的页面、适配器、控件及业务实现放在一起，不在根包按 Activity、Adapter、Service 等技术类型集中归类。
- `modules.main` 放首页、首页菜单和首页弹窗；`modules.clicker` 放连点服务实现、手势调度、悬浮控制和播放图标；`modules.theme` 放主题页面、色板、适配器及主题窗口处理；`modules.privacy` 放隐私政策页面。
- 跨功能配置放在 `common.settings`，工具放在 `common.utils`，公共界面控件放在 `common.widget`。这些只是当前 app 内的 Kotlin 包，不创建额外 Gradle 模块或框架。
- 分包时同步更新 Kotlin 引用、XML 自定义控件名称、Manifest、无障碍设置入口和色板生成脚本。保留 applicationId、SharedPreferences 文件名及既有配置键。
- `.service.ClickerAccessibilityService` 仅作为兼容入口继承 `modules.clicker` 中的服务实现，Manifest 保持原服务名称，以保留系统已存储的无障碍授权。旧 `.MainActivity` 通过 `activity-alias` 指向新首页，保留桌面启动入口。

## 功能与停止机制

- 仅引导用户手动启用无障碍服务，以执行跨应用点击。悬浮控制使用 `TYPE_ACCESSIBILITY_OVERLAY`，不声明或请求 `SYSTEM_ALERT_WINDOW`，也不能再通过 `Settings.canDrawOverlays` 阻止显示或运行。
- 黑色半透明悬浮按钮外形始终是 48dp 圆形，可以拖动；轻点后在圆心对应的屏幕坐标连续点击。
- 播放和停止复用原位置的圆形按钮，只改变内部三角形／正方形图标。开始、停止和图标动画均不得改变按钮位置或外形。
- 不显示独立的红色停止／关闭按钮。运行中只响应原位圆形按钮内的停止触摸，其他位置的触摸不作为停止操作；保留音量键兜底。
- 采用用户确认的透明停止热区方案：外观仍是原位 48dp 圆形，透明输入窗口围住圆心，只留 2～3 个物理像素的模拟点击穿透孔。正中穿透孔无法接收手指停止，帮助中需要说明可稍偏离圆心或按音量键；不使用隐藏 API，不读取其他应用内容。
- 点击目标频率可设置，默认 100 次/秒；实际频率受系统和目标应用影响，不承诺达到目标频率。
- 停止操作优先于连点调度。圆形按钮内的透明停止热区在真实输入的 `ACTION_DOWN` 时停止，不依赖 `ACTION_UP` 或普通点击回调；模拟点击不能误触停止。
- 保留音量键停止、锁屏停止和可设置的限时自动停止；音量键受设备及其他无障碍服务影响，不能作为唯一停止入口。
- 一次只派发一个短点击手势，完成后再安排下一次，不积压点击任务。停止、服务断开或销毁后，旧回调不得重新启动连点。
- 运行中的圆心允许模拟点击穿透，停止热区不能覆盖注入坐标。其他位置的触摸导致单次手势取消时延迟重试，不能结束整个会话；明确停止后的旧回调不得重试。旋转屏幕、关闭悬浮窗或服务失效时停止，解锁后不得自动恢复。
- 横竖屏或可用屏幕边界变化时，只停止连点并保留悬浮按钮，按原可移动区域的相对位置迁移到新边界内；窗口方向跟随底层应用，横竖屏适配不对图标额外叠加旋转。配置通知和 Insets 变化合并处理，屏幕稳定且旧手势结束后才允许手动再次开始，不自动恢复连点。普通主题、语言或刷新率变化不能直接关闭悬浮窗。

## 界面风格

- 参照同级 AndroidCarPlay 的主题设置，遵循 Material Design 2 的颜色角色、暗色表面、可读性及阴影层级；通过原生 Kotlin 自定义 View、Drawable 和 XML 实现，不引入参考工程的 Material Components、AndroidX 或其他框架。
- 顶部栏使用独立自定义 View `Actionbar`，首页左侧为 Material Design 叉号，点击关闭；右侧更多菜单按顺序提供“设置、主题、帮助、关于、隐私政策”，分别配 Material Design 齿轮、调色板、问号、信息、隐私盾牌图标。使用原生 `ListPopupWindow` 和独立 `MainMenuAdapter` 在最低 API 24 上显示图标，不使用反射或隐藏 API。菜单项最小高度 48dp，图标 24dp，水平内边距及图文间距 16dp，菜单圆角 8dp。独立 `ThemeSettingsActivity`、`PrivacyActivity` 使用返回箭头，不显示更多菜单。顶栏手机竖屏高 56dp、横屏高 48dp，平板高 64dp，标题 20sp。
- 独立主题页面提供“跟随系统、亮色、暗色”，默认跟随系统，选择后立即保存并应用，返回首页同步刷新。主题与点击参数分别保存，沿用已有 SharedPreferences 文件、键及颜色 ID，避免互相覆盖。
- 统一使用标准配色，不提供旧版白色／粉色的“默认配色”或“自动配色”。新安装、旧版 `default`／`system` 配置及无效颜色 ID 均使用标准靛蓝 500；已有有效 Material 颜色 ID 保留。基础主题也引用标准色板资源，不跟随系统壁纸配色。
- 主题页参考 AndroidCarPlay，以带选中勾号的 48dp 圆形色块展示官方经典色板：19 个色系、完整普通及强调色阶、黑和白，共 256 个颜色。显示模式仍独立支持“跟随系统、亮色、暗色”。
- 官方色值快照在 `tools/material_palette_source.json`，来源为 `https://m1.material.io/style/color.html#color-color-palette`。`tools/generate_material_palettes.py` 生成完整颜色资源、日夜主题叠加样式及 Kotlin 目录；默认离线生成，`--refresh` 才重新抓取官方表格，不触发编译。生成文件不要手工逐项维护。
- 色块、Actionbar 和其上方状态栏背景必须使用相同的所选原色，不能再为顶栏暗化、降饱和或应用深色变体。顶栏文字、图标和状态栏图标按该原色的对比度自动选择黑／白，包括暗色模式下选中浅色的情况。按钮及文字链接仍使用同色相的可读强调色，暗色模式可降低强调色饱和度；正文、链接和按钮文字对比度至少 4.5:1。暗色页面为 #121212，卡片、菜单、弹窗按层级使用中性灰色表面。界面通过主题属性取色，不硬编码旧版粉色。
- 色块使用原生 `ListView`、独立 `ThemeSwatchAdapter` 和自定义 `ThemeSwatchButton`，模式下拉使用原生 `Spinner` 和独立 `ThemeModeAdapter`。列表头与色块统一滚动，列数适配窗口宽度；重建时保存首个可见色块及偏移，避免横屏、大字号或切换主题导致内容无法访问。不引入 AndroidX、RecyclerView 或其他框架。
- 自主定义的间距、内外边距、可见控件尺寸使用 8dp 的倍数；字体使用 sp，字号只使用偶数。矩形界面使用克制的 4dp／8dp 圆角：按钮和输入框为 4dp，卡片、菜单及设置／帮助／关于三个弹窗为 8dp。色块和图标按钮涟漪使用圆形。用户明确指定的 Actionbar 阴影为 `android:elevation="4dp"`；卡片 1dp、实体按钮常态 2dp／按下 8dp 按 M2 标准设置，不套用间距倍数规则。系统 Insets、矢量路径坐标和模拟点击穿透孔的物理像素输入边界保持实际值。
- 图标采用 Material Design 矢量图标。悬浮开始/停止的内部图标使用三角形/正方形，变形时同步绕图标中心旋转 180 度：三角形变正方形时顺时针半圈，返回三角形时逆时针半圈，最终三角形朝右。旋转与变形共用动画进度，快速切换时连续反向；外层 48dp 圆形保持不变，动画不能延迟停止操作。
- `MainActivity` 的主开始按钮固定于底部，高度为 48dp，直接作为根布局子控件，不再额外套底部容器；内容区域独立滚动，适配导航栏和键盘 Insets。根布局绘制状态栏与 Actionbar 的连续背景，适配配色后的状态栏图标。
- 设置输入框默认紧凑到约 48dp，左右内边距 16dp、上下 8dp，字号放大时允许增长；上方标签取消正文额外行距并垂直居中，使图标与文字对齐。
- 点击参数收进设置弹窗，主题收进独立 Activity，使用说明收进帮助弹窗；首页不重复摆放。关于弹窗包含免责声明、邮箱 `iqosjay@gmail.com` 和仓库地址 `https://github.com/iqosjay/AutoClicker`。邮箱和仓库地址带下划线并使用 `setClickAction`；邮箱以 `ACTION_SENDTO`、`mailto` 打开邮件应用，仓库以 `ACTION_VIEW` 打开网页。仓库地址的显示和跳转共用同一个 HTTPS 字符串资源，文档也统一使用 HTTPS。没有可用外部应用时友好提示。
- 自定义弹窗采用 Builder，构造函数只接受 Builder，按钮结果通过返回 `Int` 常量的 `await()` 获取。该工程只使用 Kotlin 标准库提供的挂起／续体机制，不引入 `kotlinx.coroutines` 或其他框架；页面销毁时关闭弹窗并结束等待，旋转时保留未保存的输入。

## 隐私与离线

- 本应用是单机工具，不声明网络、存储、定位、相机、麦克风、联系人等无关权限，不增加联网、广告或统计 SDK。保留服务声明中的 `BIND_ACCESSIBILITY_SERVICE` 系统绑定保护。
- 无障碍能力仅用于模拟手势、无障碍悬浮控制和音量键停止；不读取窗口内容，不截屏，不订阅其他应用的界面事件。屏幕旋转继续使用 DisplayManager、Insets 及现有轮询检测，不依赖无障碍窗口事件。
- 首次启动展示不可取消的“隐私政策”弹窗，返回键和点击外部不能关闭，仅提供“退出”和“我同意”两个按钮。只有“我同意”写入当前版本的同意状态；“退出”隐藏悬浮控制并关闭应用任务，不记录同意。旧版已查看状态不等于同意，升级后需明确同意一次。生命周期销毁导致的 dismiss 不写入状态，旋转后恢复弹窗和滚动位置。同意政策不会自动授予系统权限。
- 菜单“隐私政策”打开 `PrivacyActivity`。页面和首次弹窗复用 `content_privacy.xml` 与 `values/privacy.xml`，包括权限用途、能力范围、本机存储、撤销方式、外部链接及联系邮箱，全部离线展示。`PrivacyActivity` 的邮箱带下划线，通过 `setClickAction`、`ACTION_SENDTO` 和 `mailto` 打开邮件应用，预填收件人 `iqosjay@gmail.com`；与关于弹窗共用邮件工具，没有可用邮件应用时友好提示。
- 点击参数、主题和政策同意状态只保存在应用私有目录。关闭 `allowBackup`，并在新旧备份规则中排除所有数据域的云备份与设备迁移。打开仓库或邮箱会调用外部应用，政策中明确外部应用可能联网。

## 执行与代码风格

- 面向用户的回复和进度说明遵循全局双语约定：英文部分称呼“Roy”，中文部分称呼“杰哥”，英文在前、中文在后；中文请求先给出英文翻译，同一条消息不重复翻译。
- 未经用户明确要求，不编译，不运行会隐式触发编译的验证命令，不提交 Git。
- 文本文件统一 UTF-8，无 BOM。
- Kotlin 使用 2 空格缩进；优先 `val`；使用前置自增；常量放在比较左侧；分支和循环必须使用大括号；Boolean 条件直接判断。
- Kotlin 捕获异常统一使用 `catch (x: Throwable)`；多行 `when` 对齐 `->`；`apply` 中给当前对象的属性赋值时显式使用 `this.`；连续操作同一对象时使用合适的作用域函数。
- 注释解释原因，不复述代码。
- SharedPreferences 的文件名和键字符串提取为常量，读写共用常量。
- 所有普通按钮通过 `ViewUtils.setClickAction` 设置点击动作，复用项目的防抖实现；仅该扩展函数内部调用原生 `setOnClickListener`。立即停止及同一播放／停止入口使用零防抖间隔，透明停止热区仍直接在 `ACTION_DOWN` 停止，不能让防抖阻碍停止。
- 自主命名中的 URL / URI 缩写只能使用 `url`、`uri`、`URL`、`URI`，保留 Android SDK 等外部接口的原名。
- 下拉相关的自主命名统一使用 `Dropdown`／`dropdown`，不写作 `DropDown`／`dropDown`；Android SDK 已定义的 `getDropDownView` 回调和 `android:dropDownWidth` 属性保留原名，以保持接口兼容。

## 本机签名

- `debug` 与 `release` 统一使用名为 `public` 的签名配置，签名文件为工程内的 `signing/roy_pub.jks`。这是用户专为开源提供并明确授权公开的签名，不使用或复制其他工程共用的 `roy.jks`、`roy.txt`。
- 用户要求在 `app/build.gradle.kts` 中直接明文配置该公开签名的密码和别名；`storeFile = file("../signing/roy_pub.jks")` 使用相对路径，不声明 `signingDirectory` 或读取外部密钥信息文件。
- 当前公开 Release 包放在根目录 `app_1.0.apk`，README 使用相对下载链接。Git 仅对该 APK 和该专用签名解除忽略，其他构建输出及密钥继续忽略。沿用远程仓库的 GPL-3.0 许可证和提交历史。
- 配置签名不代表授权编译、安装或发布；继续遵守用户明确要求后才编译的约定。
