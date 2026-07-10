<script>
export default {
  onLaunch() { console.log('智慧医养健康档案 小程序启动') }
}
</script>

<style>
/* ============================================================
   智慧医养 · 小程序端 全局设计系统（与 Web 端温暖健康风统一）
   ============================================================ */
page {
  --primary: #2FA37C;
  --primary-2: #279470;
  --bg: #F1EFE7;
  --card: #FFFFFF;
  --ink: #21362E;
  --ink-soft: #5B6B63;
  --line: #E6E1D5;
  --accent: #E8933B;
  --danger: #E5654B;
  --blue: #3E8ED0;

  background: var(--bg);
  color: var(--ink);
  font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: 30rpx;
}

.page-pad { padding: 28rpx; }

/* —— 卡片 —— */
.card {
  background: var(--card);
  border-radius: 28rpx;
  box-shadow: 0 6rpx 28rpx rgba(33,80,63,.08);
  padding: 32rpx;
}

/* —— 按钮 —— */
.btn-primary {
  background: linear-gradient(135deg, #37B389, var(--primary-2));
  color: #fff; border-radius: 999rpx; font-weight: 600; font-size: 32rpx;
  box-shadow: 0 12rpx 26rpx rgba(47,163,124,.30); border: none;
}
.btn-primary:active { transform: scale(.97); }
.btn-ghost { background: #fff; color: var(--primary); border: 2rpx solid var(--primary); border-radius: 999rpx; font-weight: 600; }

/* —— 渐变横幅 —— */
.banner {
  position: relative; border-radius: 32rpx; padding: 44rpx 40rpx; color: #fff; overflow: hidden;
  background: linear-gradient(135deg, #37B389 0%, #2FA37C 55%, #23856A 100%);
  box-shadow: 0 16rpx 40rpx rgba(35,133,106,.28);
}

/* —— 药丸标签 —— */
.pill { display: inline-block; padding: 8rpx 22rpx; border-radius: 999rpx; font-size: 24rpx; font-weight: 600; }
.pill-green { background: #EAF6F2; color: var(--primary); }
.pill-warn  { background: #FDF0E2; color: var(--accent); }

/* —— 文字 —— */
.title-lg { font-size: 46rpx; font-weight: 800; letter-spacing: -1rpx; }
.muted { color: var(--ink-soft); }
.num { font-variant-numeric: tabular-nums; font-weight: 800; }

/* —— 入场动画 —— */
@keyframes fadeUp { from { opacity: 0; transform: translateY(28rpx); } to { opacity: 1; transform: none; } }
@keyframes floatY { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-12rpx); } }
@keyframes spin   { to { transform: rotate(360deg); } }
@keyframes pulse  { 0%{ box-shadow:0 0 0 0 rgba(55,168,107,.5) } 70%{ box-shadow:0 0 0 16rpx rgba(55,168,107,0) } 100%{ box-shadow:0 0 0 0 rgba(55,168,107,0) } }

.enter   { animation: fadeUp .5s cubic-bezier(.22,1,.36,1) both; }
.enter-1 { animation: fadeUp .5s cubic-bezier(.22,1,.36,1) .06s both; }
.enter-2 { animation: fadeUp .5s cubic-bezier(.22,1,.36,1) .12s both; }
.enter-3 { animation: fadeUp .5s cubic-bezier(.22,1,.36,1) .18s both; }
.enter-4 { animation: fadeUp .5s cubic-bezier(.22,1,.36,1) .24s both; }
.enter-5 { animation: fadeUp .5s cubic-bezier(.22,1,.36,1) .30s both; }
</style>
