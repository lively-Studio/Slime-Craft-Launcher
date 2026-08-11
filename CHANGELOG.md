# 版本日志 / Changelog

## DEV2026.1.0-SNAPSHOT-11

- fix(upgrade): NIGHTLY channel 优先返回 DEV release，避免因 STABLE 排在前面导致搜不到最新开发版
- chore: 版本号升级至 SNAPSHOT-11

## DEV2026.1.0-SNAPSHOT-10

- fix(upgrade): applyUpdate 改用 `isOfficial()` 判定，允许 nightly 构建完成更新应用，修复下载后仍回到更新页面的问题
- chore: 版本号升级至 SNAPSHOT-10

## DEV2026.1.0-SNAPSHOT-9

- ci: 所有构建 workflow（gradle/macos/windows/linux）添加 JAR 签名密钥准备步骤，配合 `attachSignature` 对 shadow JAR 签名
- fix(upgrade): UpdateHandler 改用 `IntegrityChecker.isOfficial()` 判定，允许 nightly 构建在未签名情况下自更新；仅在当前 JAR 已签名时验证下载 JAR 签名
- chore: 版本号升级至 SNAPSHOT-9

## DEV2026.1.0-SNAPSHOT-8

- fix: 实现更新检查器，查询 GitHub Releases API 检测新版本
- chore: 版本号升级至 SNAPSHOT-8

## DEV2026.1.0-SNAPSHOT-7

- fix(theme): 替换 root.css 中 20+ 处硬编码青色 rgba 值为 accent 变量，开关底板/选中背景/涟漪动画等现在跟随自定义主题色
- fix(theme): 取消 TabPane 顶栏、启动按钮、加载条等组件的硬编码颜色，改为跟随 MonetFX primary
- fix(theme): 修复自定义主题颜色不生效问题（CSS 变量覆盖选择器从 `.root` 改为 `*`）
- fix(theme): 通过动态 accent 覆盖样式表将自定义颜色注入 CSS
- fix: 当前目录不可写时回退到 `user.home/.scl`（修复 macOS .app 启动无法创建目录）
- fix(ci): macOS x86_64 DMG 改用 Rosetta 2 交叉构建
- fix(ci): 移除 Windows aarch64（JavaFX 无 Windows ARM64 原生库）
- fix(ci): 移除 `--linux-deb-section`（JDK 18+ 选项，JDK 17 不支持）
- fix(ci): jlink `--include-locales` 使用 BCP 47 格式（`zh` 而非 `zh_CN`）
- fix(ci): Windows MSI 版本号字段限制在 0-255 范围内
- ci: 新增 macOS Intel / Linux ARM / Windows ARM 交叉架构打包支持

## DEV2026.1.0-SNAPSHOT-6

- ci: 新增跨平台打包工作流（macOS DMG / Windows EXE / Linux DEB+RPM），基于 jpackage
- feat: 添加游戏通知弹窗功能，支持成就/死亡/游戏模式提示
- fix: 移除反馈按钮，约束对话框宽度
- fix: WorkingDialogPane 自动关闭，使用主题 accent 颜色
- feat: 添加高级玩家模式切换、显示工作对话框开关
- i18n: 新增字符串翻译至全部 6 种语言

## DEV2026.1.0-SNAPSHOT-5

- feat: 用 WorkingDialogPane 动画条替换 Toast 提示
- fix: 动画条裁剪在轨道范围内
- fix: AprilFools 启动按钮隐藏功能在新布局中重新启用
- chore: 版权邮箱地址更新

## DEV2026.1.0-SNAPSHOT-4 及更早

- feat: SCL RSA 签名密钥对，官方构建验证
- feat: 添加 DEV/V 前缀版本检测和稳定版本格式
- fix: 替换图标为粘液块
- SCL dev2026.1.0: Slime Craft Launcher 初始提交
