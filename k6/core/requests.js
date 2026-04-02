import http from 'k6/http';

import { ensureAuthenticated, getAuthCookieHeader } from './auth.js';
import { RUN_ID, SCENARIO_FAMILY } from './env.js';
import { recordBusinessOutcome } from './metrics.js';
import { thinkTime, runStatusChecks } from './utils.js';

function requestTags(endpoint) {
    return {
        endpoint,
        scenario_family: SCENARIO_FAMILY,
    };
}

export function runGet({ url, endpoint, checkName, credentials }) {
    if (credentials) {
        ensureAuthenticated(credentials);
    }

    const res = http.get(
        url,
        credentials
            ? {
                headers: {
                    Cookie: getAuthCookieHeader(),
                },
                tags: requestTags(endpoint),
            }
            : { tags: requestTags(endpoint) },
    );

    runStatusChecks(res, endpoint, checkName, [200], {
        runId: RUN_ID,
        method: 'GET',
        url,
    });

    thinkTime();
    return res;
}

export function runPostJson({
    url,
    endpoint,
    body,
    checkName,
    credentials,
    allowedStatuses,
    successStatuses = [200],
    businessStatusMap = {},
}) {
    ensureAuthenticated(credentials);

    const responseCallback = allowedStatuses?.length
        ? http.expectedStatuses(...allowedStatuses)
        : null;

    const res = http.post(
        url,
        JSON.stringify(body),
        {
            headers: {
                'Content-Type': 'application/json',
                Cookie: getAuthCookieHeader(),
            },
            tags: requestTags(endpoint),
            ...(responseCallback ? { responseCallback } : {}),
        },
    );

    runStatusChecks(res, endpoint, checkName, allowedStatuses, {
        runId: RUN_ID,
        method: 'POST',
        url,
        requestBody: body,
    });

    if (successStatuses.length > 0 || Object.keys(businessStatusMap).length > 0) {
        recordBusinessOutcome(endpoint, res, successStatuses, businessStatusMap);
    }

    thinkTime();
    return res;
}
