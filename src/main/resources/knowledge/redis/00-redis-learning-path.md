# Redis 知识库学习路线与索引

## 文档定位

本目录是一套面向 Java 高级工程师面试和工程实践的 Redis Markdown 知识库。内容覆盖基础概念、数据结构、底层实现、持久化、复制、高可用、集群、事务、Lua、缓存模式、典型坑位、Java/Spring Boot 集成、运维排障、安全和高频面试题。

这些文档适合导入当前项目的 RAG 知识库，默认路径为 `src/main/resources/knowledge/redis`。

## 建议入库请求

```json
{
  "sourceType": "markdown",
  "sourcePath": "src/main/resources/knowledge/redis",
  "tags": ["redis", "java高级面试", "缓存", "分布式系统"],
  "overwrite": true
}
```

## 学习顺序

1. `01-redis-core-model-and-data-types.md`
   - 建立 Redis 是什么、为什么快、常见数据结构如何选型的基础框架。
   - 面试关键词：String、Hash、List、Set、ZSet、Stream、Bitmap、HyperLogLog、Geo、JSON、Vector Set。

2. `02-redis-internals-memory-expire-eviction.md`
   - 理解 Redis 单线程事件循环、对象编码、内存布局、过期删除和淘汰策略。
   - 面试关键词：SDS、dict、listpack、quicklist、skiplist、渐进式 rehash、过期字典、惰性删除、定期删除、LRU、LFU、内存碎片。

3. `03-redis-persistence-replication-ha-cluster.md`
   - 掌握 RDB、AOF、复制、Sentinel、Cluster 的原理和取舍。
   - 面试关键词：fork、copy-on-write、appendfsync、AOF rewrite、PSYNC、replication backlog、主从延迟、脑裂、16384 slots、MOVED、ASK、hash tag。

4. `04-redis-command-semantics-transactions-lua-streams.md`
   - 深入命令执行语义、事务、乐观锁、Pipeline、Lua、Pub/Sub、Stream。
   - 面试关键词：MULTI、EXEC、WATCH、无回滚、Pipeline 不保证原子性、Lua 原子执行、脚本阻塞、Consumer Group、Pending List。

5. `05-redis-cache-patterns-and-production-pitfalls.md`
   - 面向生产缓存架构：一致性、穿透、击穿、雪崩、大 key、热 key、分布式锁。
   - 面试关键词：Cache Aside、TTL jitter、Bloom Filter、互斥重建、逻辑过期、延迟双删、旁路缓存一致性。

6. `06-redis-java-spring-boot-practice.md`
   - Java 工程落地：Lettuce、Jedis、RedisTemplate、StringRedisTemplate、Spring Cache、Redisson。
   - 面试关键词：序列化、连接池、超时、集群重定向、Pipeline、Lua 解锁、Jackson 安全、CacheManager。

7. `07-redis-operations-observability-security.md`
   - 运维排障、安全和容量治理。
   - 面试关键词：INFO、SLOWLOG、LATENCY DOCTOR、MONITOR、CLIENT LIST、maxmemory、ACL、TLS、protected-mode、rename-command。

8. `08-redis-advanced-design-scenarios.md`
   - 高级场景设计：限流、排行榜、延迟队列、幂等、会话、Feed、库存、地理位置、RAG 向量检索。
   - 面试关键词：滑动窗口、令牌桶、ZSet 延迟队列、Stream 消息队列、BitMap 签到、HLL UV、GEO 附近的人。

9. `09-redis-senior-interview-qa.md`
   - Java 高级工程师常见 Redis 面试题和追问答案。
   - 面试关键词：原理题、场景题、线上故障题、取舍题、Java 集成题。

## 学习目标

完成本知识库后，应该能回答这些问题：

- Redis 为什么快？快在哪里？什么情况下会变慢？
- Redis 各种数据结构底层如何实现？什么时候选错结构会造成事故？
- RDB、AOF、复制、Sentinel、Cluster 分别解决什么问题？不能解决什么问题？
- Redis 缓存和数据库一致性为什么难？怎样根据业务容忍度选方案？
- Java/Spring 使用 Redis 时，序列化、连接池、超时、事务、Pipeline、集群有什么坑？
- 如何定位 Redis 延迟、内存暴涨、大 key、热 key、慢命令、连接数爆炸？
- 高级面试中，如何从命令层回答到实现层，再回答到生产取舍？

## 重要提醒

- Redis 可以当缓存、数据库、消息组件、流处理组件和向量检索组件，但不要把所有强一致业务都压到 Redis 上。
- Redis 原子性主要来自单线程命令执行模型和 Lua 脚本；它不是关系型数据库事务，也没有传统事务回滚。
- Redis 高可用不等于零丢数据。异步复制、故障转移、网络分区都会带来窗口期。
- 缓存方案必须同时设计 TTL、穿透保护、热点保护、降级策略、监控指标和容量上限。
- Java 项目里，Redis 最大的隐患通常不是命令不会用，而是序列化不统一、超时不收敛、缓存一致性边界没说清。

## 主要资料来源

- Redis 官方文档：https://redis.io/docs/latest/
- Redis data types：https://redis.io/docs/latest/develop/data-types/
- Redis persistence：https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/
- Redis replication：https://redis.io/docs/latest/operate/oss_and_stack/management/replication/
- Redis Cluster specification：https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/
- Redis Sentinel：https://redis.io/docs/latest/operate/oss_and_stack/management/sentinel/
- Redis transactions：https://redis.io/docs/latest/develop/using-commands/transactions/
- Redis pipelining：https://redis.io/docs/latest/develop/using-commands/pipelining/
- Redis Lua scripting：https://redis.io/docs/latest/develop/programmability/eval-intro/
- Redis distributed locks：https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/
- Spring Data Redis Reference：https://docs.spring.io/spring-data/redis/reference/
