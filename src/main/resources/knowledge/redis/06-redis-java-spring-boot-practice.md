# Java 与 Spring Boot 使用 Redis 实战

## Java 客户端选型

常见客户端：

- Lettuce：基于 Netty，线程安全，支持同步、异步、响应式，Spring Boot 默认常用。
- Jedis：使用简单，传统阻塞 I/O，常配连接池。
- Redisson：提供分布式对象、锁、限流器、延迟队列等高级封装。
- Spring Data Redis：Spring 对 Redis 访问的抽象，包含 RedisTemplate、Repository、Cache、Pub/Sub 等。

选择建议：

- Spring Boot 常规业务：Spring Data Redis + Lettuce。
- 需要 RedLock、可重入锁、看门狗、分布式集合：Redisson，但要理解封装背后的 Redis 语义。
- 极简命令调用：Jedis 可以，但连接池和超时要配置好。

## RedisTemplate 与 StringRedisTemplate

`RedisTemplate` 提供高级操作抽象：

- `opsForValue()`
- `opsForHash()`
- `opsForList()`
- `opsForSet()`
- `opsForZSet()`
- `execute()` 执行回调或 Lua

`StringRedisTemplate` 默认 key/value 都更偏字符串，适合纯字符串场景。

隐藏坑：

- 默认 Java 原生序列化会生成不可读二进制，跨语言差，也有反序列化安全风险。
- 多个服务使用 Redis 时，必须统一 key、value、hashKey、hashValue 序列化方式。
- 不同 template 写同一个 key 可能互相读不出来。

推荐：

- key 使用 `StringRedisSerializer`。
- 普通对象 value 使用 JSON 序列化，例如 Jackson，并固定类型策略。
- 安全敏感场景避免反序列化不可信输入。
- 为不同业务类型定义清晰的 RedisTemplate Bean，避免全局混用。

## Spring Cache

Spring Cache 用注解简化缓存：

- `@Cacheable`
- `@CachePut`
- `@CacheEvict`
- `@Caching`

适合：

- 查询结果缓存。
- 读多写少。
- 缓存逻辑简单。

隐藏坑：

- 默认 key 生成策略可能不符合跨服务规范。
- self-invocation 同类内部调用不会经过代理，注解可能不生效。
- 缓存空值、TTL、序列化、前缀要明确配置。
- `@CachePut` 和 `@Cacheable` 混用容易造成语义混乱。
- 复杂一致性逻辑不要只靠注解。

## 连接池与超时

必须配置：

- connect timeout。
- command timeout。
- pool max-active / max-idle / min-idle。
- shutdown timeout。
- socket keepalive。

原则：

- Redis 超时要小于接口总体超时预算。
- 连接池耗尽要快速失败，不要无限等待。
- 阻塞命令使用独立连接或连接池。
- Pipeline、订阅、阻塞队列不要和普通命令抢连接。

常见故障：

- Redis 慢导致 Tomcat/业务线程堆积。
- 连接池耗尽导致全站卡住。
- 超时过长导致故障扩散。
- 客户端无限重试导致雪崩。

## 序列化安全

Spring Data Redis 官方文档提醒，Java 原生序列化在不可信环境中存在反序列化安全风险。

工程建议：

- 不使用 JDK 原生序列化存不可信数据。
- 使用 JSON 时限制默认类型信息，避免危险多态反序列化。
- key/value 格式要版本化，例如 `v1:user:profile:{id}`。
- 变更对象字段时考虑兼容旧缓存。
- 不把敏感明文长久存 Redis，至少设置 TTL 和访问控制。

## Lua 在 Java 中使用

常见用法：

- `DefaultRedisScript<Long>`。
- 传入 `KEYS` 和 `ARGV`。
- 使用 `RedisTemplate.execute(script, keys, args)`。

典型解锁脚本：

```lua
if redis.call("GET", KEYS[1]) == ARGV[1] then
  return redis.call("DEL", KEYS[1])
else
  return 0
end
```

注意：

- 脚本返回类型要和 Java 泛型一致。
- Cluster 中脚本涉及多个 key 时必须同 slot。
- 脚本内容要短小，避免阻塞 Redis。

## Pipeline 在 Java 中使用

适合：

- 批量 set。
- 批量 get。
- 批量删除。

注意：

- 控制批次大小，例如 500 或 1000 一批，而不是几十万一次。
- 大 value 场景批次更小。
- Cluster pipeline 需要客户端正确按节点路由。
- Pipeline 返回结果顺序与命令顺序一致，但错误处理要逐项检查。

## Redis 事务在 Spring 中的坑

Spring Data Redis 支持事务，但要理解 Redis 事务语义。

注意：

- Redis 事务不回滚。
- `MULTI` 后读命令通常返回 queued，不是立即结果。
- 与 Spring 数据库事务不是同一种事务。
- Redis 事务和 DB 事务无法天然保证跨资源原子性。

工程建议：

- 简单 CAS 可以用 WATCH。
- 多命令原子优先 Lua。
- 跨 DB 和 Redis 的一致性用最终一致、消息补偿、outbox。

## Redisson

Redisson 提供：

- `RLock`
- `RReadWriteLock`
- `RSemaphore`
- `RRateLimiter`
- `RDelayedQueue`
- `RMap`
- `RBucket`

优点：

- 封装复杂 Lua 和续期逻辑。
- API 类似 Java 并发工具。
- 适合快速落地分布式协调能力。

隐藏坑：

- 不能因为用了 Redisson 就忽略锁过期、网络分区、业务幂等。
- 看门狗续期依赖客户端进程存活和调度。
- 锁用于效率，核心正确性仍要数据库兜底。
- Redisson 对 Redis 模式、版本和拓扑有要求。

## Spring Boot 配置示例

示意：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2s
      lettuce:
        pool:
          max-active: 32
          max-idle: 16
          min-idle: 4
```

生产还要配置：

- password 或 ACL user。
- TLS。
- Sentinel 或 Cluster。
- database index，Cluster 模式通常只有 db0。
- command timeout 和 reconnect 策略。

## 常见代码坏味道

- `keys("*")` 在线执行。
- 缓存 value 不设置 TTL。
- 所有 key 都一个前缀，没有业务域。
- `RedisTemplate<Object, Object>` 到处注入。
- JDK 序列化和 JSON 序列化混用。
- 删除缓存失败不处理。
- Redis 异常直接拖垮主链路。
- 热点缓存过期没有互斥重建。
- 连接池默认配置上线。
- 大批量操作不分批。

## 测试建议

- 单元测试 key 生成规则。
- 集成测试序列化兼容。
- 测试 TTL 是否设置。
- 测试 Redis 不可用时降级行为。
- 测试缓存删除失败重试。
- 测试并发缓存重建。
- Testcontainers 启动 Redis 做集成测试。

## 参考资料

- https://redis.io/docs/latest/integrate/spring-framework-cache/
- https://docs.spring.io/spring-data/redis/reference/redis/template.html
- https://spring.io/projects/spring-data-redis
- https://redis.io/docs/latest/develop/using-commands/pipelining/
- https://redis.io/docs/latest/develop/programmability/eval-intro/
