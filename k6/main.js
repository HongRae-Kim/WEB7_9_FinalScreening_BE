import http from 'k6/http';
import { check, fail, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const LOGIN_EMAIL = __ENV.LOGIN_EMAIL || '';
const LOGIN_PASSWORD = __ENV.LOGIN_PASSWORD || '';

const CHAT_ROOM_ID = __ENV.CHAT_ROOM_ID || '';
const PARTY_ID = __ENV.PARTY_ID || '';
const TARGET_USER_IDS = (__ENV.TARGET_USER_IDS || '')
    .split(',')
    .map((v) => v.trim())
    .filter(Boolean)
    .map(Number);

const HAS_AUTH_CREDENTIALS = !!(LOGIN_EMAIL && LOGIN_PASSWORD);
const HAS_PARTY_WRITE_TARGETS = !!(PARTY_ID && TARGET_USER_IDS.length > 0);

const ENABLE_AUTH_SCENARIOS = (__ENV.ENABLE_AUTH_SCENARIOS || (HAS_AUTH_CREDENTIALS ? 'true' : 'false')) === 'true';
const ENABLE_PARTY_WRITE = (__ENV.ENABLE_PARTY_WRITE || 'false') === 'true';

// VU별 로그인 상태 저장
let loggedIn = false;
let authCookieHeader = '';

function safeJson(res) {
    try {
        return res.json();
    } catch (e) {
        return null;
    }
}

function logUnexpected(name, res) {
    console.error(`${name} failed: status=${res.status}, body=${res.body}`);
}

function getAccessibleChatRoomId() {
    const res = http.get(
        `${BASE_URL}/api/v1/chats?size=20`,
        authedParams({ endpoint: 'chat_rooms_lookup' }),
    );

    if (res.status !== 200) {
        logUnexpected('chat_rooms_lookup', res);
        return null;
    }

    const body = safeJson(res);
    const rooms = body?.chatRooms || body?.data?.chatRooms || body?.data || [];

    if (!Array.isArray(rooms) || rooms.length === 0) {
        return null;
    }

    return rooms[0].chatRoomId || rooms[0].id || null;
}

function getAccessiblePartyId() {
    const res = http.get(
        `${BASE_URL}/api/v1/users/me/parties`,
        authedParams({ endpoint: 'my_parties_lookup' }),
    );

    if (res.status !== 200) {
        logUnexpected('my_parties_lookup', res);
        return null;
    }

    const body = safeJson(res);
    const parties = body?.data?.parties || body?.data?.myParties || body?.data || [];

    if (!Array.isArray(parties) || parties.length === 0) {
        return null;
    }

    return parties[0].partyId || parties[0].id || null;
}

function buildScenarios() {
    const scenarios = {
        posts_list_public: {
            executor: 'ramping-arrival-rate',
            exec: 'postsListPublic',
            startRate: 5,
            timeUnit: '1s',
            preAllocatedVUs: 10,
            maxVUs: 50,
            stages: [
                { target: 40, duration: '30s' },
                { target: 80, duration: '1m' },
                { target: 120, duration: '1m' },
                { target: 0, duration: '20s' },
            ],
            tags: { area: 'posts', kind: 'public-read' },
        },
    };

    if (ENABLE_AUTH_SCENARIOS && HAS_AUTH_CREDENTIALS) {
        scenarios.auth_login = {
            executor: 'constant-arrival-rate',
            exec: 'loginOnly',
            rate: 10,
            timeUnit: '1s',
            duration: '1m',
            preAllocatedVUs: 5,
            maxVUs: 20,
            startTime: '5s',
            tags: { area: 'auth', kind: 'baseline' },
        };

        scenarios.chat_rooms_read = {
            executor: 'constant-vus',
            exec: 'chatRoomsRead',
            vus: 30,
            duration: '1m',
            startTime: '15s',
            tags: { area: 'chat', kind: 'auth-read' },
        };

        scenarios.chat_messages_read = {
            executor: 'constant-vus',
            exec: 'chatMessagesRead',
            vus: 30,
            duration: '1m',
            startTime: '25s',
            tags: { area: 'chat', kind: 'auth-read' },
        };

        scenarios.party_members_read = {
            executor: 'constant-vus',
            exec: 'partyMembersRead',
            vus: 20,
            duration: '1m',
            startTime: '35s',
            tags: { area: 'party', kind: 'auth-read' },
        };
    }

    if (ENABLE_PARTY_WRITE && HAS_AUTH_CREDENTIALS && HAS_PARTY_WRITE_TARGETS) {
        scenarios.party_add_members_write = {
            executor: 'per-vu-iterations',
            exec: 'partyAddMembersWrite',
            vus: 3,
            iterations: 5,
            maxDuration: '1m',
            startTime: '45s',
            tags: { area: 'party', kind: 'write-contention' },
        };
    }

    return scenarios;
}

export const options = {
    scenarios: buildScenarios(),
    thresholds: {
        http_req_failed: ['rate<0.05'],
        'http_req_duration{scenario:posts_list_public}': ['p(95)<800', 'p(99)<1500'],
        'http_req_duration{scenario:auth_login}': ['p(95)<700'],
        'http_req_duration{scenario:chat_rooms_read}': ['p(95)<900'],
        'http_req_duration{scenario:chat_messages_read}': ['p(95)<1000'],
        'http_req_duration{scenario:party_members_read}': ['p(95)<800'],
        'http_req_duration{scenario:party_add_members_write}': ['p(95)<1200'],
    },
};

function ensureEnv(value, name) {
    if (!value) {
        fail(`${name} is required`);
    }
}

function jsonParams(tags = {}) {
    return {
        headers: {
            'Content-Type': 'application/json',
        },
        tags,
    };
}

function authedParams(tags = {}) {
    if (!authCookieHeader) {
        fail('Authenticated request attempted without auth cookies.');
    }

    return {
        headers: {
            Cookie: authCookieHeader,
        },
        tags,
    };
}

function login() {
    ensureEnv(LOGIN_EMAIL, 'LOGIN_EMAIL');
    ensureEnv(LOGIN_PASSWORD, 'LOGIN_PASSWORD');

    const res = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            email: LOGIN_EMAIL,
            password: LOGIN_PASSWORD,
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

    const jar = http.cookieJar();
    if (accessToken) {
        jar.set(BASE_URL, 'accessToken', accessToken, { path: '/' });
    }
    if (refreshToken) {
        jar.set(BASE_URL, 'refreshToken', refreshToken, { path: '/' });
    }

    authCookieHeader = accessToken
        ? refreshToken
            ? `accessToken=${accessToken}; refreshToken=${refreshToken}`
            : `accessToken=${accessToken}`
        : '';

    if (!accessToken) {
        fail(`login succeeded but accessToken cookie/token was not available: body=${res.body}`);
    }

    return res;
}

function ensureLoggedIn() {
    if (loggedIn) {
        return;
    }

    login();
    loggedIn = true;
}

export function setup() {
    const res = http.get(`${BASE_URL}/actuator/health`);

    const serverIsUp = check(res, {
        'server is up': (r) => r.status === 200,
    });

    if (!serverIsUp) {
        fail(`health check failed: status=${res.status}, body=${res.body}`);
    }

    if (ENABLE_AUTH_SCENARIOS && !HAS_AUTH_CREDENTIALS) {
        fail('ENABLE_AUTH_SCENARIOS=true requires LOGIN_EMAIL and LOGIN_PASSWORD.');
    }

    if (ENABLE_PARTY_WRITE && !HAS_PARTY_WRITE_TARGETS) {
        fail('ENABLE_PARTY_WRITE=true requires PARTY_ID and TARGET_USER_IDS.');
    }

    return { startedAt: new Date().toISOString() };
}

export function postsListPublic() {
    const res = http.get(
        `${BASE_URL}/api/v1/posts?size=20`,
        { tags: { endpoint: 'posts_list' } },
    );

    check(res, {
        'posts list status 200': (r) => r.status === 200,
        'posts list has body': (r) => !!r.body,
    });

    sleep(1);
}

export function loginOnly() {
    login();
    sleep(1);
}

export function chatRoomsRead() {
    ensureLoggedIn();

    const res = http.get(
        `${BASE_URL}/api/v1/chats?size=20`,
        authedParams({ endpoint: 'chat_rooms' }),
    );

    const ok = check(res, {
        'chat rooms status 200': (r) => r.status === 200,
    });

    if (!ok) {
        logUnexpected('chat_rooms', res);
    }

    sleep(1);
}

export function chatMessagesRead() {
    ensureLoggedIn();

    const roomId = CHAT_ROOM_ID || getAccessibleChatRoomId();
    if (!roomId) {
        fail('No accessible chat room found. Set CHAT_ROOM_ID or seed chat rooms first.');
    }

    const res = http.get(
        `${BASE_URL}/api/v1/chats/${roomId}/messages?size=30`,
        authedParams({ endpoint: 'chat_messages' }),
    );

    const ok = check(res, {
        'chat messages status 200': (r) => r.status === 200,
    });

    if (!ok) {
        logUnexpected('chat_messages', res);
    }

    sleep(1);
}

export function partyMembersRead() {
    ensureLoggedIn();

    const partyId = PARTY_ID || getAccessiblePartyId();
    if (!partyId) {
        fail('No accessible party found. Set PARTY_ID or seed party data first.');
    }

    const res = http.get(
        `${BASE_URL}/api/v1/parties/${partyId}/members`,
        authedParams({ endpoint: 'party_members' }),
    );

    const ok = check(res, {
        'party members status 200': (r) => r.status === 200,
    });

    if (!ok) {
        logUnexpected('party_members', res);
    }

    sleep(1);
}

export function partyAddMembersWrite() {
    ensureEnv(PARTY_ID, 'PARTY_ID');

    if (TARGET_USER_IDS.length === 0) {
        fail('TARGET_USER_IDS is required');
    }

    ensureLoggedIn();

    const res = http.post(
        `${BASE_URL}/api/v1/parties/${PARTY_ID}/members`,
        JSON.stringify({
            targetUserIds: TARGET_USER_IDS,
        }),
        {
            headers: {
                'Content-Type': 'application/json',
                Cookie: authCookieHeader,
            },
            tags: { endpoint: 'party_add_members' },
        },
    );

    check(res, {
        'party add members handled': (r) => [200, 400, 409].includes(r.status),
    });

    sleep(1);
}
