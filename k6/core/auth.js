import http from 'k6/http';
import { check, fail } from 'k6';

import { BASE_URL, LOGIN_EMAIL, LOGIN_PASSWORD, getCredentialsForVU } from './env.js';
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

function getLeaderCredentials() {
    return {
        email: LOGIN_EMAIL,
        password: LOGIN_PASSWORD,
    };
}

export function jsonParams(tags = {}) {
    return {
        headers: {
            'Content-Type': 'application/json',
        },
        tags,
    };
}

export function authedParams(tags = {}) {
    if (!authState.authCookieHeader) {
        fail('Authenticated request attempted without auth cookies.');
    }

    return {
        headers: {
            Cookie: authState.authCookieHeader,
        },
        tags,
    };
}

export function login(credentials = getCredentialsForVU(__VU)) {
    ensureEnv(credentials.email, 'credential email');
    ensureEnv(credentials.password, 'credential password');

    const res = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            email: credentials.email,
            password: credentials.password,
        }),
        jsonParams({ endpoint: 'auth_login' }),
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

    const jar = http.cookieJar();
    jar.set(BASE_URL, 'accessToken', accessToken, { path: '/' });

    if (refreshToken) {
        jar.set(BASE_URL, 'refreshToken', refreshToken, { path: '/' });
    }

    authState.authCookieHeader = buildCookieHeader(accessToken, refreshToken);
    authState.loggedIn = true;
    authState.authenticatedEmail = credentials.email;
    return res;
}

export function ensureAuthenticated(credentials = getCredentialsForVU(__VU)) {
    if (authState.loggedIn && authState.authenticatedEmail === credentials.email) {
        return;
    }

    login(credentials);
}

export function ensureLeaderAuthenticated() {
    ensureAuthenticated(getLeaderCredentials());
}
