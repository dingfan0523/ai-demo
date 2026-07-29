# Redis 运维、可观测性与安全

## 生产配置原则

关键配置：

- `bind`
- `protected-mode`
- `port`
- `requirepass` 或 ACL
- `maxmemory`
- `maxmemory-policy`
- `appendonly`
- `appendfsync`
- `save`
- `client-output-buffer-limit`
- `timeout`
- `tcp-keepalive`
- `slowlog-log-slower-than`

原则：

- 开发环境可以默认配置，生产必须显式配置文件。
- Redis 不应暴露到公网。
- 容器 memory limit 要大于 Redis maxmemory，并预留 fork、缓冲区和碎片空间。
- 不要把 Redis 当无限内存用。

## INFO

`INFO` 返回 Redis 服务器统计信息。

常看 section：

- `INFO server`：版本、进程、运行时间。
- `INFO clients`：连接数、阻塞客户端。
- `INFO memory`：内存、RSS、碎片率。
- `INFO persistence`：RDB/AOF 状态、fork、rewrite。
- `INFO stats`：QPS、命中率、过期、淘汰。
- `INFO replication`：主从角色、offset、延迟。
- `INFO commandstats`：命令调用和耗时。
- `INFO cluster`：集群状态。

常用指标：

- `used_memory`
- `used_memory_rss`
- `mem_fragmentation_ratio`
- `connected_clients`
- `blocked_clients`
- `instantaneous_ops_per_sec`
- `keyspace_hits`
- `keyspace_misses`
- `evicted_keys`
- `expired_keys`
- `latest_fork_usec`
- `master_repl_offset`

## SLOWLOG

`SLOWLOG` 记录执行时间超过阈值的命令。

常用命令：

- `SLOWLOG GET 128`
- `SLOWLOG LEN`
- `SLOWLOG RESET`

注意：

- 慢日志统计的是命令执行时间，不包含网络传输和客户端排队时间。
- 发现慢命令后要结合 key 大小、调用方、命令频率分析。

## Latency Monitor

Latency Monitor 用于定位延迟事件。

常用：

- `LATENCY LATEST`
- `LATENCY DOCTOR`
- `LATENCY HISTORY event`

常见延迟事件：

- fork。
- AOF fsync。
- command。
- expire cycle。
- eviction cycle。

## MONITOR

`MONITOR` 会实时打印 Redis 收到的命令。

注意：

- 对性能影响明显。
- 高 QPS 生产环境慎用。
- 更适合短时间排查或低峰期。

替代：

- 客户端埋点。
- 慢日志。
- commandstats。
- 代理层日志。

## 客户端连接治理

问题：

- 连接数过多。
- idle 连接不释放。
- 阻塞命令占住连接。
- 输出缓冲区爆炸。
- 客户端超时重试风暴。

排查：

- `CLIENT LIST`
- `INFO clients`
- `blocked_clients`
- 连接池指标

治理：

- 设置连接池上限。
- 设置命令超时。
- 隔离阻塞连接。
- 对慢客户端限流或断开。
- 明确重试次数和退避策略。

## 内存治理

容量规划要包含：

- 数据内存。
- key 和对象元数据。
- 过期字典。
- replication backlog。
- 客户端输入/输出缓冲。
- AOF rewrite 缓冲。
- fork copy-on-write。
- allocator 碎片。

经验：

- 不要让 Redis maxmemory 等于机器总内存。
- 大实例 fork 成本更高。
- 集群分片要考虑热 key，而不仅是总容量平均。

## 持久化运维

RDB：

- 定期备份 RDB。
- 校验备份文件大小和摘要。
- 备份传输到异地。
- 关注 `rdb_last_bgsave_status`。

AOF：

- 关注 `aof_last_write_status`。
- 关注 `aof_rewrite_in_progress`。
- 配置合理 rewrite 阈值。
- 磁盘满会影响写入和恢复。

## 安全

Redis 安全原则：

- 不暴露公网。
- 开启 `protected-mode`。
- 绑定内网地址。
- 使用 ACL 用户和强密码。
- 启用 TLS，尤其跨主机、跨机房、云环境。
- 限制危险命令，例如 `FLUSHALL`、`CONFIG`、`EVAL`、`KEYS`，根据版本和策略重命名或通过 ACL 禁止。
- 日志、配置和备份不要泄露密码。

ACL：

- 可按用户控制命令、key pattern、channel。
- 应用使用最小权限用户。
- 运维用户和应用用户分离。

危险点：

- 无密码公网 Redis 会被攻击者写入恶意数据、清空数据或利用配置写文件。
- Java 原生反序列化缓存不可信数据会产生安全风险。
- Lua 和模块能力要控制权限。

## 线上排障清单

Redis 慢：

- 看 `SLOWLOG GET`。
- 看 `LATENCY DOCTOR`。
- 看 `INFO commandstats`。
- 看 CPU、网络、磁盘、swap。
- 查大 key、热 key。
- 查最近是否 BGSAVE/AOF rewrite。

内存涨：

- 看 key 数变化。
- 看大 key。
- 看 TTL 覆盖率。
- 看碎片率。
- 看客户端输出缓冲。
- 看是否有 Stream 未裁剪、Pending 堆积。

连接爆：

- 看 `CLIENT LIST`。
- 定位来源 IP 和 name。
- 检查连接池配置。
- 检查是否创建短连接。
- 检查阻塞命令。

缓存命中率低：

- 看 `keyspace_hits/misses`。
- 检查 TTL 是否太短。
- 检查 key 生成是否不稳定。
- 检查缓存穿透。
- 检查预热是否失败。

主从异常：

- 看 `INFO replication`。
- 对比 offset。
- 看 backlog 是否不足。
- 看网络延迟。
- 看 replica 是否加载 RDB。

## 参考资料

- https://redis.io/docs/latest/commands/info/
- https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency-monitor/
- https://redis.io/docs/latest/operate/oss_and_stack/management/security/
- https://redis.io/docs/latest/operate/oss_and_stack/management/config/
- https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/
