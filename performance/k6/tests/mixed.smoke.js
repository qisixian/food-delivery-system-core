import http from 'k6/http';
import { check, fail } from 'k6';
import exec from 'k6/execution';
import { SharedArray } from 'k6/data';
import { Counter } from 'k6/metrics';

const baseUrl = 'http://localhost:8080';

const accounts = new SharedArray('loadtest accounts', () => {
    const data = JSON.parse(open('../data/loadtest-users.json'));
    return data.users.map(user => ({ ...user, tokenHeader: data.tokenHeader }));
});
const completedFlows = new Counter('completed_flows');

export const options = {
    scenarios: {
        order_smoke: {
            executor: 'per-vu-iterations',
            vus: 1,
            iterations: 1,
            maxDuration: '2m',
            gracefulStop: '10s',
        },
    },
    thresholds: {
        http_reqs: ['count>0'],
        http_req_failed: ['rate==0'],
        checks: ['rate==1'],
        completed_flows: ['count==1'],
    },
};

function expect(name, condition) {
    if (!check(null, { [name]: () => condition })) {
        fail(name);
    }
}

function cents(amount) {
    return Math.round(Number(amount) * 100);
}

export default function () {
    completedFlows.add(0);
    const account = accounts[exec.vu.idInTest - 1];
    expect('账号数据完整', Boolean(account?.userId && account?.token && account?.tokenHeader));

    const headers = {
        'Content-Type': 'application/json',
        [account.tokenHeader]: account.token,
    };

    // 任一步失败就停止本次流程，避免继续提交无效请求。
    function api(method, path, payload = null, name = `${method} ${path}`) {
        const response = http.request(method, `${baseUrl}${path}`,
            payload === null ? null : JSON.stringify(payload),
            { headers, timeout: '10s', redirects: 0, tags: { name } });
        expect(`${name}: HTTP 200`, response.status === 200);
        let body;
        try {
            body = response.json();
        } catch {
            // 由下面的 check 记录失败。
        }
        expect(`${name}: 返回合法 JSON`, body !== undefined && body !== null);
        expect(`${name}: code == 1`, body.code === 1);

        return body.data;
    }

    // 1.1 浏览分类
    const categories = api('GET', '/user/category/list?type=1');

    expect('分类列表非空', Array.isArray(categories) && categories.length > 0);

    const categoryId = categories[0].id;

    // 1.2 浏览分类下的菜品
    const dishes = api('GET', `/user/dish/list?categoryId=${encodeURIComponent(categoryId)}`);
    expect('菜品列表非空', Array.isArray(dishes) && dishes.length > 0);

    const dish = dishes[0];

    // 1.3 确定口味：没有选项时使用空字符串，有选项时取每组口味的第一个选项并拼接。
    const dishFlavor = (dish.flavors || []).map(flavor => {
        let values;
        values = JSON.parse(flavor.value);
        expect('菜品口味包含有效选项', Array.isArray(values) && values.length > 0 && typeof values[0] === 'string');
        return values[0];
    }).join(',');

    // 2.1 清空当前测试账号的购物车，。
    api('DELETE', '/user/shoppingCart/clean');

    // 2.2 再加入一份菜品
    api('POST', '/user/shoppingCart/add', { dishId: dish.id, dishFlavor });

    // 2.3 查询购物车，验证只有一条菜品记录，且菜品、口味、数量正确。
    const cart = api('GET', '/user/shoppingCart/list');

    expect('购物车只有一条菜品记录', Array.isArray(cart) && cart.length === 1);
    expect('购物车菜品、口味、数量正确',
        String(cart[0].dishId) === String(dish.id)
        && cart[0].dishFlavor === dishFlavor
        && cart[0].number === 1);

    // 2.4 查询默认地址
    const defaultAddress = api('GET', '/user/addressBook/default');

    // 默认地址 id 存在
    expect('默认地址 id 存在', Boolean(defaultAddress?.id));


    // 3. 用本账号的地址提交订单，金额按本次购物车加打包费计算。
    const packAmount = 3;
    const amount = (cents(cart[0].amount) * cart[0].number + cents(packAmount)) / 100;

    // 预计送达时间为当前时间 + 30 分钟，格式为 YYYY-MM-DD HH:mm:ss
    const deliveryTime = new Date(Date.now() + 30 * 60 * 1000);
    const pad = n => String(n).padStart(2, '0');
    const estimatedDeliveryTime =
        `${deliveryTime.getFullYear()}-${pad(deliveryTime.getMonth() + 1)}-${pad(deliveryTime.getDate())} ` +
        `${pad(deliveryTime.getHours())}:${pad(deliveryTime.getMinutes())}:${pad(deliveryTime.getSeconds())}`;

    const submitted = api('POST', '/user/order/submit', {
        addressBookId: defaultAddress.id,
        payMethod: 1,
        remark: 'k6 mixed smoke',
        deliveryStatus: 1,
        tablewareNumber: 1,
        tablewareStatus: 1,
        packAmount: packAmount,
        amount: amount,
        estimatedDeliveryTime: estimatedDeliveryTime,
    });
    expect('下单返回订单 ID 和订单号', Boolean(submitted?.id)
        && typeof submitted?.orderNumber === 'string' && submitted.orderNumber.length > 0);


    // 4. 详情接口使用订单号，不是订单 ID。
    const detail = api('GET', `/user/order/${encodeURIComponent(submitted.orderNumber)}`);

    // console.log(JSON.stringify(detail, null, 2));

    expect('详情与本次订单、账号及地址一致', Boolean(detail)
        && String(detail.id) === String(submitted.id)
        && String(detail.userId) === account.userId
        && String(detail.addressBookId) === String(defaultAddress.id));
    expect('订单待付款且未支付', detail.status === 1 && detail.payStatus === 0);
    expect('订单包含一条明细', Array.isArray(detail.orderDetailList)
        && detail.orderDetailList.length === 1);

    completedFlows.add(1);
    console.log(`流程完成：userId=${account.userId}，orderNumber=${submitted.orderNumber}`);
}
