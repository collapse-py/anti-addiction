#本文是AI寫的#

# 防沉迷（Anti-Addiction）

一个专为 Paper（Minecraft 服务端）平台设计的防沉迷插件。它追踪玩家的在线时长，当超过配置的时长限制时自动踢出，并在冷却期间阻止玩家重新加入，冷却结束后清空时长，让玩家可以重新游玩。

## 项目简介

此插件主要面向可能需要控制玩家在线的 Minecraft 服务器运营者，通过系统化的方式管理玩家在线时长，防止过度游戏带来的沉迷风险。

### 核心运行原理

1. **Tick 级别追踪**：每个游戏 Tick（1/20 秒）对所有在线且不在白名单中的玩家累计在线时长。
2. **自动踢出**：当累积 tick 数达到设定限制时，自动执行 `/kick`，并同时写入冷却时间。
3. **冷却期**：玩家被踢出后进入冷却期，在此期间尝试连接会被拦截，直到冷却结束。
4. **时长重置**：冷却结束后，该玩家的累计 tick 被清零，恢复正常游戏权限。

---

## 功能特性

| 功能 | 说明 |
|---|---|
| **在线时长追踪** | 每个 tick 对在线玩家的游戏时间进行累计。 |
| **时长限制踢出** | 达到 `playtime-limit` 配置的时长后自动踢出玩家。 |
| **冷却系统** | 被踢出后进入冷却期，冷却期间玩家再次加入会被提示剩余时间。 |
| **白名单豁免** | 列入白名单的玩家完全不受此插件影响。 |
| **Sideboard 实时显示** | 向每个玩家展示当前时长、时长上限、冷却状态和插件状态。 |
| **MySQL 持久化** | 玩家数据（在线时长、冷却剩余、白名单状态）存储在 MySQL 数据库中，服务器重启后不丢失。 |
| **Redis 缓存** | 利用 Redis 作为高性能的内存缓存层，降低 MySQL 的压力。 |
| **游戏内管理命令** | 提供 `/antiaddiction` 系列 Brigadier 命令，支持热重载、设置、开关和白名单管理。 |

---

## 技术栈

| 类别 | 详细说明 |
|---|---|
| **开发语言** | Java 25 |
| **构建工具** | Gradle（附带 `com.gradleup.shadow` 插件 v8.3.5 用于打包依赖） |
| **目标平台** | Paper 26.1.2（基于 Minecraft 服务端 API） |
| **数据库** | MySQL Connector/J 8.0.33 |
| **缓存** | Redis（Jedis 客户端）5.1.0 |
| **命令框架** | Paper Brigadier（`io.papermc.paper.command.brigadier`） |
| **打包方式** | Shadow Jar（fat jar；所有依赖直接打包进插件 JAR） |
| **代码生成** | MCreator 生成的基础框架 |

### 为什么选择 Paper 26.1.2？

本项目基于 Paper API 26.1.2 进行开发，使用了该版本引入的 Brigadier 命令系统来构建更加规范、可读性强的斜杠命令。

---

## 插件元数据

| 字段 | 值 |
|---|---|
| **插件名称 (Name)** | `anti-addiction` |
| **版本 (Version)** | `1.0.0` |
| **主类 (Main)** | `net.collapse.antiaddiction.AntiaddictionMod` |
| **启动器类 (Bootstrap)** | `net.collapse.antiaddiction.AntiaddictionModBootstrap` |
| **API 版本** | `26.1` |
| **作者** | `collapse` |

### 声明的斜杠命令

- `antiaddiction` — 插件的主命令入口，包含以下子命令结构

---

## 架构详解

