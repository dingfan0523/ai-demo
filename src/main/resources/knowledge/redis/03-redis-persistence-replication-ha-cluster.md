# Redis 持久化、复制、高可用与集群

## 持久化总览

Redis 提供多种持久化选择：

- RDB：按时间点生成快照。
- AOF：追加记录写命令，重启时重放。
- RDB + AOF：同时启用，兼顾备份和更高数据安全。
- 无持久化：纯缓存场景可以关闭。

面试回答要强调：持久化提升数据恢复能力，但不等于强一致事务数据库。不同 fsync 策略、宕机时机、复制延迟都会影响数据安全。

## RDB

RDB 是某一时刻的数据快照，文件紧凑，适合备份、灾备、冷启动。

触发方式：

- 配置 `save seconds changes`。
- 手动 `SAVE`，同步阻塞，不推荐线上随意使用。
- 手动 `BGSAVE`，fork 子进程后台生成。
- 主从全量同步时生成 RDB。

优点：

- 文件小，便于备份和传输。
- 恢复速度通常比 AOF 快。
- 父进程主要 fork 子进程，持久化 I/O 由子进程处理。

缺点：

- 两次快照之间的数据可能丢失。
- 大实例 fork 可能阻塞。
- fork 后写入会触发 copy-on-write，内存峰值增加。

## AOF

AOF 记录每个会修改数据集的写命令，重启时重放恢复数据。

常见配置：

- `appendonly yes`
- `appendfsync always`：每次写都 fsync，最安全但最慢。
- `appendfsync everysec`：通常默认选择，最多丢约 1 秒数据。
- `appendfsync no`：交给操作系统刷盘，性能最好但风险最高。

AOF rewrite：

- AOF 会随着写入增长。
- 重写会生成能恢复当前状态的最短命令集合。
- Redis 7 起使用 multi-part AOF：base file、incremental file、manifest。
- 重写期间仍可继续处理写入。

隐藏坑：

- AOF 不是越安全越好，`always` 可能显著增加延迟。
- AOF rewrite 也需要 fork 和 copy-on-write。
- RDB 和 AOF 同时启用时，重启通常使用更完整的 AOF 恢复。
- 从 RDB 切到 AOF 要在运行中正确启用并持久化配置，直接改配置重启可能丢数据。

## fork 与 copy-on-write

RDB 和 AOF rewrite 都依赖 fork 子进程。

关键点：

- fork 当下父子进程共享物理内存页。
- 父进程继续写数据时，被修改的页会复制一份。
- 写入越多，COW 额外内存越大。
- 大实例 fork 本身也可能造成毫秒到秒级停顿。

生产建议：

- Redis maxmemory 不要顶满物理内存。
- 给 COW、复制 backlog、客户端缓冲区留余量。
- 监控 `latest_fork_usec`。
- 避免在业务高峰执行 rewrite、全量同步、大规模删除。

## 复制 Replication

Redis 复制是主从模式，replica 会尝试成为 master 的副本。

用途：

- 读扩展。
- 数据冗余。
- Sentinel/Cluster 故障转移基础。
- 分摊备份或分析查询。

复制流程简化：

- replica 连接 master。
- 首次通常全量同步，master 生成 RDB 给 replica。
- 后续 master 持续发送写命令流。
- 断线重连时，如果 replication backlog 还保留需要的数据，可部分重同步。

关键概念：

- `replication id`
- `offset`
- `replication backlog`
- `PSYNC`

隐藏坑：

- Redis 复制默认异步，master 写成功不代表 replica 已收到。
- 读写分离会遇到主从延迟，刚写后读 replica 可能读旧数据。
- master 宕机前未复制的数据可能丢失。
- replica 过多会增加 master 复制压力。
- 全量同步会带来 fork、网络和磁盘压力。

## 主从延迟

常见原因：

- master 写入量大。
- replica CPU 慢或网络慢。
- replica 正在加载 RDB。
- 大 key 写入导致复制流巨大。
- 客户端在 replica 上执行慢查询。

治理：

- 监控复制 offset 差距。
- 关键读走 master 或使用读己之写策略。
- 按业务区分强一致读和最终一致读。
- 控制大 key 和写入峰值。

## Sentinel

Sentinel 用于 Redis 主从架构的高可用管理。

能力：

- 监控 master 和 replica。
- 主观下线、客观下线判断。
- 自动选举和故障转移。
- 通知客户端新 master 地址。

关键点：

- Sentinel 自身需要多个节点，通常 3 个或以上。
- quorum 用于判断客观下线。
- 故障转移后，旧 master 恢复通常会变成 replica。

隐藏坑：

- Sentinel 不是数据分片方案。
- Sentinel 不能消除异步复制导致的数据丢失窗口。
- 客户端必须支持 Sentinel，不能写死单一 Redis 地址。
- 网络分区可能造成脑裂，需要合理配置 `min-replicas-to-write` 等保护。

## Redis Cluster

Redis Cluster 解决水平分片和高可用。

核心概念：

- 集群有 16384 个 hash slots。
- 每个 key 通过 CRC16 计算 slot。
- master 节点负责一部分 slots。
- replica 复制 master，用于故障转移。
- 客户端收到 `MOVED` 或 `ASK` 重定向后访问正确节点。

hash tag：

- `{}` 中的内容用于计算 slot。
- `order:{1001}:base` 和 `order:{1001}:items` 会落到同一 slot。
- 多 key 命令、事务、Lua 涉及多个 key 时，需要同 slot。

Cluster 优点：

- 横向扩容容量和吞吐。
- 自动分片。
- master 故障可由 replica 接管。

Cluster 限制：

- 跨 slot 多 key 操作受限。
- 批量操作要按 slot 拆分。
- 客户端必须支持 Cluster 拓扑刷新和重定向。
- 迁移 slots 时可能出现 ASK/MOVED。
- 热 key 或热 slot 仍可能打爆单节点。

## Cluster 故障转移

基本过程：

- 节点间通过 gossip 探测状态。
- master 被判定 fail 后，其 replica 参与选举。
- 获胜 replica 接管 slots。
- 集群更新拓扑，客户端刷新路由。

隐藏坑：

- 故障转移需要多数 master 可达，否则集群可能不可用。
- 异步复制仍可能丢最后一段写入。
- 副本优先级、复制偏移、新旧程度会影响选举。
- 应用端要处理短暂异常、重试和幂等。

## 读写分离

读 replica 可以提高读吞吐，但要接受最终一致。

适合：

- 读多写少。
- 允许读旧数据。
- 报表、非关键展示、排行榜快照。

不适合：

- 刚写后必须立即读到。
- 库存、余额、权限等强一致路径。

工程实践：

- 明确哪些查询可以读从。
- 写后短时间读 master。
- 暴露主从延迟指标。
- 失败重试要考虑读到旧数据的语义。

## 高可用不等于强一致

面试要回答清楚：

- RDB/AOF 解决重启恢复，不解决多副本同步一致性。
- Replication 解决副本复制，但默认异步。
- Sentinel 解决主从自动切换，但切换窗口可能丢数据。
- Cluster 解决分片扩容和节点级故障转移，但跨 slot 原子性受限。
- 如果业务必须强一致，核心约束通常仍要落数据库、共识系统或事务系统。

## 参考资料

- https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/
- https://redis.io/docs/latest/operate/oss_and_stack/management/replication/
- https://redis.io/docs/latest/operate/oss_and_stack/management/sentinel/
- https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/
