export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const LOGIN_EMAIL = __ENV.LOGIN_EMAIL || '';
export const LOGIN_PASSWORD = __ENV.LOGIN_PASSWORD || '';

export const CHAT_ROOM_ID = __ENV.CHAT_ROOM_ID || '';
export const PARTY_ID = __ENV.PARTY_ID || '';
export const TARGET_USER_IDS = (__ENV.TARGET_USER_IDS || '')
    .split(',')
    .map((value) => value.trim())
    .filter(Boolean)
    .map(Number);

function parseCredentials(rawValue) {
    return (rawValue || '')
        .split(',')
        .map((value) => value.trim())
        .filter(Boolean)
        .map((entry) => {
            const separatorIndex = entry.indexOf(':');
            if (separatorIndex === -1) {
                return null;
            }

            const email = entry.slice(0, separatorIndex).trim();
            const password = entry.slice(separatorIndex + 1).trim();
            if (!email || !password) {
                return null;
            }

            return { email, password };
        })
        .filter(Boolean);
}

function parseIdList(rawValue) {
    return (rawValue || '')
        .split(',')
        .map((value) => value.trim())
        .filter(Boolean);
}

function parseNumber(rawValue, fallback) {
    const parsed = Number.parseFloat(rawValue);
    return Number.isFinite(parsed) ? parsed : fallback;
}

function getMappedValueForVU(vuId, values, fallback) {
    if (values.length > 0) {
        const normalizedVuId = Math.max(1, Number(vuId) || 1);
        return values[(normalizedVuId - 1) % values.length];
    }

    return fallback;
}

export const TEST_CREDENTIALS = parseCredentials(__ENV.TEST_CREDENTIALS || '');
export const TEST_CHAT_ROOM_IDS = parseIdList(__ENV.TEST_CHAT_ROOM_IDS || '');
export const TEST_PARTY_IDS = parseIdList(__ENV.TEST_PARTY_IDS || '');

export const HAS_FALLBACK_AUTH_CREDENTIALS = !!(LOGIN_EMAIL && LOGIN_PASSWORD);
export const HAS_AUTH_CREDENTIALS = TEST_CREDENTIALS.length > 0 || HAS_FALLBACK_AUTH_CREDENTIALS;
export const HAS_CHAT_ROOM_TARGETS = !!(CHAT_ROOM_ID || TEST_CHAT_ROOM_IDS.length > 0);
export const HAS_PARTY_TARGETS = !!(PARTY_ID || TEST_PARTY_IDS.length > 0);
export const HAS_PARTY_WRITE_TARGETS = !!(HAS_PARTY_TARGETS && TARGET_USER_IDS.length > 0);

export const ENABLE_PARTY_WRITE = (__ENV.ENABLE_PARTY_WRITE || 'false') === 'true';
export const TEST_MODE = __ENV.TEST_MODE || 'mixed';
export const RUN_ID = __ENV.RUN_ID || 'manual';

export const LOAD_PROFILE = __ENV.LOAD_PROFILE || 'baseline';

const PROFILE_MULTIPLIERS = {
    baseline: { vu: 0.5, rate: 0.5 },
    high: { vu: 1.0, rate: 1.0 },
    stress: { vu: 2.0, rate: 2.0 },
};

export const LOAD_MULTIPLIERS = PROFILE_MULTIPLIERS[LOAD_PROFILE] || PROFILE_MULTIPLIERS.baseline;

// Mixed mode weights control relative scenario intensity, not exact traffic percentages.
export const SCENARIO_WEIGHTS = {
    posts_list_public: parseNumber(__ENV.WEIGHT_POSTS || __ENV.RATIO_POSTS, 0.70),
    auth_login: parseNumber(__ENV.WEIGHT_AUTH || __ENV.RATIO_AUTH, 0.10),
    chat_rooms_read: parseNumber(__ENV.WEIGHT_CHAT_ROOMS || __ENV.RATIO_CHAT_ROOMS, 0.04),
    chat_messages_read: parseNumber(__ENV.WEIGHT_CHAT_MSGS || __ENV.RATIO_CHAT_MSGS, 0.11),
    party_members_read: parseNumber(__ENV.WEIGHT_PARTY_READ || __ENV.RATIO_PARTY_READ, 0.03),
    party_add_members_write: parseNumber(__ENV.WEIGHT_PARTY_WRITE || __ENV.RATIO_PARTY_WRITE, 0.02),
};

export function getCredentialsForVU(vuId) {
    if (TEST_CREDENTIALS.length > 0) {
        const normalizedVuId = Math.max(1, Number(vuId) || 1);
        return TEST_CREDENTIALS[(normalizedVuId - 1) % TEST_CREDENTIALS.length];
    }

    return { email: LOGIN_EMAIL, password: LOGIN_PASSWORD };
}

export function getChatRoomIdForVU(vuId) {
    return getMappedValueForVU(vuId, TEST_CHAT_ROOM_IDS, CHAT_ROOM_ID);
}

export function getPartyIdForVU(vuId) {
    return getMappedValueForVU(vuId, TEST_PARTY_IDS, PARTY_ID);
}
