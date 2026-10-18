import http from 'k6/http';
import { check, fail } from 'k6';
import exec from 'k6/execution';
import { SharedArray } from 'k6/data';

const baseUrl = 'http://localhost:8080';
const duration = '5m';
const flowsPerSecond = 500; // 每秒共 x 个流程，按 55% / 25% / 20% 分配到 browse / cart / order。
// flowsPerSecond = 500 情况下 RPS 为 3550
const vusPerScenario = 300;
const packAmount = 3;

const accounts = new SharedArray('loadtest accounts', () => {
    const data = JSON.parse(open('../data/loadtest-users.json'));
    return data.users.map(user => ({ ...user, tokenHeader: data.tokenHeader }));
});

if (!Number.isInteger(flowsPerSecond) || flowsPerSecond <= 0 || flowsPerSecond % 20 !== 0) {
    throw new Error('flowsPerMinute 必须是 20 的正整数倍，以保持 60% / 25% / 15%');
}
if (vusPerScenario * 3 > accounts.length) {
    throw new Error('三个场景的 VU 总数不能超过账号池容量');
}

function scenario(functionName, weight) {
    const rate = flowsPerSecond / 20 * weight;
    return {
        executor: 'ramping-arrival-rate',
        exec: functionName,
        startRate: 0,
        timeUnit: '1s',
        stages: [
            { duration: '1m', target: rate }, // 预热：逐步升至目标速率。
            { duration, target: rate }, // 保持目标速率；汇总指标仍包含预热阶段。
        ],
        preAllocatedVUs: vusPerScenario,
        gracefulStop: '30s',
    };
}

export const options = {
    scenarios: {
        browse: scenario('browse', 11),
        cart: scenario('cart', 5),
        order: scenario('order', 4),
    },
    thresholds: {
        http_reqs: ['count>0'],
        http_req_failed: ['rate==0'],
        checks: ['rate==1'],
        dropped_iterations: ['count==0'],
    },
};

function expect(name, condition) {
    if (!check(null, { [name]: () => condition })) fail(name);
}

function randomItem(items) {
    return items[Math.floor(Math.random() * items.length)];
}

function cents(amount) {
    return Math.round(Number(amount) * 100);
}

function session() {
    const account = accounts[exec.vu.idInTest - 1];
    expect('账号数据完整', Boolean(account?.userId && account?.token && account?.tokenHeader));
    const headers = {
        'Content-Type': 'application/json',
        [account.tokenHeader]: account.token,
    };

    function api(method, path, payload = null, name = `${method} ${path.split('?')[0]}`) {
        const response = http.request(method, `${baseUrl}${path}`,
            payload === null ? null : JSON.stringify(payload),
            { headers, timeout: '10s', redirects: 0, tags: { name } });
        expect(`${name}: HTTP 200`, response.status === 200);
        let body;
        try {
            body = response.json();
        } catch {
            // 由下面的 check 记录非法 JSON。
        }
        if (body?.code !== 1) {
            console.error(JSON.stringify({
                time: new Date().toISOString(),
                scenario: exec.scenario.name,
                method,
                path,
                status: response.status,
                code: body?.code,
                msg: body?.msg,
                durationMs: response.timings.duration,
                traceId: response.headers['X-Trace-Id'],
            }));
        }
        expect(`${name}: code == 1`, body?.code === 1);
        return body.data;
    }
    return { account, api };
}

export function browse() {
    const { api } = session();
    const categoryList = api('GET', '/user/category/list?type=1');
    expect('分类列表非空', Array.isArray(categoryList) && categoryList.length > 0);
    for (let i = 0; i < 3; i++) {
        const categoryId = randomItem(categoryList).id;
        const dishes = api('GET', `/user/dish/list?categoryId=${encodeURIComponent(categoryId)}`);
        expect('菜品列表非空', Array.isArray(dishes) && dishes.length > 0);
    }
}

