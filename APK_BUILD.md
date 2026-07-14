# Android APK 打包教程

本文说明如何在 Windows 11 上使用 PowerShell，将本项目的患者 Web 端和医生 Web 端分别打包为可安装的 Android APK。

## 1. 打包方式说明

项目使用 Capacitor 8 将 Vue + Vite 的构建产物放入 Android WebView：

1. `vite build --mode android` 根据 `.env.android` 构建静态页面。
2. `cap sync android` 将 `dist/` 同步到原生 Android 工程。
3. Gradle 将 Android 工程编译成 APK。

APK 只包含前端页面，不包含 Spring Boot、MySQL、Redis 或 Chroma。手机运行 APK 时仍然需要访问已经部署的后端服务器。

患者端和医生端是两个独立应用，可以同时安装：

| 应用 | 前端目录 | 包名 | 当前服务器 |
|------|----------|------|------------|
| 患者端 | `web/` | `com.secp.hda.patient` | `http://8.137.115.167` |
| 医生端 | `doctor-web/` | `com.secp.hda.doctor` | `http://8.137.115.167:8081` |

## 2. 当前电脑的环境目录

这台电脑已经安装并使用以下环境：

| 环境 | 目录 |
|------|------|
| Node.js | `D:\Node.js` |
| JDK 21 | `D:\JDK\jdk-21` |
| Android SDK | `D:\AndroidSDK` |
| Gradle 缓存 | `D:\GradleCache` |
| 项目 | `D:\SECP\SECP` |

Capacitor 8 要求 Node.js 22 或更高版本。本项目 Android 工程使用 Android SDK 36、最低 Android SDK 24，也就是 Android 7.0 及以上。

打开一个新的 PowerShell，检查环境：

```powershell
node --version
java -version
& "D:\AndroidSDK\platform-tools\adb.exe" version
```

建议配置以下用户环境变量：

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "D:\JDK\jdk-21", "User")
[Environment]::SetEnvironmentVariable("ANDROID_HOME", "D:\AndroidSDK", "User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", "D:\AndroidSDK", "User")
[Environment]::SetEnvironmentVariable("GRADLE_USER_HOME", "D:\GradleCache", "User")
```

将 Android 命令加入用户 `Path` 后，需要关闭并重新打开 PowerShell：

```powershell
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
$items = @(
    "D:\AndroidSDK\platform-tools",
    "D:\AndroidSDK\cmdline-tools\latest\bin"
)
foreach ($item in $items) {
    if (($userPath -split ";") -notcontains $item) {
        $userPath = "$userPath;$item"
    }
}
[Environment]::SetEnvironmentVariable("Path", $userPath, "User")
```

## 3. 首次准备项目

患者端和医生端需要分别安装 npm 依赖：

```powershell
Set-Location D:\SECP\SECP\web
npm install

Set-Location D:\SECP\SECP\doctor-web
npm install
```

两个 Android 工程各自通过 `local.properties` 指定 SDK。该文件包含本机路径并已被 Git 忽略，换电脑后需要重新创建：

```powershell
Set-Content -LiteralPath "D:\SECP\SECP\web\android\local.properties" -Value "sdk.dir=D:/AndroidSDK" -Encoding ascii
Set-Content -LiteralPath "D:\SECP\SECP\doctor-web\android\local.properties" -Value "sdk.dir=D:/AndroidSDK" -Encoding ascii
```

如果 Android SDK 尚未安装完整，可以使用 `sdkmanager` 安装项目需要的组件并接受许可：

```powershell
& "D:\AndroidSDK\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools" "platforms;android-36" "build-tools;36.0.0"
& "D:\AndroidSDK\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
```

## 4. 配置服务器地址

Android 构建使用以下文件：

```text
web/.env.android
doctor-web/.env.android
```

当前配置为：

```dotenv
# web/.env.android
VITE_SERVER_ORIGIN=http://8.137.115.167
```

```dotenv
# doctor-web/.env.android
VITE_SERVER_ORIGIN=http://8.137.115.167:8081
```

以后服务器域名或端口变化时，只修改对应文件，然后重新执行同步和打包即可。地址末尾不要添加 `/api`，代码会自行拼接 API 路径。

`npm run dev` 不会读取 `.env.android`，因此本地 Web 开发仍使用 Vite 代理，不受 Android 地址影响。

## 5. 打包患者端 Debug APK

```powershell
Set-Location D:\SECP\SECP\web
npm run android:sync

Set-Location D:\SECP\SECP\web\android
$env:GRADLE_USER_HOME = "D:\GradleCache"
.\gradlew.bat assembleDebug
```

构建成功后，APK 位于：

```text
D:\SECP\SECP\web\android\app\build\outputs\apk\debug\app-debug.apk
```

## 6. 打包医生端 Debug APK

```powershell
Set-Location D:\SECP\SECP\doctor-web
npm run android:sync

