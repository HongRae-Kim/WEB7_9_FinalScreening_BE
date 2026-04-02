import { Counter, Trend } from 'k6/metrics';

import { SCENARIO_FAMILY } from './env.js';

export const businessResultCount = new Counter('business_result_count');
export const businessResultDuration = new Trend('business_result_duration', true);
export const endpointSuccessDuration = new Trend('endpoint_success_duration', true);

export function recordBusinessOutcome(endpoint, res, successStatuses = [200], businessStatusMap = {}) {
    const businessResult = successStatuses.includes(res.status)
        ? 'success'
        : (businessStatusMap[res.status] || `handled_${res.status}`);

    const tags = {
        endpoint,
        business_result: businessResult,
        scenario_family: SCENARIO_FAMILY,
    };

    businessResultCount.add(1, tags);
    businessResultDuration.add(res.timings.duration, tags);

    if (businessResult === 'success') {
        endpointSuccessDuration.add(res.timings.duration, {
            endpoint,
            scenario_family: SCENARIO_FAMILY,
        });
    }
}
