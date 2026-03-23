import { TEST_MODE } from '../core/env.js';

const COMMON_THRESHOLDS = {
    http_req_failed: ['rate<0.05'],
};

const THRESHOLD_PRESETS = {
    posts: {
        'http_req_duration{scenario:posts_list_public}': ['p(95)<800', 'p(99)<1500'],
        'http_req_duration{endpoint:posts_list}': ['p(95)<800', 'p(99)<1500'],
    },
    auth: {
        'http_req_duration{scenario:auth_login}': ['p(95)<700'],
        'http_req_duration{endpoint:auth_login}': ['p(95)<700'],
    },
    chat_rooms: {
        'http_req_duration{scenario:chat_rooms_read}': ['p(95)<900'],
        'http_req_duration{endpoint:chat_rooms}': ['p(95)<900'],
    },
    chat_messages: {
        'http_req_duration{scenario:chat_messages_read}': ['p(95)<1000'],
        'http_req_duration{endpoint:chat_messages}': ['p(95)<1000'],
    },
    party_members: {
        'http_req_duration{scenario:party_members_read}': ['p(95)<800'],
        'http_req_duration{endpoint:party_members}': ['p(95)<800'],
    },
    party_write: {
        'http_req_duration{scenario:party_add_members_write}': ['p(95)<1200'],
        'http_req_duration{endpoint:party_add_members}': ['p(95)<1200'],
    },
    mixed: {
        'http_req_duration{endpoint:posts_list}': ['p(95)<800', 'p(99)<1500'],
        'http_req_duration{endpoint:auth_login}': ['p(95)<700'],
        'http_req_duration{endpoint:chat_rooms}': ['p(95)<900'],
        'http_req_duration{endpoint:chat_messages}': ['p(95)<1000'],
        'http_req_duration{endpoint:party_members}': ['p(95)<800'],
        'http_req_duration{endpoint:party_add_members}': ['p(95)<1200'],
    },
};

export const thresholds = {
    ...COMMON_THRESHOLDS,
    ...(THRESHOLD_PRESETS[TEST_MODE] || THRESHOLD_PRESETS.mixed),
};
