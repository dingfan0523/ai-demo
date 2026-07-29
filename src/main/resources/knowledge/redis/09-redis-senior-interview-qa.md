# Redis Java 高级工程师面试题

## 1. Redis 为什么快？

答：Redis 主要数据在内存中；核心命令在单线程事件循环中顺序执行，避免锁竞争；数据结构和编码高度优化；RESP 协议简单；支持 Pipeline 降低 RTT。追问时要补充：Redis 也会因大 key、慢命令、fork、AOF fsync、网络、客户端缓冲、swap 变慢。

## 2. Redis 单线程为什么还能支持高并发？

答：Redis 的瓶颈很多时候不是 CPU，而是内存访问和网络 I/O；事件驱动模型能高效处理大量连接；命令执行短小，单线程避免锁竞争。高并发不代表所有操作都快，大 key 和慢命令仍会阻塞所有客户端。

## 3. Redis 6 I/O 多线程后是否不再单线程？

答：I/O 多线程主要优化网络读写，核心命令执行语义仍按单线程原子模型理解。不能因为有 I/O threads 就在 Lua 或大 key 操作上放松约束。

## 4. String 和 Hash 存对象怎么选？

答：整体读写、对象小、结构简单可用 String JSON；字段频繁局部更新、只读部分字段可用 Hash。Hash 也可能成为大 key，String JSON 也可能有反序列化和全量回写成本。

## 5. ZSet 为什么适合排行榜？

答：ZSet 为 member 维护 score，并支持按 score/rank 范围查询。大集合常结合 dict 和 skiplist，dict 支持按 member 查分数，skiplist 支持有序范围操作。

## 6. Bitmap 适合什么场景？

答：适合大量布尔状态，如签到、活跃用户。优点是节省内存；坑是 offset 过大会直接撑大字符串，需要连续 id 映射。

## 7. HyperLogLog 为什么不能做精确统计？

答：它是近似基数估计算法，适合 UV 趋势，不保存成员明细，不能用于计费、权限、库存等强精确场景。

## 8. Redis 过期 key 是立即删除吗？

答：不是。Redis 结合惰性删除和定期抽样删除。到期 key 不保证准点消失，大量 key 同时过期会带来 CPU 尖刺和雪崩风险。

## 9. Redis 淘汰策略有哪些？

答：常见有 `noeviction`、`allkeys-lru`、`volatile-lru`、`allkeys-lfu`、`volatile-lfu`、`allkeys-random`、`volatile-random`、`volatile-ttl`。纯缓存常用 allkeys-lru/lfu；准数据库慎用淘汰。

## 10. LRU 和 LFU 如何选？

答：LRU 看最近访问，适合访问热点随时间变化；LFU 看访问频率，适合长期热点。Redis 使用近似实现，不是全局精确。

## 11. 什么是大 key？怎么治理？

答：value 很大或集合元素很多的 key。危害是阻塞、网络峰值、迁移慢、删除慢。治理包括拆分、分页、限制长度、SCAN 分批、UNLINK 异步删除、巡检告警。

## 12. 什么是热 key？怎么治理？

答：访问频率极高的 key。治理包括本地缓存、热点分片、读副本、请求合并、互斥重建、限流降级和预热。

## 13. RDB 和 AOF 区别？

答：RDB 是时间点快照，文件小、恢复快，但可能丢快照间数据；AOF 记录写命令，持久性更高，可配置 fsync，但文件更大、重放可能慢。高数据安全常用 RDB + AOF。

## 14. AOF everysec 会丢数据吗？

答：可能。默认每秒 fsync，极端宕机可能丢约 1 秒写入。它是性能和持久性的折中。

## 15. fork 和 copy-on-write 为什么影响 Redis？

答：RDB/AOF rewrite 会 fork 子进程。fork 大实例会停顿；fork 后父进程写入会复制内存页，导致额外内存峰值。

