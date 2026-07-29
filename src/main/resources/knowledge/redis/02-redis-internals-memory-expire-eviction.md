# Redis 底层实现、内存、过期与淘汰

## 单线程模型

Redis 常说的“单线程”主要指核心命令执行在单线程事件循环中完成。这样做的好处是避免命令执行阶段的锁竞争，让数据结构操作保持简单。Redis 仍然会使用后台线程或子进程处理一些任务，例如关闭文件、AOF fsync、惰性释放、RDB/AOF rewrite 子进程等。

面试表达：

- Redis 快不是因为“完全没有线程”，而是因为核心命令路径避免了复杂多线程锁竞争。
- 单线程意味着慢命令、大 key 操作、脚本长时间执行会阻塞其他客户端。
- Redis 6 以后支持 I/O threads 优化网络读写，但命令执行语义仍要按单线程原子模型理解。

## 事件循环

Redis 使用事件驱动模型处理客户端连接、命令读写、定时任务和文件事件。

关键点：

- 客户端 socket 可读时读取命令。
- 解析 RESP 协议并执行命令。
- 把响应写入客户端输出缓冲区。
- 周期性执行 serverCron，例如过期检查、统计、复制维护。

常见延迟来源：

- 单次命令执行时间过长。
- 输出缓冲区堆积。
- 客户端读取慢。
- fork、fsync、内存分配或释放成本高。

## RESP 协议

RESP 是 Redis 序列化协议。客户端通过简单文本/二进制友好的格式发送命令并接收响应。

工程意义：

- 协议简单使客户端生态丰富。
- Pipeline 本质上是连续发送多个 RESP 命令，不等待每条响应。
- AOF 也使用 Redis 协议格式记录写命令，因此可读性和恢复性较好。

## Redis 对象模型

Redis value 有逻辑类型和底层编码两个层面。

逻辑类型：

- string
- list
- hash
- set
- zset
- stream

底层编码可能随数据规模变化：

- String 可使用整数编码、embstr、raw。
- Hash 小对象可用 listpack，变大后转 hashtable。
- List 使用 quicklist。
- Set 小整数集合可用 intset，变复杂后转 hashtable。
- ZSet 小集合可用 listpack，变大后通常使用 skiplist 加 dict。

面试要点：

- 同一个 Redis 类型，不等于固定一种底层结构。
- Redis 会为了内存效率在小数据结构上使用紧凑编码。
- 数据变大、元素变复杂后会转换编码，转换可能产生一次性成本。

## SDS

SDS 是 Redis 的简单动态字符串实现，解决 C 字符串不记录长度、扩容不方便、二进制不安全等问题。

优势：

- O(1) 获取长度。
- 预留空间减少频繁扩容。
- 二进制安全，可以存任意字节。
- 避免缓冲区溢出风险。

面试回答：

- Redis key 和 String value 都可以看作二进制安全字节序列。
- 不要假设 value 一定是 UTF-8 文本。

## dict 与渐进式 rehash

Redis 的 keyspace、Hash 编码、Set 编码都会大量使用哈希表。

渐进式 rehash：

- 哈希表扩容或缩容时，不会一次性搬迁所有 bucket。
- Redis 通过后续命令逐步迁移，降低单次阻塞。
- rehash 期间可能同时维护新旧两个表，查询需要查两边。

隐藏坑：

- 渐进式 rehash 降低尖刺，但不是完全没有成本。
- 大量 key 新增或删除时，CPU 和内存波动会变明显。

## listpack、quicklist、skiplist

listpack：

- 紧凑连续内存结构。
- 适合小 Hash、小 ZSet。
- 节省指针和对象开销。
- 插入删除可能涉及内存移动。

quicklist：

- Redis List 的核心结构。
- 可理解为多个紧凑 listpack 节点串起来。
- 在内存效率和两端操作性能之间折中。

skiplist：

- ZSet 大集合常用结构之一。
- 支持按 score 排序和范围查询。
- 平均 O(logN) 插入、删除、查找。

面试追问：

- 为什么 ZSet 需要 dict 加 skiplist？
- dict 让 `ZSCORE member` 快速查 member 分数。
- skiplist 让 `ZRANGE`、`ZRANGEBYSCORE` 这类范围操作高效。

## 过期机制

Redis 过期删除不是每个 key 到期立即删除，而是组合策略：

- 惰性删除：访问 key 时发现已过期再删除。
- 定期删除：后台周期性抽样检查过期字典。

这样设计是 CPU 和内存之间的折中：

