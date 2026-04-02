import http from 'k6/http';
import { check, fail } from 'k6';

import { BASE_URL, SCENARIO_FAMILY } from './env.js';
import { ensureEnv, safeJson } from './utils.js';

const authState = {
    loggedIn: false,
    authCookieHeader: '',
    authenticatedEmail: '',
};

function buildCookieHeader(accessToken, refreshToken) {
    return refreshToken
        ? `accessToken=${accessToken}; refreshToken=${refreshToken}`
        : `accessToken=${accessToken}`;
}

function buildTags(tags = {}) {
    return {
        scenario_family: SCENARIO_FAMILY,
        ...tags,
    };
}

export function jsonParams(tags = {}) {
    return {
        headers: {
            'Content-Type': 'application/json',
        },
        tags: buildTags(tags),
    };
}

export function getAuthCookieHeader() {
    if (!authState.authCookieHeader) {
        fail('Authenticated request attempted without auth cookies.');
    }

    return authState.authCookieHeader;
}

export function authedParams(tags = {}) {
    return {
        headers: {
            Cookie: getAuthCookieHeader(),
        },
        tags: buildTags(tags),
    };
}

function createCookieJarSession(accessToken, refreshToken) {
    const jar = http.cookieJar();
    jar.set(BASE_URL, 'accessToken', accessToken, { path: '/' });

    if (refreshToken) {
        jar.set(BASE_URL, 'refreshToken', refreshToken, { path: '/' });
    }
}

function performLogin(credentials, endpointTag) {
    ensureEnv(credentials.email, 'credential email');
    ensureEnv(credentials.password, 'credential password');

    const res = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            email: credentials.email,
            password: credentials.password,
        }),
        jsonParams({ endpoint: endpointTag }),
    );

    const ok = check(res, {
        'login status is 200': (r) => r.status === 200,
    });

    if (!ok) {
        fail(`login failed: status=${res.status}, body=${res.body}`);
    }

    const body = safeJson(res);
    const accessToken =
        res.cookies?.accessToken?.[0]?.value ||
        body?.accessToken ||
        null;
    const refreshToken =
        res.cookies?.refreshToken?.[0]?.value ||
        body?.refreshToken ||
        null;

    if (!accessToken) {
        fail(`login succeeded but accessToken cookie/token was not available: body=${res.body}`);
    }

    return {
        res,
        email: credentials.email,
        userId: body?.user?.userId ?? null,
        authCookieHeader: buildCookieHeader(accessToken, refreshToken),
        accessToken,
        refreshToken,
    };
}

export function createDiscoverySession(credentials) {
    return performLogin(credentials, 'setup_auth_login');
}

export function login(credentials) {
    const session = performLogin(credentials, 'auth_login');

    createCookieJarSession(session.accessToken, session.refreshToken);
    authState.authCookieHeader = session.authCookieHeader;
    authState.loggedIn = true;
    authState.authenticatedEmail = session.email;

    return session.res;
}

export function ensureAuthenticated(credentials) {
    if (authState.loggedIn && authState.authenticatedEmail === credentials.email) {
        return;
    }

    login(credentials);
}