```
src/main/java/net/collapse/antiaddiction/
├── AntiaddictionMod.java              # 插件主入口（JavaPlugin 子类）
├── AntiaddictionModBootstrap.java     # 启动引导类，负责插件初始化流程
├── config/
│   └── PluginConfig.java              # 配置管理类，负责加载、保存配置
├── task/
│   ├── PlaytimeAccumulatorTask.java   # 核心任务：每个 tick 对在线玩家累计游戏时长
│   └── PeriodicSaveTask.java          # 定时任务：定期将所有临时数据写入 MySQL & Redis
├── storage/
│   ├── PlayerData.java                # 玩家数据模型（UUID、名称、tick 时长、冷却时间、白名单状态）
│   └── MySQLPlayerDataProvider.java   # MySQL 数据持久化提供者，负责玩家的加载和保存
├── cache/
│   └── RedisCache.java                # Redis 缓存层，提升热数据读取性能
├── listener/
│   └── PlayerSessionListener.java     # 玩家加入/退出监听器，负责数据加载、踢人和缓存更新
├── scoreboard/
│   └── ScoreboardManager.java         # 计分板管理器，为每个玩家创建自定义 Sidebar 显示
├── commands/
│   └── AntiAddiction.java             # Brigadier 命令注册与执行逻辑
└── util/
    └── TimeParser.java                # 时间字符串解析工具，支持 "1h 30m"、"10s"、"2t" 等格式
```

### 核心任务逻辑

#### `PlaytimeAccumulatorTask`（时长累加任务）

- 负责在 Minecraft 中每一个游戏 tick 被触发。
- 遍历所有在线玩家，忽略白名单内玩家。
- 若玩家处于冷却期内，跳过；否则 `+1 tick`。
- 当玩家 tick 数达到 `playtime-limit` 时：
  - 设置冷却时间戳
  - 调用 `Player.kickPlayer("§c要休息")` 踢出
  - 立即持久化数据（MySQL + Redis）

#### `PlayerSessionListener`（玩家会话监听器）

- **`PlayerJoinEvent`**：玩家加入时：
  1. 优先从 Redis 加载数据，再尝试从 MySQL 加载。
  2. 若处于冷却期：计算剩余时间，如果未过期则踢出并提示冷却剩余时间；如果已过期则清空 tick 并告知运营者。
  3. 若 tick 已超限：立即进入冷却并踢出。
  4. 将玩家数据放入内存 session map 并同步到 Redis。
- **`PlayerQuitEvent`**：玩家退出时：
  1. 将当前数据持久化到 MySQL & Redis。
  2. 清除玩家的计分板。

#### 数据持久化策略

使用的是**双层存储策略**：
1. **内存层**：每个在线的玩家都维护一个 `PlayerData` 对象，所有计数操作用内存完成，性能极高。
2. **Redis 层**（热数据缓存）：玩家数据在加入和退出时同步到 Redis，玩家重连时优先从 Redis 读取。
3. **MySQL 层**（持久层）：通过 `PeriodicSaveTask` 每隔 `persistence-interval` 秒将所有内存数据写入 MySQL，作为最终权威数据源。

---

## 安装与配置

### 前置要求

- **JDK 25**（用于编译和运行）
- **运行环境**：Paper 26.1.2+ 或 Bukkit API 26.1+ 兼容的 Minecraft 服务端

### 安装步骤

1. 将编译好的 `plugin.jar`（即 `build/libs/plugin.jar`）复制到服务器的 `plugins/` 目录。
2. 重启服务端（或使用 `/reload`，但推荐直接重启）。
3. 插件会在 `plugins/anti-addiction/config.yml` 自动生成配置文件。
4. 根据需求修改配置。
5. 在游戏内使用 `/antiaddiction` 命令进行管理。

---

## 配置说明

配置文件路径：`plugins/anti-addiction/config.yml`

### 时间单位说明

支持多种时间格式：

| 格式示例 | 说明 |
|---|---|
| `144000` | 原始 tick 数（1 tick = 50ms） |
| `2h` | 2 小时 |
| `1h 30m` | 1 小时 30 分钟 |
| `30m` | 30 分钟 |
| `10s` | 10 秒 |
| `1d` | 1 天 |
| `1m` | 1 月（视为 30 天） |
| `1y` | 1 年（视为 365 天） |

时间格式被 `TimeParser` 解析后转换为 tick 数（20 ticks = 1 秒）。

### 主配置

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `antiaddiction-enabled` | boolean | `true` | 插件的总开关。关闭后插件不执行任何功能，但数据和命令仍可用。 |
| `playtime-limit` | 时间 / tick 数 | `144000`（约 2 小时） | 单个玩家累计的在线总时长限制。 |
| `cooldown-duration` | 时间 / tick 数 | `36000`（约 30 分钟） | 玩家超限后被踢出后需等待的冷却时间。 |
| `persistence-interval` | int（秒） | `60` | 定时保存任务执行的间隔秒数。 |

