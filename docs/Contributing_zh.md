# 贡献指南

<!-- #BEGIN LANGUAGE_SWITCHER -->
[English](Contributing.md) | **中文** (**简体**, [繁體](Contributing_zh_Hant.md))
<!-- #END LANGUAGE_SWITCHER -->

## 构建 SCL

### 环境需求

构建 SCL 启动器需要安装 JDK 17 (或更高版本)。你可以从此处下载它: [Download Liberica JDK](https://bell-sw.com/pages/downloads/#jdk-25-lts)。

在安装 JDK 后，请确保 `JAVA_HOME` 环境变量指向符合需求的 JDK 目录。
你可以这样查看 `JAVA_HOME` 指向的 JDK 版本:

<details>
<summary>Windows</summary>

PowerShell:
```
PS > & "$env:JAVA_HOME/bin/java.exe" -version
openjdk version "25" 2025-09-16 LTS
OpenJDK Runtime Environment (build 25+37-LTS)
OpenJDK 64-Bit Server VM (build 25+37-LTS, mixed mode, sharing)
```

</details>

<details>
<summary>Linux/FreeBSD</summary>

```
> $JAVA_HOME/bin/java -version
openjdk version "25" 2025-09-16 LTS
OpenJDK Runtime Environment (build 25+37-LTS)
OpenJDK 64-Bit Server VM (build 25+37-LTS, mixed mode, sharing)
```

</details>

<details>
<summary>macOS</summary>

```
> /usr/libexec/java_home --exec java -version
openjdk version "25" 2025-09-16 LTS
OpenJDK Runtime Environment (build 25+37-LTS)
OpenJDK 64-Bit Server VM (build 25+37-LTS, mixed mode, sharing)
```

</details>

### 获取 SCL 源码

- 通过 [Git](https://git-scm.com/downloads) 可以获取最新源码:
  ```shell
  git clone https://github.com/lively-Studio/Slime-Craft-Launcher.git
  cd SCL
  ```
- 从 [GitHub Release 页面](https://github.com/lively-Studio/Slime-Craft-Launcher/releases)可以手动下载特定版本的源码。

### 构建 SCL

想要构建 SCL，请切换到 SCL 项目的根目录下，并执行以下命令:

```shell
./gradlew clean :SCL:shadowJar
```

构建出的 SCL 程序文件位于根目录下的 `SCL/build/libs` 子目录中。

## 调试选项

> [!WARNING]
> 本文介绍的是 SCL 的内部功能，我们不保证这些功能的稳定性，并且随时可能修改或删除这些功能。
>
> 使用这些功能时请务必小心，错误地使用这些功能可能会导致 SCL 行为异常甚至崩溃。

SCL 提供了一系列调试选项，用于控制启动器的行为。

这些选项可以通过环境变量或 JVM 参数指定。如果两者同时存在，那么 JVM 参数会覆盖环境变量的设置。

| 环境变量                        | JVM 参数                                       | 功能                             | 默认值                                                                                                         | 额外说明         |
|-----------------------------|----------------------------------------------|--------------------------------|-------------------------------------------------------------------------------------------------------------|--------------|
| `SCL_JAVA_HOME`            |                                              | 指定用于启动 SCL 的 Java             |                                                                                                             | 仅对 exe/sh 生效 |
| `SCL_JAVA_OPTS`            |                                              | 指定启动 SCL 时的默认 JVM 参数          |                                                                                                             | 仅对 exe/sh 生效 |
| `SCL_FORCE_GPU`            |                                              | 指定是否强制使用 GPU 加速渲染              | `false`                                                                                                     |
| `SCL_ANIMATION_FRAME_RATE` |                                              | 指定 SCL 的动画帧率                  | `60`                                                                                                        |              |
| `SCL_LANGUAGE`             |                                              | 指定 SCL 的默认语言                  | 使用系统默认语言                                                                                                    |
| `SCL_UI_SCALE`             |                                              | 指定 SCL 的 UI 缩放比例                 | 遵循系统当前的缩放比例                                                                                       | 支持倍数 (1.5)、百分比 (150%) 或 DPI (144dpi) |
|                             | `-Dscl.dir=<path>`                          | 指定 SCL 的当前数据文件夹               | `./.scl`                                                                                                   |              |
|                             | `-Dscl.home=<path>`                         | 指定 SCL 的用户数据文件夹               | Windows: `%APPDATA%\.scl`<br>Linux/BSD: `$XDG_DATA_HOME/scl`<br>macOS: `~Library/Application Support/scl` |              |
|                             | `-Dscl.self_integrity_check.disable=true`   | 检查更新时不检查本体完整性                  |                                                                                                             |              |
|                             | `-Dscl.bmclapi.override=<url>`              | 指定 BMCLAPI 的 API Root          | `https://bmclapi2.bangbang93.com`                                                                           |              |
|                             | `-Dscl.discoapi.override=<url>`             | 指定 foojay Disco API 的 API Root | `https://api.foojay.io/disco/v3.0`                                                                          |
| `SCL_FONT`                 | `-Dscl.font.override=<font family>`         | 指定 SCL 默认字体                   | 使用系统默认字体                                                                                                    |              |
|                             | `-Dscl.update_source.override=<url>`        | 指定 SCL 更新源                    | `https://github.com/lively-Studio/Slime-Craft-Launcher/api/update_link`                                                               |              |
|                             | `-Dscl.authlibinjector.location=<path>`     | 指定 authlib-injector JAR 文件的位置  | 使用 SCL 内嵌的 authlib-injector                                                                                |              |
|                             | `-Dscl.openjfx.repo=<maven repository url>` | 添加用于下载 OpenJFX 的自定义 Maven 仓库   |                                                                                                             |              |
|                             | `-Dscl.native.encoding=<encoding>`          | 指定原生编码                         | 使用系统的本机编码                                                                                                   |              |
|                             | `-Dscl.microsoft.auth.id=<App ID>`          | 指定 Microsoft OAuth App ID      | 使用 SCL 内置的 Microsoft OAuth App ID                                                                          |              |
|                             | `-Dscl.curseforge.apikey=<Api Key>`         | 指定 CurseForge API 密钥           | 使用 SCL 内置的 CurseForge API 密钥                                                                               |              |
|                             | `-Dscl.native.backend=<auto/jna/none>`      | 指定SCL使用的本机后端                  | `auto`                                                                                                      |
|                             | `-Dscl.hardware.fastfetch=<true/false>`     | 指定是否使用 fastfetch 检测硬件信息        | `true`                                                                                                      |

