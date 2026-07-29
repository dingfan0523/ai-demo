# Redis 核心模型与数据结构

## Redis 是什么

Redis 是内存优先的数据结构服务器。它常被用作缓存、快速键值数据库、消息组件、流处理组件、排行榜系统、分布式协调辅助组件、限流计数器、会话存储和向量检索组件。

面试回答不要只说“Redis 是 key-value 数据库”。更准确的回答是：

- Redis 的 key 是二进制安全字符串。
- Redis 的 value 不是单一字符串，而是一组带命令语义的数据结构。
- Redis 对单条命令和 Lua 脚本提供原子执行。
- Redis 默认以内存为主要存储介质，可通过 RDB、AOF 做持久化。
- Redis 通过复制、Sentinel、Cluster 解决读扩展、高可用和分片扩容问题。

## Redis 为什么快

常见原因：

- 数据主要在内存中，避免多数磁盘随机 I/O。
- 命令处理模型简单，核心命令在单线程事件循环中顺序执行，避免复杂锁竞争。
- 数据结构针对常见场景做了编码优化，例如小 Hash/List/Set 可用紧凑结构节省内存。
- 网络协议 RESP 简单，客户端实现成本低。
- 支持 Pipeline 批量发送命令，降低网络 RTT。
- 很多操作是 O(1) 或 O(logN)，并且命令语义直接贴近业务结构。

Redis 也会变慢：

- 大 key 读写、删除、迁移、序列化会阻塞事件循环。
- 慢命令如 `KEYS`、大范围 `SMEMBERS`、大范围 `ZRANGE` 会占用主线程。
- fork 生成 RDB/AOF rewrite 子进程时，大内存实例可能有明显停顿。
- 网络带宽、客户端连接数、输出缓冲区堆积可能成为瓶颈。
- 内存碎片、swap、透明大页、慢磁盘 fsync 会造成延迟尖刺。

## Key 设计

推荐命名：

- `业务域:对象类型:对象id:字段`
- 示例：`user:profile:10001`、`order:status:202607280001`、`cache:sku:12345`

Key 设计原则：

- 可读、稳定、长度适中。Key 太长会浪费内存，太短会降低可维护性。
- 需要批量运维时保留前缀，例如 `cache:sku:*`，但线上扫描要用 `SCAN`，不要用 `KEYS`。
- Cluster 中需要多 key 原子操作时，用 hash tag 固定同一 slot，例如 `cart:{user100}:items` 和 `cart:{user100}:meta`。
- 不要把动态随机字段放到 key 前缀最前面，否则不利于扫描和统计。

## String

String 是 Redis 最基础类型，value 是二进制安全字符串，最大长度受 Redis 限制和内存约束影响。它可以存文本、JSON 字符串、数字、位图、序列化对象。

常用命令：

- `SET key value EX seconds NX`
- `GET key`
- `MGET key1 key2`
- `INCR key`
- `INCRBY key n`
- `DECR key`
- `APPEND key value`
- `GETSET key value`

典型场景：

- 缓存单个对象的 JSON。
- 分布式锁基础命令：`SET lock value NX PX millis`。
- 计数器：浏览量、点赞数、接口调用次数。
- 限流窗口：`INCR` + `EXPIRE`。
- Bitmap 的底层载体。

隐藏坑：

- `SETNX` 后再 `EXPIRE` 不是原子操作，进程崩溃会留下死锁。应使用 `SET key value NX EX/PX`。
- 对象 JSON 过大容易形成大 key，建议拆 Hash 或拆多个 key。
- `MGET` 可以降低 RTT，但一次拿太多大 value 会造成网络和反序列化尖刺。
- `INCR` 只能作用于可解析为整数的字符串。

## Hash

Hash 是 field-value 映射，适合存对象的多个字段。它类似 Java 的 `Map<String, String>`。

常用命令：

- `HSET user:1 name Alice age 20`
- `HGET user:1 name`
- `HMGET user:1 name age`
- `HINCRBY user:1 score 1`
- `HDEL user:1 field`
- `HSCAN user:1 cursor`

