# AutoClicker · 连点器

一个使用 Kotlin 与 Android 原生 SDK 实现的离线连点工具。通过可拖动的悬浮圆点定位，轻点开始，再按圆点停止。

**[点击下载 app_1.0.apk](./app_1.0.apk?raw=true)**

适用于 Android 7.0（API 24）及以上版本。无需注册、登录或联网。

## 功能

- 48dp 黑色半透明悬浮圆点，可拖动定位。
- 播放与停止图标平滑变形，并同步旋转 180°。
- 目标频率为 1–100 次/秒，默认 100 次/秒；实际速度受手机系统和目标应用影响。
- 自动停止时限为 1–3600 秒，默认 60 秒，每次启动重新计时。
- 支持原位按钮、音量键、锁屏及限时停止。横竖屏切换会停止连点并保留悬浮按钮，需手动重新开始。
- 独立主题设置页面，支持跟随系统、亮色、暗色，以及自动配色和 256 个经典 Material 颜色。
- Actionbar 使用所选色块的原色，文字、图标和状态栏自动适配黑白前景。
- 首次启动展示隐私政策，也可随时从菜单进入隐私政策页面查看。

## 使用

1. 安装 APK，查看首次启动的隐私政策说明。
2. 在首页进入系统无障碍设置，手动启用「连点器」。
3. 在右上角「设置」中调整点击频率与自动停止时间。
4. 点击「显示悬浮控制」，打开目标应用，将圆点拖到需要点击的位置。
5. 轻点三角形开始，按圆形按钮停止。停止后可重新拖动定位。

运行时圆心留有极小的点击穿透孔，停止时请稍偏离圆心按下；也可按音量键或锁屏停止。音量键表现受设备及其他无障碍服务影响，不能作为唯一的停止方式。

如果手机中已安装使用其他签名的旧版本，需要先卸载旧版，再安装本 APK；卸载会清除本地设置。

## 权限与隐私

应用只需要用户手动启用无障碍服务，用于模拟点击、显示悬浮控制及通过音量键停止。悬浮窗口使用无障碍服务提供的窗口能力，**不需要额外的悬浮窗授权**。

应用不申请网络、存储、定位、相机、麦克风、联系人等权限，不读取屏幕内容，不订阅其他应用的界面事件，不包含广告、统计或联网 SDK。点击参数、主题及隐私提示状态仅保存在本机；系统云备份与设备迁移已关闭或排除。

关于页面中的仓库和邮箱链接由外部浏览器或邮件应用打开。外部应用可能联网，连点器本身不建立网络连接。

完整说明见应用内「隐私政策」。首次弹窗与独立页面共用同一份[隐私政策文案](app/src/main/res/values/privacy.xml)。

## 构建

工程使用 Kotlin、Android 原生 XML、Activity、自定义 View 和 Drawable，不依赖 AndroidX、Compose、Material Components、JHGK_LIBS 或其他应用开发框架。

构建环境：

- Android SDK Platform 37。
- Gradle Wrapper 9.5，Android Gradle Plugin 9.3.3。
- Gradle Daemon 使用 JDK 25，配置见 `gradle/gradle-daemon-jvm.properties`。
- Java 编译兼容级别为 11，最低运行版本为 Android API 24。

在 Android Studio 中打开工程并配置 SDK，或在本地 `local.properties` 中设置 `sdk.dir`。首次构建可能下载 Gradle、JDK 或编译依赖；这些是开发环境的网络访问，安装后的应用无需联网。

Windows PowerShell：

```powershell
.\gradlew.bat :app:assembleRelease
Copy-Item app/build/outputs/apk/release/app-release.apk app_1.0.apk
```

macOS / Linux：

```bash
bash gradlew :app:assembleRelease
cp app/build/outputs/apk/release/app-release.apk app_1.0.apk
```

Gradle 产物位于 `app/build/outputs/apk/release/app-release.apk`，仓库根目录的 `app_1.0.apk` 为提供下载的 Release 包。

## 公开签名

本项目使用专用于开源的 `signing/roy_pub.jks`，Debug 与 Release 共用该签名。签名配置位于 [app/build.gradle.kts](app/build.gradle.kts)，`storeFile` 通过相对路径 `../signing/roy_pub.jks` 引用，不依赖工程外的密钥文件。

## 配色资源

经典 Material 色板包含 19 个色系及其普通、强调色阶，加上黑与白，共 256 个颜色。原始色值来自 [Material 官方色板](https://m1.material.io/style/color.html#color-color-palette)，快照保存在 `tools/material_palette_source.json`。

修改生成逻辑后，可使用 Python 重新生成颜色、主题与 Kotlin 目录：

```bash
python tools/generate_material_palettes.py
```

默认离线生成，不触发编译。仅在需要重新获取官方色板时添加 `--refresh`。不要逐项手工修改生成的颜色或主题文件。

## 开源与联系

- 仓库：[iqosjay/AutoClicker](https://github.com/iqosjay/AutoClicker)
- SSH：`git@github.com:iqosjay/AutoClicker.git`
- 邮箱：<iqosjay@gmail.com>
- 许可证：[GNU GPL v3.0](LICENSE)

请仅在你有权操作且允许自动化的场景下使用，并遵守目标应用的规则。实际点击效果受系统和目标应用影响；开始前请确认位置、频率及停止时限。
