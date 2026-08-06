# Contributing Guide

<!-- #BEGIN LANGUAGE_SWITCHER -->
**English** | 中文 ([简体](Contributing_zh.md), [繁體](Contributing_zh_Hant.md))
<!-- #END LANGUAGE_SWITCHER -->

## Build SCL

### Requirements

To build the SCL launcher, you need to install JDK 17 (or higher). You can download it here: [Download Liberica JDK](https://bell-sw.com/pages/downloads/#jdk-25-lts).

After installing the JDK, make sure the `JAVA_HOME` environment variable points to the required JDK directory.
You can check the JDK version that `JAVA_HOME` points to like this:

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

### Get SCL Source Code

- You can get the latest source code via [Git](https://git-scm.com/downloads):
  ```shell
  git clone https://github.com/lively-Studio/Slime-Craft-Launcher.git
  cd SCL
  ```
- You can manually download a specific version of the source code from the [GitHub Release page](https://github.com/lively-Studio/Slime-Craft-Launcher/releases).

### Build SCL

To build SCL, switch to the root directory of the SCL project and run the following command:

```shell
./gradlew clean :SCL:shadowJar
```

The built SCL program files are located in the `SCL/build/libs` subdirectory under the project root.

## Debug Options

> [!WARNING]
> This document describes SCL's internal features, which we do not guarantee to be stable and may be modified or removed at any time.
>
> Please use these features with caution, as improper use may cause SCL to behave abnormally or even crash.

SCL provides a series of debug options to control the behavior of the launcher.

These options can be specified via environment variables or JVM parameters. If both are present, JVM parameters will override the environment variable settings.

| Environment Variable        | JVM Parameter                                | Function                                                  | Default Value                                                                                               | Additional Notes          |
|-----------------------------|----------------------------------------------|-----------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|---------------------------|
| `SCL_JAVA_HOME`            |                                              | Specifies the Java used to launch SCL                    |                                                                                                             | Only effective for exe/sh |
| `SCL_JAVA_OPTS`            |                                              | Specifies the default JVM parameters when launching SCL  |                                                                                                             | Only effective for exe/sh |
| `SCL_FORCE_GPU`            |                                              | Specifies whether to force GPU-accelerated rendering      | `false`                                                                                                     |                           |
| `SCL_ANIMATION_FRAME_RATE` |                                              | Specifies the animation frame rate of SCL                | `60`                                                                                                        |                           |
| `SCL_LANGUAGE`             |                                              | Specifies the default language of SCL                    | Uses the system default language                                                                            |                           |
| `SCL_UI_SCALE`             |                                              | Specifies the UI scaling for SCL                         | Uses the system's current scaling                                                                           | Supports scale factor (1.5), percentage (150%), or DPI (144dpi).                          |
|                             | `-Dscl.dir=<path>`                          | Specifies the current data folder of SCL                 | `./.scl`                                                                                                   |                           |
|                             | `-Dscl.home=<path>`                         | Specifies the user data folder of SCL                    | Windows: `%APPDATA%\.scl`<br>Linux/BSD: `$XDG_DATA_HOME/scl`<br>macOS: `~Library/Application Support/scl` |                           |
|                             | `-Dscl.self_integrity_check.disable=true`   | Disables self-integrity checks during updates             |                                                                                                             |                           |
|                             | `-Dscl.bmclapi.override=<url>`              | Specifies the API Root for BMCLAPI                        | `https://bmclapi2.bangbang93.com`                                                                           |                           |
|                             | `-Dscl.discoapi.override=<url>`             | Specifies the API Root for foojay Disco API               | `https://api.foojay.io/disco/v3.0`                                                                          |                           | 
| `SCL_FONT`                 | `-Dscl.font.override=<font family>`         | Specifies the default font for SCL                       | Uses the system default font                                                                                |                           |
|                             | `-Dscl.update_source.override=<url>`        | Specifies the update source for SCL                      | `https://github.com/lively-Studio/Slime-Craft-Launcher/api/update_link`                                                               |                           |
|                             | `-Dscl.authlibinjector.location=<path>`     | Specifies the location of the authlib-injector JAR file   | Uses the built-in authlib-injector                                                                          |                           |
|                             | `-Dscl.openjfx.repo=<maven repository url>` | Adds a custom Maven repository for downloading OpenJFX    |                                                                                                             |                           |
|                             | `-Dscl.native.encoding=<encoding>`          | Specifies the native encoding                             | Uses the system's native encoding                                                                           |                           |
|                             | `-Dscl.microsoft.auth.id=<App ID>`          | Specifies the Microsoft OAuth App ID                      | Uses the built-in Microsoft OAuth App ID                                                                    |                           |
|                             | `-Dscl.curseforge.apikey=<Api Key>`         | Specifies the CurseForge API key                          | Uses the built-in CurseForge API key                                                                        |                           |
|                             | `-Dscl.native.backend=<auto/jna/none>`      | Specifies the native backend used by SCL                 | `auto`                                                                                                      |                           |
|                             | `-Dscl.hardware.fastfetch=<true/false>`     | Specifies whether to use fastfetch for hardware detection | `true`                                                                                                      |                           |
