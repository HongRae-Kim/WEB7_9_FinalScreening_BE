import http from 'k6/http';
import { check, fail } from 'k6';

import {
    BASE_URL,
    ENABLE_PARTY_WRITE,
    HAS_AUTH_CREDENTIALS,
    HAS_CHAT_ROOM_TARGETS,
    HAS_PARTY_TARGETS,
    TARGET_USER_IDS,
    TEST_MODE,
} from '../core/env.js';

const PROTECTED_TEST_MODES = new Set(['chat_rooms', 'chat_messages', 'party_members', 'party_write']);

function requiresProtectedAuth() {
    return PROTECTED_TEST_MODES.has(TEST_MODE) || (TEST_MODE === 'mixed' && HAS_AUTH_CREDENTIALS);
}

function validateTargets() {
    const requiresChatRoomId = TEST_MODE === 'chat_messages' || (TEST_MODE === 'mixed' && HAS_AUTH_CREDENTIALS);
    const requiresPartyId = ['party_members', 'party_write'].includes(TEST_MODE)
        || (TEST_MODE === 'mixed' && (HAS_AUTH_CREDENTIALS || ENABLE_PARTY_WRITE) && HAS_AUTH_CREDENTIALS);
    const requiresWriteTargets = TEST_MODE === 'party_write' || (TEST_MODE === 'mixed' && ENABLE_PARTY_WRITE && HAS_AUTH_CREDENTIALS);

    if (requiresChatRoomId && !HAS_CHAT_ROOM_TARGETS) {
        fail('CHAT_ROOM_ID or TEST_CHAT_ROOM_IDS is required.');
    }

    if (requiresPartyId && !HAS_PARTY_TARGETS) {
        fail('PARTY_ID or TEST_PARTY_IDS is required.');
    }

    if (requiresWriteTargets && TARGET_USER_IDS.length === 0) {
        fail('TARGET_USER_IDS is required');
    }
}

export function setup() {
    const res = http.get(`${BASE_URL}/actuator/health`);

    const serverIsUp = check(res, {
        'server is up': (r) => r.status === 200,
    });

    if (!serverIsUp) {
        fail(`health check failed: status=${res.status}, body=${res.body}`);
    }

    if (TEST_MODE === 'auth' && !HAS_AUTH_CREDENTIALS) {
        fail('auth mode requires TEST_CREDENTIALS or LOGIN_EMAIL and LOGIN_PASSWORD.');
    }

    if (requiresProtectedAuth() && !HAS_AUTH_CREDENTIALS) {
        fail('Protected API tests require TEST_CREDENTIALS or LOGIN_EMAIL and LOGIN_PASSWORD.');
    }

    validateTargets();
    return { startedAt: new Date().toISOString() };
}
