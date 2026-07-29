# Redis 命令语义、事务、Lua、Pipeline、Pub/Sub 与 Stream

## 命令原子性

Redis 单条命令在核心执行线程中是原子的。一个命令执行过程中不会被另一个客户端命令插入。

注意：

- 原子性不等于事务隔离级别丰富。
- 多条命令组合不天然原子。
- 如果读取、判断、写入分成多条命令，可能存在并发竞态。

示例：

- `INCR key` 原子。
- `GET key` 后应用判断再 `SET key` 不是原子。
- `SET key value NX PX 30000` 是单条原子加锁命令。
- 比较锁 owner 后删除锁需要 Lua 保证原子。

## Pipeline

Pipeline 是客户端一次发送多条命令，减少网络往返时间。

适合：

- 批量写入。
- 批量读取小对象。
- 初始化缓存。
- 降低 RTT 对吞吐的影响。

不保证：

- Pipeline 不等于事务。
- Pipeline 中间可能有其他客户端命令执行。
- Pipeline 不自动回滚。

隐藏坑：

- Pipeline 太大可能撑爆客户端或服务端输出缓冲区。
- 集群模式下 Pipeline 要按 slot/节点拆分。
- 大 value 批量返回会造成网络尖刺。

## 事务 MULTI/EXEC

Redis 事务围绕 `MULTI`、`EXEC`、`DISCARD`、`WATCH`。

语义：

- `MULTI` 后命令进入队列。
- `EXEC` 后按顺序执行队列中命令。
- 执行期间不会被其他客户端命令插入。
- `DISCARD` 放弃队列。
- `WATCH` 提供乐观锁 CAS。

重要面试点：

- Redis 事务没有传统关系型数据库的回滚机制。
- 入队前语法错误会导致事务不能执行。
- `EXEC` 后某条命令运行错误，其他命令仍会执行。
- 事务保证队列顺序执行和隔离插入，不保证复杂 ACID。

## WATCH 乐观锁

`WATCH key` 监控 key，如果 `EXEC` 前被其他客户端修改，则事务失败。

典型流程：

1. `WATCH balance:user1`
2. `GET balance:user1`
3. 应用计算新值
4. `MULTI`
5. `SET balance:user1 newValue`
6. `EXEC`

如果 `EXEC` 返回空，说明被修改，需要重试。

隐藏坑：

- 高冲突场景下重试成本高。
- WATCH 只适合简单 CAS，不适合复杂长事务。
- Cluster 中多 key WATCH 仍受同 slot 限制。

## Lua 脚本

Lua 脚本在 Redis 服务端执行，可以把多条命令和判断逻辑打包为原子操作。

适合：

- 比较 owner 后释放分布式锁。
- 库存扣减。
- 滑动窗口限流。
- 延迟队列抢占。
- 幂等去重加状态更新。

优势：

- 减少网络往返。
- 多命令原子。
- 逻辑贴近数据。

隐藏坑：

- Lua 执行期间阻塞 Redis 主线程。
- 脚本必须短小、可控，不要做大循环和慢操作。
- 脚本涉及 key 时，Cluster 要保证 key 在同一 slot。
- 生产中要用 `EVALSHA` 或 Redis Functions 管理脚本版本。
- 不要在脚本里执行不确定、不可重复或长时间阻塞的逻辑。

## Redis Functions

Redis Functions 是比传统 Lua 脚本更工程化的服务端函数管理方式。它允许把函数加载到 Redis 后按名称调用。

适合：

- 需要版本化、复用、统一管理的服务端逻辑。
- 多业务共享的原子操作。

注意：

- 仍然要遵守短小、可控、避免阻塞主线程的原则。
- 需要确认服务端版本和运维发布流程。

## 分布式锁

单实例基础加锁：

```text
SET lock:order:1 uniqueToken NX PX 30000
```

安全释放锁：

```lua
if redis.call("GET", KEYS[1]) == ARGV[1] then
  return redis.call("DEL", KEYS[1])
else
  return 0
end
```

