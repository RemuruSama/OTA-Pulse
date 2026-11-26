# OTA Pulse

OTA Pulse 是一款 Android 应用，用于发现、下载和检查 `OnePlus`、`Realme`、`OPPO` 机型的 OTA 升级包。本仓库还包含一个静态落地页：[index.html](https://remurusama.github.io/OTA-Pulse/)。

> 从 BBK 服务器请求并下载 OTA 升级包。

## 应用功能

- 从内置 JSON 设备目录浏览支持的机型（涵盖 OnePlus、Realme、OPPO，共 148+ 条设备定义）
- 收藏常用机型，方便快速访问
- 获取并查看 OTA 更新元数据，提供丰富的详情与分区提取选项
- 下载 OTA 升级包并显示进度通知（自研 OkHttp 下载引擎，支持暂停 / 续传）
- 使用内置全面屏浏览器检查 OTA 链接
- 高级工具：
  - 手动查询
  - 分区提取
  - 链接解析
  - ARB 检查器
- 导入 / 导出自定义设备定义与设置
- 添加自定义设备定义
- OTA 查询历史记录
- 首页更新发现流程，可识别硬件型号与 SoC
- 检查 OTA Pulse 应用更新，支持后台更新检查与开机自启
- 25+ 种区域语言翻译
- 高可靠性：健壮的错误处理、无泄漏的流、线程安全的数据持久化，以及低端硬件保护

## 技术栈

- Kotlin（Android）
- Jetpack Compose（100% Compose UI，采用 Material Design 3 与 Material Kolor 动态主题）
- minSdk 29 • targetSdk 37 • compileSdk 37
- Coroutines 与 Flow（响应式状态管理）
- Hilt（依赖注入）
- WorkManager（与 Hilt 集成）
- Room 数据库（本地持久化）
- OkHttp（网络请求 + 支持续传的自研下载引擎）
- Markwon、Gson、Flexbox、Brotli
- Protobuf Lite
- XZ / Apache Commons Compress
- AndroidX Navigation Compose、Lifecycle Compose、Dynamic Animation

## 项目结构

```text
Otaupdater/
├─ app/
│  ├─ build.gradle.kts
│  └─ src/main/
│     ├─ java/com/abhinav/otapulse/
│     │  ├─ app/             # 应用外壳、MainActivity、MainViewModel、OtaPulseApp（Compose 根节点）
│     │  ├─ arb/             # 防回滚与提取安全
│     │  ├─ catalog/         # 设备目录逻辑与用户数据持久化
│     │  │  ├─ model/        # 共享模型（PredefinedDevice、Region 等）
│     │  │  └─ repository/   # JSON 解析、收藏、自定义设备
│     │  ├─ core/            # 共享基础设施
│     │  │  ├─ common/       # 工具类、加密、权限、映射器、OTA 卡片与分享辅助
│     │  │  ├─ database/     # Room 数据库配置与类型转换器
│     │  │  ├─ download/     # 自研 OkHttp 下载引擎与前台服务
│     │  │  ├─ model/        # 共享领域模型
│     │  │  ├─ network/      # 网络基础组件、OTA 解析器、GitHub 更新器、应用下载器
│     │  │  ├─ notifications/ # 下载通知辅助
│     │  │  ├─ preferences/  # 应用与主题设置 datastore 及偏好
│     │  │  ├─ receiver/     # 开机与下载动作接收器
│     │  │  ├─ ui/           # 共享 Compose 设计系统组件与全息主题
│     │  │  └─ worker/       # 后台任务（下载、更新检查）
│     │  ├─ di/              # Hilt 依赖注入模块
│     │  ├─ feature/         # 面向用户的产品流程与 Compose 页面
│     │  │  ├─ about/        # 应用信息、社交主页、更新说明弹层
│     │  │  ├─ browser/      # 应用内 WebView 浏览器 Activity 与页面
│     │  │  ├─ devicecatalog/ # Compose 展示层（目录浏览、OTA 详情弹层、添加设备）
│     │  │  ├─ devices/      # 设备目录的领域用例与 ViewModel
│     │  │  ├─ downloads/    # 下载队列、文件生命周期、Compose 下载页
│     │  │  ├─ history/      # OTA 查询历史的记录与浏览
│     │  │  ├─ otatools/     # 手动查询、分区提取、链接解析、ARB 检查器页面
│     │  │  ├─ settings/     # 主题、浏览器偏好、ARB 开关、导入导出、开源库
│     │  │  └─ updates/      # 首页更新发现页面与状态
│     │  ├─ navigation/      # Compose Navigation 宿主（OtaPulseNavGraph）、页面路由与转场
│     │  └─ ota/             # OTA 引擎、payload、远程 ZIP、断点续传
│     └─ res/                # 图形资源、布局以及 25+ 种语言翻译
├─ OTAPulse/
│  ├─ logo/                  # 应用图标资源
│  └─ Screenshot/            # 应用商店 / README 截图
├─ devices/                  # JSON 设备目录（oneplus.json、oppo.json、realme.json）
├─ gradle/
│  └─ libs.versions.toml     # 版本目录
├─ architecture.md
├─ index.html                # 静态落地页
└─ settings.gradle.kts
```

更详细的包结构、职责划分与运行时流程请参阅 [architecture.md](architecture.md)。

## Android 功能模块

- `feature/devicecatalog` 与 `feature/devices` —— 设备浏览、地区 / 版本筛选、收藏、OTA 详情弹层、自定义设备添加
- `feature/downloads` —— 下载队列（自研 OkHttp 引擎）、暂停 / 续传流程、文件生命周期管理
- `feature/history` —— 基于 Room 的 OTA 查询历史记录与浏览
- `feature/updates` —— 首页更新发现卡片，展示设备代号与 SoC 详情
- `feature/otatools` —— 手动查询、分区提取、链接解析与 ARB 检查器
- `feature/browser` —— 全面屏应用内 WebView 浏览器
- `feature/settings` —— 全息与单色主题偏好、浏览器设置、ARB 开关、导入导出、开源库
- `feature/about` —— 项目信息、开发者主页、更新日志

## 环境配置

1. 在 Android Studio 中打开本项目。
2. 如需构建签名发布版，请在项目根目录创建 `keystore.properties`。
3. 添加：

```properties
STORE_PASSWORD=your_store_password
KEY_PASSWORD=your_key_password
```

4. 同步 Gradle。
5. 在 Android 10 及以上的设备或模拟器上运行 `app` 配置。

## 贡献者须知

- 设备定义请放在顶层的 `devices/` JSON 文件中，解析逻辑放在 `catalog/`
- 面向用户的页面与 UI 组件请放在 `feature`（若多个 feature 共用，则放在 `core/ui/components`）
- 导航定义与路由声明请放在 `navigation/`
- OTA 解析 / 提取逻辑请放在 `ota` 与 `arb`
- 只有当代码确实跨 feature 边界共用时，才移动到 `core`
- 下载引擎位于 `core/download` —— 下载基础设施请保留在此处
- 根目录的落地页是独立的 Web 资源，不属于 Android 运行时的一部分

## 致谢

- [R0rt1z2/realme-ota](https://github.com/R0rt1z2/realme-ota)

## 支持

> 如果这个项目对你有帮助，欢迎在 GitHub 上点个 **star ⭐** —— 这有助于提升项目的曝光度！