- 只惰性删除会导致冷 key 过期后长期占内存。
- 只定时精确删除会带来大量定时器和 CPU 开销。
- 抽样定期删除能在成本和及时性之间平衡。

隐藏坑：

- 过期 key 不保证毫秒级准时消失。
- 大量 key 同时过期会造成 CPU 尖刺和缓存雪崩。
- 过期事件通知不是可靠消息，不要当业务强依赖队列。

## TTL 语义

常用命令：

- `EXPIRE key seconds`
- `PEXPIRE key millis`
- `TTL key`
- `PTTL key`
- `PERSIST key`
- `SET key value EX seconds`

注意：

- 覆盖 key 的命令可能清除原 TTL，具体要看命令语义。
- `TTL = -1` 表示 key 存在但没有过期时间。
- `TTL = -2` 表示 key 不存在。
- 分布式缓存 TTL 要加随机抖动，避免同时失效。

## 内存淘汰 maxmemory

当 Redis 设置 `maxmemory` 后，达到上限时会按 `maxmemory-policy` 淘汰 key 或拒绝写入。

常见策略：

- `noeviction`：不淘汰，写入报错。适合不能丢缓存或当数据库使用的场景。
- `allkeys-lru`：所有 key 近似 LRU 淘汰。
- `volatile-lru`：只在设置了过期时间的 key 中近似 LRU 淘汰。
- `allkeys-lfu`：所有 key 近似 LFU 淘汰。
- `volatile-lfu`：只在带 TTL 的 key 中近似 LFU 淘汰。
- `allkeys-random`：所有 key 随机淘汰。
- `volatile-random`：带 TTL 的 key 随机淘汰。
- `volatile-ttl`：带 TTL 的 key 中优先淘汰剩余 TTL 短的。

工程选择：

- 纯缓存：常用 `allkeys-lru` 或 `allkeys-lfu`。
- 只有部分 key 允许淘汰：使用 `volatile-*`，但要保证可淘汰 key 都设置 TTL。
- Redis 当准数据库：谨慎使用淘汰，通常 `noeviction` 并配合容量告警。

隐藏坑：

- Redis 的 LRU/LFU 是近似算法，不是全局精确。
- 淘汰发生在写入路径，会增加写延迟。
- 如果使用 `volatile-lru` 但很多 key 没 TTL，可能无法释放足够内存，最终写失败。

## 大 key

大 key 指 value 特别大，或集合元素特别多的 key。

危害：

- 读写阻塞主线程。
- 网络传输慢。
- 删除释放内存慢。
- Cluster 迁移慢。
- 复制和持久化压力变大。

治理：

- 拆分 key，例如 `user:feed:{uid}:page:1`。
- 限制集合长度，例如 List 配合 `LTRIM`。
- 使用 `SCAN`、`HSCAN`、`SSCAN`、`ZSCAN` 分批处理。
- 删除大 key 使用 `UNLINK` 或后台异步释放能力。
- 建立大 key 巡检。

## 热 key

热 key 指访问频率极高的 key。

危害：

- 单节点 CPU 或网络打满。
- Cluster 下一个 slot/节点成为瓶颈。
- 缓存击穿时数据库被打爆。

治理：

- 本地缓存加短 TTL。
- 热点 key 分片，例如 `hot:sku:123:{0..N}`。
- 读副本扩展，但要接受读延迟。
- 对热点重建加互斥锁或逻辑过期。
- 对访问入口做限流、降级、请求合并。

## 内存碎片

内存碎片表示 Redis 实际占用 RSS 明显大于有效数据内存。

常看指标：

- `used_memory`
- `used_memory_rss`
- `mem_fragmentation_ratio`

成因：

- 大量不同大小对象频繁创建删除。
- allocator 不能及时归还内存给 OS。
- 大 key 删除和重建。

治理：

- 打开主动碎片整理需评估 CPU 成本。
- 规范 key 大小和 TTL。
- 避免频繁写入超大对象。
- 必要时迁移、重启、扩容。

## swap 与 THP

Redis 依赖内存低延迟，swap 会造成灾难性延迟。透明大页 THP 也可能放大 fork 和内存延迟。

生产建议：

- 禁止或严格控制 swap。
- 关闭 THP。
- 预留内存给 fork copy-on-write、复制缓冲、客户端输出缓冲。
- 容器部署时明确 memory limit 和 Redis maxmemory 的关系。

## 参考资料

- https://redis.io/docs/latest/develop/reference/protocol-spec/
- https://redis.io/docs/latest/develop/reference/eviction/
- https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/memory-optimization/
- https://redis.io/docs/latest/commands/expire/