必须回答的坑：

- 不能用 `SETNX` 后再 `EXPIRE`，两条命令之间崩溃会死锁。
- 不能直接 `DEL lock`，可能删除别人后来加的锁。
- 锁过期时间不能太短，否则业务没执行完锁已过期。
- 锁续期需要 watchdog 或显式续期，但续期也要考虑进程停顿。
- Redis 锁更适合效率锁，不适合唯一正确性约束。库存、余额等最终约束要在数据库层兜底。
- Redlock 有争议，面试中要说明适用边界：它比单实例更抗某些故障，但不能替代共识系统。

## Pub/Sub

Pub/Sub 是即时发布订阅。

特点：

- 发布者发消息到 channel。
- 在线订阅者收到消息。
- Redis 不保存历史消息。
- 没有消费确认和重试。

适合：

- 配置变更通知。
- 本地缓存失效广播。
- 在线状态广播。

不适合：

- 订单支付、库存扣减等关键可靠消息。
- 需要离线重放和消费确认的任务。

## Keyspace Notifications

Keyspace Notifications 可以发布 key 变化事件，例如过期、删除、修改。

适合：

- 调试。
- 非关键监听。
- 缓存失效通知辅助。

隐藏坑：

- 需要配置开启。
- 不是可靠消息系统。
- 过期事件触发时间不等于 key 到期时间。
- 消费者离线会丢事件。

## Stream

Stream 是 Redis 的追加日志结构，适合比 Pub/Sub 更可靠的轻量消息流。

核心概念：

- stream key：消息流。
- entry id：消息 ID，通常形如 `ms-seq`。
- consumer group：消费组。
- consumer：组内消费者。
- PEL：Pending Entries List，已投递未确认消息。

常用命令：

- `XADD mystream * type order_created orderId 1`
- `XREAD BLOCK 5000 STREAMS mystream $`
- `XGROUP CREATE mystream group1 $ MKSTREAM`
- `XREADGROUP GROUP group1 c1 COUNT 10 BLOCK 5000 STREAMS mystream >`
- `XACK mystream group1 id`
- `XPENDING mystream group1`
- `XAUTOCLAIM mystream group1 c2 60000 0-0`

典型流程：

1. 生产者 `XADD`。
2. 消费者组 `XREADGROUP`。
3. 业务处理成功后 `XACK`。
4. 对超时 pending 消息做 claim 和重试。

隐藏坑：

- 消息可能重复投递，消费者必须幂等。
- 只 `XREADGROUP` 不 `XACK` 会造成 PEL 堆积。
- Stream 长度要治理，否则内存持续增长。
- `MAXLEN ~` 是近似裁剪，适合性能优先。
- Redis Stream 不等于 Kafka，分区模型、磁盘保留和生态能力不同。

## SCAN

`SCAN` 用于渐进式遍历 key，替代线上危险的 `KEYS`。

特点：

- 游标式。
- 每次返回一部分。
- 可能返回重复元素。
- 遍历期间新增删除 key 时结果不保证强一致。

工程建议：

- 客户端要处理重复。
- 配合 `MATCH` 和 `COUNT`。
- 大规模删除时分批 `SCAN` + `UNLINK`。

## 慢命令红线

线上谨慎使用：

- `KEYS *`
- 大 key 的 `HGETALL`
- 大 key 的 `SMEMBERS`
- 大范围 `ZRANGE`
- 大集合交并差
- 复杂 Lua 脚本
- `MONITOR`
- 同步 `SAVE`

替代：

- `SCAN/HSCAN/SSCAN/ZSCAN`
- 分页范围查询
- 分片 key
- 异步删除 `UNLINK`
- 离线任务或副本执行分析

## 参考资料

- https://redis.io/docs/latest/develop/using-commands/transactions/
- https://redis.io/docs/latest/develop/using-commands/pipelining/
- https://redis.io/docs/latest/develop/programmability/eval-intro/
- https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/
- https://redis.io/docs/latest/develop/pubsub/keyspace-notifications/
