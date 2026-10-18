# k6 测试

所有命令在项目根目录执行。

## 准备

- 安装 k6，启动 MySQL、Redis 和应用；混合测试使用 `sky_take_out_loadtest` 压测库。
- 负载参数直接修改对应脚本；运行 Load 前先确认负载配置。

## 生成账号（仅 mixed 测试需要）

1. 在压测库准备业务表及菜品数据，再执行 [账号初始化 SQL](sql/batch_create_load_test_user_and_address.sql)，创建 1000 个用户及默认地址。
2. 在 IDEA 中用 JDK 21 运行 [LoadTestTokenGenerator.java](../../sky-server/src/test/java/com/sky/loadtest/LoadTestTokenGenerator.java) 的 `main()`；工作目录设为项目根目录，无需运行参数。
3. 生成 `performance/k6/data/loadtest-users.json`，重复运行会覆盖；Token 默认有效期为 8 小时，过期后需要重新生成。

## 运行测试

| 脚本 | 用途与当前负载                                                    |
| --- |------------------------------------------------------------|
| `dish-list.smoke.js` | 菜品列表检查：1 个 VU，10 秒                                         |
| `dish-list.load.js` | 菜品列表压测：目标 1000 次请求/秒，5 分钟                                  |
| `mixed.smoke.js` | 浏览、加购、下单、详情：执行一次完整流程                                       |
| `mixed.load.js` | 浏览/购物车/下单占比 55%/25%/20%；1 分钟升至每秒 500 个流程（3550RPS），再保持 5 分钟 |

选择一个脚本运行：

```bash
k6 run performance/k6/tests/dish-list.smoke.js
k6 run performance/k6/tests/dish-list.load.js
k6 run performance/k6/tests/mixed.smoke.js
k6 run performance/k6/tests/mixed.load.js
```

测试会创建待付款订单，不执行支付。

结果重点看失败率、检查是否通过、p95/p99 和 `dropped_iterations`。

测试报告中 http_reqs 所展示的 RPS 为包含了预热阶段的 RPS 平均值，并非实际满载 RPS。


## 导出报告

以下以 mixed.load 为例，修改 `test_name` 可切换脚本：

```bash
mkdir -p performance/k6/results
test_name=mixed.load
report_prefix="performance/k6/results/${test_name}-$(date -u +%Y%m%dT%H%M%SZ)"

K6_WEB_DASHBOARD=true \
K6_WEB_DASHBOARD_PORT=-1 \
K6_WEB_DASHBOARD_PERIOD=1s \
K6_WEB_DASHBOARD_EXPORT="${report_prefix}.html" \
k6 run \
    --summary-export="${report_prefix}.summary.json" \
    "performance/k6/tests/${test_name}.js"
```

HTML 用浏览器查看曲线，JSON 用于保存汇总及自动对比；只看 HTML 时可去掉 `--summary-export`。