## 16. 主从复制是强一致吗？

答：不是。Redis 复制默认异步，master 写成功不代表 replica 已同步。故障转移可能丢最后一段未复制数据。

## 17. 读写分离有什么坑？

答：主从延迟导致读旧数据。刚写后需要读 master，或者业务接受最终一致。强一致路径不要读从。

## 18. Sentinel 解决什么？

答：Sentinel 解决主从监控、故障检测、自动故障转移和主节点发现。它不做分片，也不能消除异步复制丢数据窗口。

## 19. Cluster 为什么是 16384 个 slot？

答：Cluster 用固定数量 hash slot 做分片，key 映射到 slot，slot 分配给 master。16384 是实现上在路由表大小、迁移粒度和管理成本之间的折中。

## 20. MOVED 和 ASK 区别？

答：MOVED 表示 slot 已稳定迁到另一个节点，客户端应更新路由；ASK 表示迁移过程中的临时重定向，客户端只对本次请求按目标节点执行。

## 21. Cluster 多 key 操作为什么受限？

答：多 key 原子操作要求所有 key 在同一 slot。可用 hash tag，如 `{user1}`，让相关 key 落同一 slot。

## 22. Redis 事务支持回滚吗？

答：不支持传统回滚。EXEC 后某条命令运行错误，其他命令仍执行。Redis 选择简单和性能，不提供关系型数据库式 rollback。

## 23. WATCH 是什么？

答：WATCH 是乐观锁 CAS。被 WATCH 的 key 在 EXEC 前被其他客户端修改，则 EXEC 失败，客户端重试。

## 24. Pipeline 和事务有什么区别？

答：Pipeline 是网络优化，批量发送命令，不保证原子；事务是 MULTI/EXEC 队列顺序执行，保证执行期间不被插入，但也不回滚。

## 25. Lua 为什么能保证原子？

答：Lua 脚本在 Redis 服务端执行，执行期间不会插入其他命令。但脚本运行太久会阻塞主线程，所以脚本必须短小。

## 26. 分布式锁正确写法？

答：加锁用 `SET key uniqueValue NX PX timeout`；释放用 Lua 比较 value 后删除。业务要设置合理过期、支持续期或幂等，核心正确性由数据库兜底。

## 27. 为什么不能直接 DEL 释放锁？

答：锁可能已过期并被其他客户端重新获得，直接 DEL 会删除别人的锁。必须比较 owner token。

## 28. Redlock 可靠吗？

答：Redlock 比单实例锁考虑更多故障，但仍有争议，不能替代共识系统。面试中要说明：用于效率锁可以，强正确性要数据库约束或共识组件兜底。

## 29. 缓存穿透、击穿、雪崩区别？

答：穿透是查不存在数据绕过缓存打 DB；击穿是热点 key 失效瞬间大量请求打 DB；雪崩是大量 key 同时失效或 Redis 整体不可用导致 DB 被打爆。

## 30. 如何解决缓存穿透？

答：参数校验、缓存空值、Bloom Filter、限流风控。Bloom 有误判无漏判，删除困难。

## 31. 如何解决缓存击穿？

答：互斥锁重建、逻辑过期、热点永不过期加后台刷新、本地缓存和请求合并。

## 32. 如何解决缓存雪崩？

答：TTL 随机抖动、分批预热、多级缓存、限流熔断、Redis 高可用、数据库保护。

## 33. 缓存和数据库一致性怎么做？

答：常用先更新数据库，再删除缓存；删除失败要重试或消息补偿；设置 TTL 作为最终兜底。强一致场景不要依赖缓存保证正确性。

## 34. 为什么不推荐先删缓存再更新数据库？

答：并发读可能在数据库更新前读到旧值并写回缓存，导致旧缓存长期存在。

## 35. 延迟双删解决什么？

答：降低并发读把旧值写回缓存的概率。它不是强一致保证，延迟时间和任务可靠性都要设计。