Set-Location D:\SECP\SECP\doctor-web\android
$env:GRADLE_USER_HOME = "D:\GradleCache"
.\gradlew.bat assembleDebug
```

构建成功后，APK 位于：

```text
D:\SECP\SECP\doctor-web\android\app\build\outputs\apk\debug\app-debug.apk
```

## 7. 整理最终 APK

执行以下命令，将两个 APK 复制到统一输出目录并使用容易辨认的名称：

```powershell
$output = "D:\SECP\SECP\apk-output"
New-Item -ItemType Directory -Path $output -Force | Out-Null

Copy-Item -LiteralPath "D:\SECP\SECP\web\android\app\build\outputs\apk\debug\app-debug.apk" `
    -Destination "$output\HDA-Patient-Test.apk" -Force

Copy-Item -LiteralPath "D:\SECP\SECP\doctor-web\android\app\build\outputs\apk\debug\app-debug.apk" `
    -Destination "$output\HDA-Doctor-Test.apk" -Force
```

最终文件：

```text
D:\SECP\SECP\apk-output\HDA-Patient-Test.apk
D:\SECP\SECP\apk-output\HDA-Doctor-Test.apk
```

## 8. 安装到 Android 手机

### 方法一：直接发送文件

将 APK 发送到手机，打开文件并允许“安装未知应用”。患者端和医生端包名不同，可以同时安装。

### 方法二：使用 USB 和 adb

1. 在手机中开启开发者选项和 USB 调试。
2. 使用数据线连接电脑并在手机上允许调试。
3. 在 PowerShell 执行：

```powershell
adb devices
adb install -r "D:\SECP\SECP\apk-output\HDA-Patient-Test.apk"
adb install -r "D:\SECP\SECP\apk-output\HDA-Doctor-Test.apk"
```

`-r` 表示保留应用数据并覆盖安装相同包名的新版本。如果签名发生变化，需要先卸载旧版本再安装。

## 9. 日常修改后的重新打包

前端代码变化后，不需要重新创建 Android 工程。对相应端重复以下两步：

```powershell
npm run android:sync
Set-Location android
.\gradlew.bat assembleDebug
```

仅修改 `.env.android` 也必须先运行 `npm run android:sync`，否则 APK 中仍然是上一次同步的服务器地址。

## 10. Gradle 下载与离线配置

当前两个 Android 工程的 `gradle/wrapper/gradle-wrapper.properties` 使用这台电脑上的离线文件：

```properties
distributionUrl=file:///D:/GradleCache/downloads/gradle-8.14.3-bin.zip
```

在当前电脑上请保留：

```text
D:\GradleCache\downloads\gradle-8.14.3-bin.zip
```

团队成员在其他电脑构建时，可以选择以下任一方式：

1. 将相同版本的 Gradle 压缩包放到相同路径。
2. 将两个工程的 `distributionUrl` 改为官方地址：

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.3-bin.zip
```

使用官方地址时首次构建需要联网下载，后续会从 `GRADLE_USER_HOME` 缓存读取。

## 11. 正式签名 APK

`assembleDebug` 生成的是 Android Debug 签名 APK，适合课程演示和内部测试，不适合应用商店发布。

需要正式发布时，推荐使用 Android Studio：

1. 打开 `web/android` 或 `doctor-web/android`。
2. 选择 `Build > Generate Signed Bundle / APK`。
3. 选择 `APK`，创建或选择自己的 `.jks` 密钥库。
4. 选择 `release` 构建。
5. 妥善保存密钥库、别名和密码，后续更新必须使用同一个签名。

密钥库和密码不要提交到 Git。正式发布前还应配置 HTTPS、关闭不必要的明文流量，并根据应用商店要求调整版本号和隐私说明。

## 12. 常见问题

### `JAVA_HOME is not set`

当前 PowerShell 临时设置：

```powershell
$env:JAVA_HOME = "D:\JDK\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

### `SDK location not found`

检查对应 Android 工程下的 `local.properties` 是否存在，并确认内容为：

```properties
sdk.dir=D:/AndroidSDK
```

### Gradle 提示找不到本地 ZIP

确认 `D:\GradleCache\downloads\gradle-8.14.3-bin.zip` 存在，或者按照第 10 节改回官方 `distributionUrl`。

### Vite 构建出现 `spawn EPERM`

关闭正在占用项目文件的进程，重新打开 PowerShell后再执行。仍然失败时，可尝试用管理员 PowerShell运行一次构建，并检查安全软件是否拦截 `node_modules\esbuild`。

### 登录和普通接口正常，但实时咨询无法连接

这通常不是 APK 打包问题。服务器必须将 `/ws/doctor-consult` 按 WebSocket 协议反向代理到后端，并保留 `Upgrade` 和 `Connection` 请求头。当前测试服务器该路径仍可能返回前端 `index.html`，需要服务器负责人修正 Nginx 配置。

### 安装后仍然访问旧服务器

按以下顺序重新构建：修改 `.env.android`，运行 `npm run android:sync`，运行 `assembleDebug`，复制新 APK，最后在手机上覆盖安装。

### HTTP 接口在手机中被拦截

当前 `capacitor.config.json` 和 Android Manifest 已允许测试环境的 HTTP 明文流量。正式环境应部署 HTTPS，而不是长期依赖该设置。
