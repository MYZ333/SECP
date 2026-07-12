-- 幂等校验：原子地"存在即消费"（防重复提交）
-- KEYS[1] 幂等 token 的 Redis key
-- 返回 1 = 首次(放行)，0 = 重复(拒绝)
local key = KEYS[1]
if redis.call('EXISTS', key) == 1 then
    redis.call('DEL', key)
    return 1
else
    return 0
end
