import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = 'http://localhost:8080';
const categoryId = '16';

export const options = {
    scenarios: {
        dish_list_smoke: {
            executor: 'constant-vus',
            vus: 1,
            duration: '10s',
            gracefulStop: '10s',
        },
    },
    thresholds: {
        http_reqs: ['count>0'],
        http_req_failed: ['rate==0'],
        checks: ['rate==1'],
    },
};

export default function () {
    const response = http.get(
        `${baseUrl}/user/dish/list?categoryId=${encodeURIComponent(categoryId)}`,
        {
            timeout: '10s',
            tags: { name: 'GET /user/dish/list' },
        },
    );

    let body;
    let validJson = false;
    try {
        body = response.json();
        validJson = true;
    } catch {
        // Record malformed responses as failed checks instead of aborting the iteration.
    }

    check(response, {
        'HTTP status is 200': (res) => res.status === 200,
        'response is valid JSON': () => validJson,
        'business code is 1': () => body != null && body.code === 1,
        'data is an array': () => body != null && Array.isArray(body.data),
        'data contains dishes': () =>
            body != null && Array.isArray(body.data) && body.data.length > 0,
    });

    sleep(1);
}