export function cart() {
    const { api } = session();
    const categoryList = api('GET', '/user/category/list?type=1');
    expect('分类列表非空', Array.isArray(categoryList) && categoryList.length > 0);
    for (let i = 0; i < 3; i++) {
        const categoryId = randomItem(categoryList).id;
        const dishes = api('GET', `/user/dish/list?categoryId=${encodeURIComponent(categoryId)}`);
        expect('菜品列表非空', Array.isArray(dishes) && dishes.length > 0);
        const dish = randomItem(dishes);
        const dishFlavor = (dish.flavors || []).map(flavor => {
            let values = JSON.parse(flavor.value);
            expect('口味选项有效', Array.isArray(values) && typeof values[0] === 'string');
            return values[0];
        }).join(',');
        api('POST', '/user/shoppingCart/add', { dishId: dish.id, dishFlavor });
        api('POST', '/user/shoppingCart/sub', { dishId: dish.id, dishFlavor });
    }
}

export function order() {
    const { account, api } = session();
    api('DELETE', '/user/shoppingCart/clean');
    const categoryList = api('GET', '/user/category/list?type=1');
    expect('分类列表非空', Array.isArray(categoryList) && categoryList.length > 0);
    for (let i = 0; i < 3; i++) {
        const categoryId = randomItem(categoryList).id;
        const dishes = api('GET', `/user/dish/list?categoryId=${encodeURIComponent(categoryId)}`);
        expect('菜品列表非空', Array.isArray(dishes) && dishes.length > 0);
        const dish = randomItem(dishes);
        const dishFlavor = (dish.flavors || []).map(flavor => {
            let values = JSON.parse(flavor.value);
            expect('口味选项有效', Array.isArray(values) && typeof values[0] === 'string');
            return values[0];
        }).join(',');
        api('POST', '/user/shoppingCart/add', { dishId: dish.id, dishFlavor });
    }
    const cartItems = api('GET', '/user/shoppingCart/list');
    expect('购物车菜品总份数为 3', Array.isArray(cartItems)
        && cartItems.reduce((sum, item) => sum + item.number, 0) === 3);
    const defaultAddress = api('GET', '/user/addressBook/default');
    expect('默认地址存在', Boolean(defaultAddress?.id));
    const amount = (cartItems.reduce((sum, item) => sum + cents(item.amount) * item.number, 0)
        + cents(packAmount)) / 100;

    // 与 smoke 相同：运行机器本地时间 + 30 分钟。
    const time = new Date(Date.now() + 30 * 60 * 1000);
    const pad = n => String(n).padStart(2, '0');
    const estimatedDeliveryTime =
        `${time.getFullYear()}-${pad(time.getMonth() + 1)}-${pad(time.getDate())} ` +
        `${pad(time.getHours())}:${pad(time.getMinutes())}:${pad(time.getSeconds())}`;
    const submitted = api('POST', '/user/order/submit', {
        addressBookId: defaultAddress.id,
        payMethod: 1,
        remark: 'k6 mixed load',
        deliveryStatus: 1,
        tablewareNumber: 3,
        tablewareStatus: 1,
        packAmount,
        amount,
        estimatedDeliveryTime,
    });
    expect('下单返回订单号', typeof submitted?.orderNumber === 'string' && submitted.orderNumber.length > 0);
    const detail = api('GET', `/user/order/${encodeURIComponent(submitted.orderNumber)}`,
        null, 'GET /user/order/{orderNumber}');
    expect('订单属于当前账号及地址', Boolean(detail)
        && detail.number === submitted.orderNumber
        && String(detail.userId) === String(account.userId)
        && String(detail.addressBookId) === String(defaultAddress.id));
    // 同一道菜被重复选中时会合并明细，所以检查总份数，而非明细条数。
    expect('订单菜品总份数为 3', Array.isArray(detail.orderDetailList)
        && detail.orderDetailList.reduce((sum, item) => sum + item.number, 0) === 3);
}
