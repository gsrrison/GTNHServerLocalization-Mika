# GTNH Server Localization

GTNH 服务端语言本地化模组，让服务端侧生成的文本也能使用指定语言。

> Mod ID: `serverlocalization-gtnh`  
> Package: `site.gtnhserverlocalization`  
> 原项目：[`Unukalhai727/GTNHServerLocalization`](https://github.com/Unukalhai727/GTNHServerLocalization)

## 功能

- 在服务端启动时读取语言文件，并注入到 Minecraft 1.7.10 的 `StringTranslate` 翻译表。
- 支持读取 TxLoader 目录结构下的 `.lang` 文件。
- 支持读取 GregTech 导出的 `GregTech_{lang}.lang` 文件。
- 修复 GregTech/Detrav 电动探矿仪部分矿石名称从原始 `ItemStack` 取名导致未本地化的问题，例如显示 `nether quartz ore`。
- 仅服务端安装即可，客户端不需要安装本模组。

## 适用范围

这个模组只影响服务端侧通过 `StringTranslate` 生成的文本，例如部分聊天消息、命令反馈、死亡提示，以及服务端发送给客户端的部分模组文本。

它不会替代客户端资源包，也不会翻译客户端本地渲染的 GUI、物品提示、材质、图片或客户端专用文本。

## 安装

1. 下载 release 中的主 jar，例如 `serverlocalization-gtnh-v2026.06.30.jar`。
2. 将 jar 放入服务端 `mods/` 目录。
3. 启动一次服务端，生成配置文件。
4. 根据需要修改语言配置和放置语言文件。

## 配置

配置文件由 Forge 生成，通常位于：

```text
config/serverlocalization-gtnh.cfg
```

默认配置：

```cfg
general {
    S:lang=zh_CN
}
```

`lang` 用于指定要加载的语言代码，例如 `zh_CN`、`en_US`。

## 支持的语言文件

### TxLoader

启动服务端时会扫描以下路径：

```text
config/txloader/load/{modName}/lang/{lang}.lang
config/txloader/forceload/{modName}/lang/{lang}.lang
```

例如：

```text
config/txloader/forceload/GregTech/lang/zh_CN.lang
```

这些文件按 UTF-8 的 Java `Properties` 格式读取：

```properties
death.attack.hot=%1$s 被熔岩烧死了
tile.example.name=示例方块
```

### GregTech

还会在服务端运行目录根目录读取：

```text
GregTech_{lang}.lang
```

例如：

```text
GregTech_zh_CN.lang
```

该文件使用 Forge Configuration 格式，读取 `languagefile` 分类下的键值：

```cfg
languagefile {
    S:"death.attack.hot"=被熔岩烧死了
    S:"gt.blockmachines.123.name"=工业熔炉
}
```

## GregTech/Detrav 探矿仪修复

Detrav 的探矿结果包 `detrav.net.ProspectingPacket` 原本会直接根据 `Block + meta` 创建 `ItemStack` 并调用显示名。GregTech 矿石的真实材料信息不总是能从这个原始 `ItemStack` 名称正确体现，因此可能出现未本地化或错误的英文名称。

本模组作为 coremod 加载时，会对 `ProspectingPacket.addBlock(...)` 做 ASM 补丁，将显示名解析改为：

1. 通过 GregTech `OreManager.getOreInfo(block, meta)` 获取矿石信息。
2. 优先使用 GregTech 的本地化矿石名称。
3. 如果无法解析，再回退到原版 `ItemStack.getDisplayName()`。

这个补丁只修复探矿仪结果中的显示名称，不修改世界生成，也不会隐藏真实存在的矿石。

## 主要代码结构

| 文件 | 作用 |
| --- | --- |
| `GTNHServerLocalization.java` | Forge `@Mod` 入口，加载配置并在服务端启动时注入翻译 |
| `Config.java` | 读取 `general.lang` 配置 |
| `TranslationInjector.java` | 反射访问 `StringTranslate` 内部翻译表并写入翻译 |
| `TxLoaderTranslator.java` | 扫描并读取 TxLoader `.lang` 文件 |
| `GregTechTranslator.java` | 读取 `GregTech_{lang}.lang` |
| `ProspectionNameHelper.java` | 为探矿仪结果解析 GregTech 矿石显示名 |
| `asm/GTNHServerLocalizationCorePlugin.java` | coremod 入口 |
| `asm/ProspectingPacketTransformer.java` | ASM 补丁实现 |

## 构建

本项目使用 GTNH Gradle Convention 构建，已在 Java 25 下测试。

Windows:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.3'
.\gradlew.bat build
```

构建产物位于：

```text
build/libs/
```

## 本地参考文件

仓库中的 `reference/` 目录用于本地存放服务端文件、汉化文件或测试参考文件，已经加入 `.gitignore`，不会提交到仓库。