### MySQL 配置

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `mysql.enabled` | boolean | `false` | 是否启用 MySQL 持久化。 |
| `mysql.host` | String | `localhost` | MySQL 服务器地址。 |
| `mysql.port` | int | `3306` | MySQL 服务端口。 |
| `mysql.database` | String | `antiaddiction` | 数据库名称。 |
| `mysql.username` | String | `root` | 数据库用户名。 |
| `mysql.password` | String | `''` | 数据库密码。 |

> **注意**：启用 MySQL 前，需自行在 MySQL 服务器中创建 `anti_addiction_players` 数据表，结构请参考 `MySQLPlayerDataProvider.java` 中的建表语句。

### Redis 配置

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `redis.enabled` | boolean | `false` | 是否启用 Redis 缓存。 |
| `redis.host` | String | `localhost` | Redis 服务器地址。 |
| `redis.port` | int | `6379` | Redis 服务端口。 |

> Redis 作为纯内存缓存层，仅存储当前在线的玩家数据，服务重启后自动从 MySQL 恢复。

### 计分板配置

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `scoreboard.enabled` | boolean | `true` | 是否向玩家展示自定义计分板。 |
| `scoreboard.title` | String | `&6Anti-Addiction` | 计分板标题，支持 `&` 颜色代码。 |
| `scoreboard.refresh-interval` | int（tick） | `20` | 计分板的刷新频率（20tick = 1秒）。 |

计分板向玩家实时展示：
- **Playtime** — 当前在线时长 / 时长上限（秒）
- **Cooldown** — 冷却状态（剩余时间 / 已过期 / 未触发）
- **Status** — 插件当前开启/关闭状态
- **Whitelisted** — 若玩家在白名单中则显示

### 白名单配置

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `whitelist` | List\<String\> | `[]` | 玩家名称列表。列在其中的玩家将被完全豁免此插件的时长限制。 |

---

## 命令手册

所有命令通过 `/antiaddiction` 斜杠命令入口调用，**仅限 OP 使用**。

### 命令树结构

```
/antiaddiction
├── reload                          — 重新加载配置
├── set
│   ├── x <时间>                    — 设置在线时长限制
│   └── y <时间>                    — 设置冷却时长
├── toggle
│   ├── mysql <on|off>             — 启用/禁用 MySQL
│   ├── redis <on|off>             — 启用/禁用 Redis
│   └── antiaddiction <on|off>     — 启用/禁用整个插件
└── whitelist
    ├── add <玩家名>               — 添加玩家到白名单
    ├── remove <玩家名>            — 从白名单移除玩家
    └── list                       — 列出所有白名单玩家
```

### 命令详解

#### `/antiaddiction reload`

重新读取配置文件，刷新系统内存中缓存的配置值。

用法：
```
/antiaddiction reload
```

响应：
- `§aConfiguration reloaded.` — 成功

#### `/antiaddiction set x <时间>`

设置游戏时长上限。

用法：
```
/antiaddiction set x 2h
/antiaddiction set x 7200s
/antiaddiction set x 144000
```

响应：
- `§aPlaytime limit set to 2h (144000 ticks)` — 成功
- `§cInvalid time format: ...` — 时间格式非法

#### `/antiaddiction set y <时间>`

设置冷却时长。

用法：
```
/antiaddiction set y 30m
/antiaddiction set y 1800s
```

响应：
- `§aCooldown duration set to 30m (1800 ticks)` — 成功

#### `/antiaddiction toggle mysql <on|off>`

动态启用或关闭 MySQL 持久化。

用法：
```
/antiaddiction toggle mysql on
/antiaddiction toggle mysql off
```

响应：
- `§amysql enabled` / `§amysql disabled` — 成功

#### `/antiaddiction toggle redis <on|off>`

动态启用或关闭 Redis 缓存。

用法：
```
/antiaddiction toggle redis on
/antiaddiction toggle redis off
```

响应：
- `§aredis enabled` / `§aredis disabled` — 成功

#### `/antiaddiction toggle antiaddiction <on|off>`

