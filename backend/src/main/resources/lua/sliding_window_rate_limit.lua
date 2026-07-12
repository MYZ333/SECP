-- 滑动窗口限流（ZSET 实现，原子执行避免并发竞态）
-- KEYS[1]         限流 key
-- ARGV[1] window  窗口毫秒
-- ARGV[2] limit   窗口内允许的最大请求数
-- ARGV[3] now     当前时间戳(毫秒)
-- ARGV[4] member  本次请求唯一标识(时间戳+随机)
-- 返回 1 = 放行，0 = 拒绝
local key    = KEYS[1]
local window = tonumber(ARGV[1])
local limit  = tonumber(ARGV[2])
local now    = tonumber(ARGV[3])
local member = ARGV[4]

-- 清理窗口外的旧记录
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

local count = redis.call('ZCARD', key)
if count < limit then
    redis.call('ZADD', key, now, member)
    redis.call('PEXPIRE', key, window)
    return 1
else
    return 0
end
