import { IS_CONTENTION_PRESET, MIN_WRITE_SUCCESS_SAMPLES } from '../core/env.js';

const REALISTIC_THRESHOLDS = {
    http_req_failed: ['rate<0.01'],
    dropped_iterations: ['count==0'],
    'http_req_duration{endpoint:posts_list}': ['p(95)<50'],
    'http_req_duration{endpoint:auth_login}': ['p(95)<180'],
    'http_req_duration{endpoint:chat_rooms}': ['p(95)<50'],
    'http_req_duration{endpoint:chat_messages}': ['p(95)<50'],
    'http_req_duration{endpoint:party_members}': ['p(95)<50'],
    'endpoint_success_duration{endpoint:party_add_members}': ['p(95)<40'],
    'business_result_count{endpoint:party_add_members,business_result:success}': [`count>=${Math.max(1, MIN_WRITE_SUCCESS_SAMPLES)}`],
};

const CONTENTION_THRESHOLDS = {
    http_req_failed: ['rate<0.05'],
    'http_req_duration{endpoint:party_add_members}': ['p(95)<1200'],
    'endpoint_success_duration{endpoint:party_add_members}': ['p(95)<200'],
    'business_result_count{endpoint:party_add_members,business_result:success}': ['count>0'],
};

export const thresholds = IS_CONTENTION_PRESET
    ? CONTENTION_THRESHOLDS
    : REALISTIC_THRESHOLDS;