## 36. Java RedisTemplate 最大坑是什么？

答：序列化。默认 JDK 序列化可读性差、跨语言差，也有安全风险。项目要统一 key/value/hash 序列化。

## 37. Spring Cache 有什么坑？

答：self-invocation 代理不生效，key 生成策略不明确，TTL 和空值缓存配置不当，复杂一致性不适合只靠注解。

## 38. Lettuce 和 Jedis 区别？

答：Lettuce 基于 Netty，线程安全，支持同步异步响应式；Jedis 传统阻塞 I/O，通常依赖连接池。Spring Boot 常用 Lettuce。

## 39. Redis 连接池怎么配置？

答：设置最大连接、空闲连接、获取连接等待时间、命令超时。阻塞命令、订阅、Pipeline 最好隔离连接。

## 40. Redis 慢怎么排查？

答：看 SLOWLOG、LATENCY DOCTOR、INFO commandstats、CPU、网络、磁盘、swap、latest_fork_usec、大 key、热 key、AOF fsync、客户端连接和输出缓冲。

## 41. KEYS 为什么危险？

答：`KEYS *` 会遍历整个 keyspace，阻塞主线程。线上用 SCAN 分批遍历。

## 42. SCAN 有什么注意事项？

答：SCAN 游标式遍历，不保证一次完整快照，可能返回重复元素，客户端要去重并容忍遍历期间变化。

## 43. Redis 能当消息队列吗？

答：可以但要分场景。List 简单队列可靠性弱；Pub/Sub 不持久；Stream 支持消费组、ACK、Pending，更像轻量消息流，但不等于 Kafka。

## 44. Stream Pending 堆积怎么办？

答：检查消费者是否 ACK，使用 XPENDING 查看，XAUTOCLAIM/XCLAIM 转移超时消息，消费者处理必须幂等，并设置裁剪策略。

## 45. Redis 如何做限流？

答：固定窗口用 INCR+EXPIRE；滑动窗口用 ZSet；令牌桶用 Lua 原子计算。根据平滑性、精度、成本选择。

## 46. Redis 如何做延迟队列？

答：ZSet score 存执行时间，消费者取到期任务并原子抢占；更可靠的可用 Stream 或专业 MQ。

## 47. Redis 安全基线是什么？

答：不暴露公网，开启 protected-mode，绑定内网，使用 ACL/密码，TLS，限制危险命令，最小权限，保护配置、日志和备份。

## 48. Redis 内存满了会怎样？

答：取决于 maxmemory-policy。可能淘汰 key，也可能写入报错。淘汰发生在写路径，策略选择会影响业务语义。

## 49. 如何发现缓存命中率低？

答：看 INFO stats 的 keyspace_hits 和 keyspace_misses，结合业务埋点、TTL、key 生成规则、穿透流量和预热状态分析。

## 50. Redis 可以保证库存不超卖吗？

答：Redis Lua 可以做高并发预扣，但最终正确性要数据库唯一约束、乐观锁或事务兜底，并设计超时释放、幂等和对账。

## 面试总结模板

回答 Redis 题可以按四层展开：

1. 命令和使用层：这个功能怎么用。
2. 原理层：底层结构、复杂度、执行语义。
3. 生产层：大 key、热 key、延迟、故障、监控。
4. 取舍层：什么时候不用 Redis，或需要 DB/MQ/共识系统兜底。

## 参考资料

- https://redis.io/docs/latest/get-started/
- https://redis.io/docs/latest/develop/data-types/
- https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/
- https://redis.io/docs/latest/operate/oss_and_stack/management/replication/
- https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/
- https://redis.io/docs/latest/develop/using-commands/transactions/
- https://redis.io/docs/latest/develop/using-commands/pipelining/
- https://redis.io/docs/latest/develop/programmability/eval-intro/
- https://docs.spring.io/spring-data/redis/reference/redis/template.html
