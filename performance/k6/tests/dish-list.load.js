import http from 'k6/http';
import { check } from 'k6';

const baseUrl = 'http://localhost:8080';
const categoryId = '16';

export const options = {
    scenarios: {
        dish_list_load: {
            executor: 'constant-arrival-rate',
            rate: 1000,
            timeUnit: '1s',
            duration: '5m',
            preAllocatedVUs: 25,
            maxVUs: 50,
            gracefulStop: '15s',
        },
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
    // Enforce correctness and delivery of the scheduled load, not a latency SLO.
    thresholds: {
        http_reqs: ['count>0'],
        http_req_failed: ['rate==0'],
        checks: ['rate==1'],
        dropped_iterations: ['count==0'],
    },
};

export default function () {
    // Exactly one request per iteration: no retries, redirects, or pacing sleep.
    const response = http.get(
        `${baseUrl}/user/dish/list?categoryId=${encodeURIComponent(categoryId)}`,
        {
            timeout: '10s',
            redirects: 0,
            tags: { name: 'GET /user/dish/list' },
        },
    );

    let body;
    let validJson = false;
    try {
        body = response.json();
        validJson = true;
    } catch {
        // Keep malformed responses visible as failed checks.
    }

    check(response, {
        'HTTP status is 200': (res) => res.status === 200,
        'response is valid JSON': () => validJson,
        'business code is 1': () => body != null && body.code === 1,
        'data is an array': () => body != null && Array.isArray(body.data),
        'data contains dishes': () =>
            body != null && Array.isArray(body.data) && body.data.length > 0,
    });
}