临时开关插件的自动踢人逻辑。关闭后仍可正常读取配置，但不再自动踢出玩家和累计 tick。

用法：
```
/antiaddiction toggle antiaddiction off
/antiaddiction toggle antiaddiction on
```

响应：
- `§aantiaddiction enabled` / `§aantiaddiction disabled` — 成功

#### `/antiaddiction whitelist add <玩家名>`

将指定玩家添加到白名单。对已经在线且是该玩家名的对象会立即生效。

用法：
```
/antiaddiction whitelist add Steve
```

响应：
- `§aAdded Steve to whitelist.` — 成功
- `§eSteve is already whitelisted.` — 已存在

#### `/antiaddiction whitelist remove <玩家名>`

从白名单中移除指定玩家。

用法：
```
/antiaddiction whitelist remove Steve
```

响应：
- `§aRemoved Steve from whitelist.` — 成功
- `§eSteve is not whitelisted.` — 不在白名单

#### `/antiaddiction whitelist list`

列出所有已在白名单中的玩家昵称。

响应示例：
```
§aWhitelisted players:
§7- Steve
§7- Alex
```

---

## 数据模型

### PlayerData

每个玩家由以下字段建模：

| 字段 | 类型 | 说明 |
|---|---|---|
| `uuid` | UUID | 玩家唯一标识符 |
| `name` | String | 玩家当前昵称（以便白名单可以使用名称匹配）|
| `playtimeTicks` | long | 本次在线累计的 tick 数 |
| `cooldownUntil` | long | Unix 时间戳（毫秒级），0 表示无冷却 |
| `whitelisted` | boolean | 是否在白名单中 |

> `playtimeTicks` 在玩家每次上线后初始化为 0，在冷却期间不会累计，仅在时序上每次 tick `+1`。

---

## 构建与打包

### 使用 Gradle 构建 Shadow JAR

```bash
# Windows 使用
gradlew.bat shadowJar

# Linux/macOS 使用
./gradlew shadowJar
```

构建产物：

- `build/libs/plugin.jar` — 包含全部依赖的 Fat JAR，拖入 `plugins/` 即可使用。

> 构建产物中不进行依赖重定位（`relocate`），会将 MySQL Connector 和 Jedis 的原生类直接打包，因此 JAR 体积较大。

### 本地直接运行 Paper 测试服务器

项目提供了 `runPaperServer` 任务，它会自动下载 Paper 并启动服务端：

```bash
# Windows
gradlew.bat runPaperServer

# Linux/macOS
./gradlew runPaperServer
```

该任务会完成以下步骤：
1. 创建 `run/tools`、`run/plugins` 目录。
2. 下载 `paper-26.1.2-74.jar` 至 `run/paper.jar`（本地缓存）。
3. 执行 `java -jar paper.jar --rev 26.1.2` 初始化服务端文件。
4. 将构建后的 `plugin.jar` 复制进 `run/plugins/`。
5. 启动 `run/` 下的 Paper 服务端。

---

## 注意事项

1. **MySQL 需要手动建表**：在启用 `mysql.enabled: true` 之前，请在 MySQL 中手动创建 `anti_addiction_players` 数据表（参考 `MySQLPlayerDataProvider` 代码中的 SQL）。
2. **依赖未重定位**：`shadowJar` 配置中将原生依赖（MySQL、Redis）直接打包，无包名重写，因此无需额外配置 classloader。
3. **Java 25 工具链**：由于 Paper API 26.1.2 对编译环境有要求，本项目必须使用 Java 25 进行编译。确保本地 JDK 版本为 25。
4. **Redis 可关机运行**：Redis 仅作为前端缓存，即使禁用或不可用，插件仍可依赖 MySQL 正常工作。
5. **服务端 `/reload` 风险**：部分纪念板（Scoreboard）数据在 `/reload` 后可能残留，建议通过重启服务端来彻底刷新插件状态。

---

## 路线图 / 可扩展方向

- [ ] 自动创建数据库表
- [ ] 多维度支持（如按年龄、按时间分段限制）
- [ ] 多语言支持（中/英切换）
- [ ] Web 管理面板
- [ ] 支持 SQLite（无独立数据库需求时）

---

## License

MIT