典型场景：

- 用户资料、商品快照、配置项、购物车字段。
- 高频更新对象的局部字段，避免整段 JSON 反序列化和回写。
- feature store 或推荐特征存储。

隐藏坑：

- Redis 传统 TTL 是 key 级别，不是 field 级别。新版本支持 Hash field expiration 时也要确认客户端、服务端版本和命令兼容。
- Hash 字段过多仍然是大 key，`HGETALL` 会产生阻塞和网络峰值。
- 字段名本身也占内存，极小对象未必比 JSON String 更省。
- Java 序列化不统一时，Hash field/value 可能出现乱码或无法跨服务读取。

## List

List 是按插入顺序排列的字符串列表，常用于简单队列、栈、时间线片段。

常用命令：

- `LPUSH queue item`
- `RPOP queue`
- `BRPOP queue timeout`
- `LRANGE key start stop`
- `LTRIM key start stop`

典型场景：

- 简单任务队列。
- 最新 N 条动态：`LPUSH` 后 `LTRIM`。
- 阻塞消费：`BRPOP`。

隐藏坑：

- List 不适合需要可靠确认、消费组、重试和消息追踪的队列；这类场景优先 Stream 或专业 MQ。
- `LRANGE 0 -1` 对大列表危险。
- `BLPOP/BRPOP` 连接会阻塞，客户端连接池要隔离。

## Set

Set 是无序、唯一元素集合，常用于去重、关系集合、标签集合。

常用命令：

- `SADD key member`
- `SISMEMBER key member`
- `SREM key member`
- `SCARD key`
- `SINTER key1 key2`
- `SUNION key1 key2`
- `SDIFF key1 key2`
- `SSCAN key cursor`

典型场景：

- 用户标签、黑名单、点赞去重、抽奖候选。
- 共同好友、共同关注、差集推荐。

隐藏坑：

- 大集合的交并差可能很重，最好评估集合大小并控制返回量。
- `SMEMBERS` 一次返回全集，线上大 key 禁用。
- Cluster 中跨 slot 多 key 集合操作不可直接执行，需 hash tag 或应用层聚合。

## Sorted Set

Sorted Set 又叫 ZSet，是唯一 member 加 score 的有序集合，底层通常结合字典和跳表等结构，兼顾按 member 查询和按 score/rank 范围查询。

常用命令：

- `ZADD rank 100 user1`
- `ZSCORE rank user1`
- `ZINCRBY rank 1 user1`
- `ZRANGE rank 0 9 REV WITHSCORES`
- `ZRANGEBYSCORE delay -inf now LIMIT 0 100`
- `ZREM rank user1`

典型场景：

- 排行榜。
- 延迟队列，score 存执行时间戳。
- 时间线、Feed 索引。
- 滑动窗口限流。

隐藏坑：

- score 是浮点数，金额、强精度业务不要直接依赖浮点精度。
- 热门排行榜会成为热 key，需要分片榜单或本地缓存。
- 延迟队列用 ZSet 需要处理并发抢占、失败重试和可见性超时。

## Bitmap

Bitmap 不是独立 value 类型，而是基于 String 的位操作能力。

常用命令：

- `SETBIT sign:202607 userOffset 1`
- `GETBIT sign:202607 userOffset`
- `BITCOUNT sign:202607`
- `BITOP AND result key1 key2`

典型场景：

- 用户签到。
- 活跃用户统计。
- 布尔状态压缩。

隐藏坑：

- offset 过大时会导致 String 扩容到很大。
- 用户 id 不能直接当 offset，通常要映射为连续偏移。
- Bitmap 适合布尔集合，不适合保存复杂属性。

## Bitfield

Bitfield 可在 String 上按位段读写多个小整数，适合压缩多个计数器或状态位。

典型场景：

- 游戏属性、小范围状态、紧凑计数器。
- 多个小整数塞入一个 key，减少 key 数量。

隐藏坑：

- 可读性差，字段布局必须文档化。
- 溢出策略要明确，否则结果可能被截断或报错。

