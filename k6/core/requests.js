import http from 'k6/http';

import { authedParams, ensureAuthenticated } from './auth.js';
import { RUN_ID } from './env.js';
import { thinkTime, runStatusChecks } from './utils.js';

export function runGet({ url, endpoint, checkName, authenticated = false }) {
    if (authenticated) {
        ensureAuthenticated();
    }

    const res = http.get(
        url,
        authenticated ? authedParams({ endpoint }) : { tags: { endpoint } },
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
    allowedStatuses,
    ensureAuth = true,
}) {
    if (ensureAuth) {
        ensureAuthenticated();
    }

    const responseCallback = allowedStatuses?.length
        ? http.expectedStatuses(...allowedStatuses)
        : null;

    const res = http.post(
        url,
        JSON.stringify(body),
        {
            headers: {
                'Content-Type': 'application/json',
                Cookie: authedParams().headers.Cookie,
            },
            tags: { endpoint },
            ...(responseCallback ? { responseCallback } : {}),
        },
    );

    runStatusChecks(res, endpoint, checkName, allowedStatuses, {
        runId: RUN_ID,
        method: 'POST',
        url,
        requestBody: body,
    });

    thinkTime();
    return res;
}
