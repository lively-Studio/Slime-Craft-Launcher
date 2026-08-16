# SCL 项目长期记忆

## 项目信息
- **项目名称**: Slime Craft Launcher (SCL, 原名 HMCL)
- **仓库**: lively-Studio/Slime-Craft-Launcher (GPLv3)
- **技术栈**: Java 17 + JavaFX + JFoenix (Material Design) + Gradle 8.x (Kotlin DSL)
- **模块**: SCLBoot (Java 8 引导) / SCLCore (纯逻辑核心) / SCL (JavaFX UI)

## 架构约定
1. 所有类必须标注 `@NotNullByDefault`（但已有多文件未标注，新增文件应标注）
2. 使用 `///` Markdown 风格 Javadoc
3. 窗口装饰: `StageStyle.TRANSPARENT` + 自绘 `Decorator`
4. 导航: `Navigator` 堆栈导航 + `DecoratorAnimatedPage`
5. 页面接口: `DecoratorPage` (提供 title/backable/refreshable 状态)
6. 事件系统: `EventBus.EVENT_BUS` 全局单例
7. UI 组件库: `studio.lively.scl.ui.construct` (AdvancedListItem, AdvancedListBox, TabHeader 等)
8. 图标: `studio.lively.scl.ui.SVG` 枚举 (Material Symbols)
9. 国际化: `studio.lively.scl.util.i18n.I18n`

## 关键路径
- SCL 用户主目录: `Metadata.SCL_USER_HOME` (由 scl.home 系统属性或环境变量决定)
- SCL 本地目录: `Metadata.SCL_LOCAL_HOME` (`.scl/` 或由环境变量决定)
- 插件目录: `$SCL_USER_HOME/plugins/`

## UI 主题：NOTHING-UI-2 设计令牌（2026-08-14 起）
SCL 的 JavaFX 主题使用自研 NUI2 设计令牌，dark-first。
- 令牌前缀 `-nothing-*`，定义在 `SCL/src/main/resources/assets/css/nothing-ui.css`（暗色优先：20 层表面 / 玻璃 / 20 级阴影 / 10 色强调 / 排版 / 圆角 / 间距 / 状态色 / 文字层级 / 轮廓）。
- 加载链：`StyleSheets.java`（setGlobalStylesheets）依次加载 nothing-ui.css → accent 覆盖（MonetFX 动态改 `-nothing-accent-*`）→ brightness-dark.css → brightness-light.css。
- 浅色模式仅做"兼容"：brightness-light.css 重映射表面/文字/轮廓/动态令牌；NUI2 设计理念是暗色唯一，浅色非设计目标。
- 第三天（2026-08-15）补全了浅色模式：此前漏覆盖 `-nothing-text-secondary/tertiary`、`-nothing-surface-two`、`-nothing-surface-5`，导致浅色下文字/对话框不可见。
- 注意：`com.jfoenix` 是捆绑第三方控件库，内部硬编码色（JFXDepthManager/JFXRippler）不在自研主题范围内，未改。
- `ThemeColor` 用户可选主题色板（含默认蓝 #5C6BC0）保留为产品特性，未强制改为 NUI2 cyan。

## 2026-08-10 添加的拓展/插件系统
实现了 Issue #1 要求的拓展功能系统:

### SCLCore 模块 (10 个文件)
- `studio.lively.scl.plugin.Plugin` - 插件生命周期接口 (onLoad/onEnable/onDisable)
- `studio.lively.scl.plugin.PluginDescriptor` - 插件描述符 (从 plugin.json 加载)
- `studio.lively.scl.plugin.PluginState` - 插件状态枚举 (DISCOVERED/LOADED/ENABLED/DISABLED/ERROR)
- `studio.lively.scl.plugin.PluginClassLoader` - 隔离类加载器 (每个插件独立)
- `studio.lively.scl.plugin.PluginManager` - 核心管理器 (发现/加载/启用/禁用/重载)
- `studio.lively.scl.plugin.ExtensionPoint` - 扩展点注解
- `studio.lively.scl.plugin.PluginPageProvider` - 自定义页面扩展点
- `studio.lively.scl.plugin.PluginDownloadSourceProvider` - 自定义下载源扩展点
- `studio.lively.scl.plugin.PluginThemeProvider` - 自定义主题扩展点
- `studio.lively.scl.plugin.PluginStateChangeEvent` - 插件状态变更事件

### SCL 模块修改
- `studio.lively.scl.ui.plugin.PluginListPage` - 插件管理页面 (列表 + 扩展功能展示)
- `Controllers.java` - 添加 getPluginListPage()、cleanup
- `RootPage.java` - 导航栏添加 EXTENSION 图标入口
- `Launcher.java` - 启动时初始化 PluginManager + 停止时 shutdown
- `I18N.properties` / `I18N_zh_CN.properties` - 添加 plugin.* 翻译 key