## HyperLogLog

HyperLogLog 用于近似基数统计，适合 UV 这种不要求精确用户集合的场景。

常用命令：

- `PFADD uv:20260728 user1`
- `PFCOUNT uv:20260728`
- `PFMERGE uv:month uv:day1 uv:day2`

典型场景：

- 日活 UV、页面 UV、搜索词 UV。

隐藏坑：

- 结果是近似值，不可用于强精确计费、权限、库存。
- 只能估算数量，不能列出具体成员。

## Geo

Geo 用于地理位置索引，本质上和 Sorted Set 编码有关。

常用命令：

- `GEOADD shop:geo longitude latitude shopId`
- `GEOSEARCH shop:geo FROMLONLAT lon lat BYRADIUS 3 km WITHDIST`

典型场景：

- 附近门店、附近司机、附近设备。

隐藏坑：

- 地球模型和精度有限，不适合严肃 GIS。
- 热门城市或热门区域会形成热点。
- 位置信息有隐私风险，TTL、脱敏和权限要设计。

## Stream

Stream 是追加日志结构，支持消息 ID、消费组、消费者、Pending List 和确认机制。

常用命令：

- `XADD stream * field value`
- `XREAD COUNT 10 STREAMS stream $`
- `XGROUP CREATE stream group $ MKSTREAM`
- `XREADGROUP GROUP group consumer COUNT 10 STREAMS stream >`
- `XACK stream group id`
- `XPENDING stream group`
- `XCLAIM stream group consumer min-idle-time id`

典型场景：

- 轻量消息队列。
- 事件流。
- 异步任务。
- 审计日志。

隐藏坑：

- Stream 不是 Kafka，容量、回溯、分区、生态和长保留能力不同。
- Consumer Group 必须处理 Pending 消息，否则会堆积。
- `MAXLEN ~` 是近似裁剪，不保证精确长度。
- 消息处理要幂等，因为重试和转移消费者会导致重复消费。

## Pub/Sub

Pub/Sub 是发布订阅，不保存历史消息。

典型场景：

- 在线通知。
- 配置刷新。
- 非关键广播。

隐藏坑：

- 订阅者离线会丢消息。
- 不提供消费确认、重放、持久化。
- 关键业务消息不要只用 Pub/Sub。

## JSON、Probabilistic、Time Series、Vector Set

Redis 生态包含更多数据结构或模块能力：

- JSON：存层级结构对象，可按路径更新和查询。
- Bloom/Cuckoo Filter：高效判断元素是否可能存在，适合缓存穿透防护。
- Count-Min Sketch、Top-K、t-digest：近似统计。
- Time Series：时间序列数据。
- Vector Set：向量相似度检索，适合推荐、语义搜索、RAG 场景。
- Array：新版本提供的稀疏、可按下标访问的序列结构，使用前要确认 Redis 版本。

工程原则：

- 如果只是普通缓存，核心五大结构足够。
- 如果要做近似统计、向量检索、时间序列，先确认 Redis Open Source、Redis Stack、Redis Software/Cloud 的版本和命令支持。
- 面试中可以把这些能力作为加分项，但不要把模块能力和所有 Redis 部署默认能力混为一谈。

## 数据结构选型速查

- 单值缓存：String。
- 对象字段频繁局部更新：Hash。
- 简单队列、最新列表：List。
- 去重、共同关系：Set。
- 排序、排行榜、延迟任务：ZSet。
- 布尔签到、活跃状态：Bitmap。
- UV 近似统计：HyperLogLog。
- 附近位置：Geo。
- 可靠一点的 Redis 内部消息流：Stream。
- 只广播在线消息：Pub/Sub。
- 防缓存穿透：Bloom Filter 或本地/Redis 空值缓存。

## 参考资料

- https://redis.io/docs/latest/get-started/
- https://redis.io/docs/latest/develop/data-types/
- https://redis.io/docs/latest/commands/
- https://redis.io/docs/latest/develop/reference/protocol-spec/
