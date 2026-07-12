<script>
export default {
  onLaunch() { console.log('智慧医养医生端启动') }
}
</script>

<style>
page {
  --primary: #2E6FE0;
  --primary-2: #2458B8;
  --success: #2FA37C;
  --bg: #EFF4FA;
  --card: #FFFFFF;
  --ink: #16283E;
  --ink-soft: #5D7189;
  --line: #DFE7F2;
  background: var(--bg);
  color: var(--ink);
  font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: 30rpx;
}
.page-pad { padding: 28rpx; }
.card { background: var(--card); box-shadow: 0 8rpx 28rpx rgba(28,63,120,.08); padding: 30rpx; }
.btn-primary { background: linear-gradient(135deg, #3E86EC, var(--primary-2)); color: #fff; font-weight: 700; border-radius: 0; }
.btn-ghost { background: #fff; color: var(--primary); border: 2rpx solid var(--primary); border-radius: 0; }
.muted { color: var(--ink-soft); }
.title-lg { font-size: 44rpx; font-weight: 800; }
.num { font-variant-numeric: tabular-nums; font-weight: 900; }
input, textarea { font-size: 30rpx; }
</style>
