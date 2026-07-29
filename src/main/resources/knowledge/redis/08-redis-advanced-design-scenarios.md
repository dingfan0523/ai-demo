# Redis 高级设计场景

## 限流

固定窗口：

- `INCR rate:user:1:202607281200`
- 第一次设置 `EXPIRE`
- 简单高效，但窗口边界可能突刺。

滑动窗口：

- ZSet score 存时间戳。
- 请求来时删除窗口外记录。
- 统计窗口内数量。
- 添加当前请求。

令牌桶：

- 记录剩余 token 和上次补充时间。
- Lua 原子计算补充和扣减。

面试取舍：

- 固定窗口简单但不平滑。
- 滑动窗口精确但内存和计算成本更高。
- 令牌桶适合平滑限流和允许短突发。

## 排行榜

使用 ZSet：

- `ZINCRBY leaderboard 1 user1`
- `ZRANGE leaderboard 0 99 REV WITHSCORES`
- `ZREVRANK leaderboard user1`

设计点：

- 按天、周、月拆 key。
- 热门榜单加本地缓存。
- 大榜单分页限制。
- 用户昵称头像不要都塞到 member，可用 Hash 补资料。

隐藏坑：

- score 精度。
- 热榜 key 过热。
- 需要历史榜单时不能只覆盖一个 key。

## 延迟队列

ZSet 实现：

- member 是任务 id。
- score 是执行时间戳。
- 消费者查询 `ZRANGEBYSCORE delay -inf now LIMIT 0 n`。
- 用 Lua 或 `ZREM` 抢占成功后处理。

问题：

- 消费失败怎么办？
- 消费者崩溃任务会不会丢？
- 并发抢占是否安全？
- 是否需要死信队列？

更可靠选择：

- Redis Stream。
- 专业 MQ 的延迟消息。

## 幂等

Redis 实现幂等：

- `SET idem:requestId result NX EX ttl`
- 处理中状态、成功状态、失败状态分开。
- 或使用 Hash 记录请求状态。

注意：

- TTL 要覆盖业务重试周期。
- 幂等 key 要来自业务唯一请求号，而不是随机 trace id。
- 金额、库存等仍要数据库唯一约束兜底。

## 分布式会话

适合：

- 多实例 Web 应用 session 共享。
- 登录态、验证码、短期 token。

设计：

- 设置 TTL。
- 滑动续期要谨慎，避免活跃用户永不过期占满内存。
- session value 不要过大。
- 敏感字段加密或不放 Redis。

隐藏坑：

- Redis 故障导致全站登录态异常。
- 序列化类变更导致 session 反序列化失败。
- 单 key 热点，例如全局 token 配置。

## Feed 流

推模式：

- 发布内容时写入粉丝收件箱 List/ZSet。
- 读很快，写放大严重。

拉模式：

- 读时从关注列表聚合作者动态。
- 写轻，读重。

混合模式：

- 普通用户推。
- 大 V 拉或只推活跃粉丝。

Redis 作用：

- 存短期 timeline 索引。
- ZSet 按时间排序。
- Hash 存动态摘要。

隐藏坑：

- 大 V 写扩散。
- timeline 无限增长。
- 删除和权限变化难同步。

## 库存扣减

Redis 可做：

- 活动库存预扣。
- 秒杀限流。
- 防重复提交。

原子扣减 Lua：

- 检查库存大于 0。
- 扣减库存。
- 记录用户购买标记。

必须兜底：

- 数据库库存最终扣减。
- 订单幂等。
- 超时释放库存。
- Redis 与 DB 对账。

面试回答：

- Redis 能抗高并发入口，但不能单独承担最终账实一致。

## 抽奖

Set：

- `SADD lottery:candidates userId`
- `SPOP lottery:candidates count`

ZSet：

- 带权重抽奖可结合 score 或别的算法。

注意：

- 公平性和可审计性要记录。
- 奖品库存要数据库兜底。
- 用户资格要幂等校验。

## 签到

Bitmap：

- 每天一个 key，offset 是用户映射 id。
- 每个用户每月一个 key，offset 是日期。

统计：

- `BITCOUNT`
- `GETBIT`
- 连续签到需要应用层或 Lua 计算。

注意：

- offset 映射要连续，否则浪费巨大内存。

## UV 统计

HyperLogLog：

- `PFADD uv:page:1:20260728 userId`
- `PFCOUNT uv:page:1:20260728`

适合：

- 页面 UV。
- 活动 UV。
- 粗粒度趋势。

不适合：

- 计费。
- 权限判断。
- 需要列出用户。

## 附近的人或门店

Geo：

- `GEOADD geo:shop lon lat shopId`
- `GEOSEARCH geo:shop FROMLONLAT lon lat BYRADIUS 5 km`

注意：

- 热点城市分片。
- 隐私合规。
- 坐标更新频率高时要评估写压力。

## 布隆过滤器防穿透

流程：

1. 数据库已有 id 加入 Bloom Filter。
2. 请求先问 Bloom。
3. 判断不存在则直接返回。
4. 可能存在再查缓存/数据库。

注意：

- Bloom 有误判，无漏判。
- 删除困难。
- 要规划容量和误判率。
- 数据初始化和增量同步要可靠。

## RAG 与向量检索

Redis 现代版本和生态支持向量检索能力，可用于语义搜索和 RAG。

RAG 中 Redis 可能扮演：

- 文档 chunk metadata 存储。
- embedding 向量索引。
- 会话上下文缓存。
- 召回结果缓存。
- 热门问题答案缓存。

设计点：

- chunk id、document id、source uri、tags 要存 metadata。
- 向量索引要记录 embedding model 和 index version。
- 更新文档后要重建相关 chunk。
- RAG 缓存必须防 prompt injection，把检索内容视为不可信上下文。

## 参考资料

- https://redis.io/docs/latest/develop/data-types/
- https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/
- https://redis.io/docs/latest/develop/use-cases/
- https://redis.io/docs/latest/get-started/
