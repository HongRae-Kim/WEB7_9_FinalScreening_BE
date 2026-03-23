import { fail } from 'k6';

import {
    ENABLE_PARTY_WRITE,
    HAS_AUTH_CREDENTIALS,
    HAS_PARTY_WRITE_TARGETS,
    LOAD_MULTIPLIERS,
    SCENARIO_WEIGHTS,
    TEST_MODE,
} from '../core/env.js';

const BASE_SCENARIO_CONFIG = {
    posts_list_public: {
        executor: 'ramping-arrival-rate',
        exec: 'postsListPublic',
        startRate: 30,
        timeUnit: '1s',
        preAllocatedVUs: 120,
        maxVUs: 400,
        stages: [
            { target: 30, duration: '30s' },
            { target: 120, duration: '30s' },
            { target: 160, duration: '1m' },
            { target: 200, duration: '1m' },
            { target: 60, duration: '30s' },
            { target: 0, duration: '30s' },
        ],
        tags: { area: 'posts', kind: 'public-read' },
    },
    auth_login: {
        executor: 'ramping-arrival-rate',
        exec: 'loginOnly',
        startRate: 5,
        timeUnit: '1s',
        preAllocatedVUs: 40,
        maxVUs: 150,
        stages: [
            { target: 5, duration: '30s' },
            { target: 20, duration: '30s' },
            { target: 20, duration: '1m' },
            { target: 5, duration: '30s' },
            { target: 0, duration: '10s' },
        ],
        tags: { area: 'auth', kind: 'baseline' },
    },
    chat_rooms_read: {
        executor: 'ramping-vus',
        exec: 'chatRoomsRead',
        startVUs: 5,
        stages: [
            { target: 15, duration: '30s' },
            { target: 30, duration: '30s' },
            { target: 30, duration: '1m' },
            { target: 10, duration: '30s' },
            { target: 0, duration: '10s' },
        ],
        tags: { area: 'chat', kind: 'auth-read' },
    },
    chat_messages_read: {
        executor: 'ramping-vus',
        exec: 'chatMessagesRead',
        startVUs: 20,
        stages: [
            { target: 50, duration: '30s' },
            { target: 100, duration: '30s' },
            { target: 100, duration: '1m' },
            { target: 30, duration: '30s' },
            { target: 0, duration: '10s' },
        ],
        tags: { area: 'chat', kind: 'auth-read' },
    },
    party_members_read: {
        executor: 'ramping-vus',
        exec: 'partyMembersRead',
        startVUs: 10,
        stages: [
            { target: 25, duration: '30s' },
            { target: 50, duration: '30s' },
            { target: 50, duration: '1m' },
            { target: 15, duration: '30s' },
            { target: 0, duration: '10s' },
        ],
        tags: { area: 'party', kind: 'auth-read' },
    },
    party_add_members_write: {
        executor: 'per-vu-iterations',
        exec: 'partyAddMembersWrite',
        vus: 10,
        iterations: 20,
        maxDuration: '1m',
        tags: { area: 'party', kind: 'write-contention' },
    },
};

const SINGLE_SCENARIO_MODES = {
    posts: 'posts_list_public',
    auth: 'auth_login',
    chat_rooms: 'chat_rooms_read',
    chat_messages: 'chat_messages_read',
    party_members: 'party_members_read',
    party_write: 'party_add_members_write',
};

const MIXED_SCENARIOS = [
    { name: 'posts_list_public', enabled: () => true },
    { name: 'auth_login', enabled: () => HAS_AUTH_CREDENTIALS },
    { name: 'chat_rooms_read', enabled: () => HAS_AUTH_CREDENTIALS },
    { name: 'chat_messages_read', enabled: () => HAS_AUTH_CREDENTIALS },
    { name: 'party_members_read', enabled: () => HAS_AUTH_CREDENTIALS },
    { name: 'party_add_members_write', enabled: () => ENABLE_PARTY_WRITE && HAS_AUTH_CREDENTIALS && HAS_PARTY_WRITE_TARGETS },
];

function cloneConfig(name) {
    return JSON.parse(JSON.stringify(BASE_SCENARIO_CONFIG[name]));
}

function scaleByLoadProfile(config) {
    const { vu: vuMul, rate: rateMul } = LOAD_MULTIPLIERS;
    const scaled = JSON.parse(JSON.stringify(config));

    if (scaled.executor === 'ramping-arrival-rate') {
        scaled.startRate = Math.max(1, Math.ceil(scaled.startRate * rateMul));
        scaled.preAllocatedVUs = Math.max(1, Math.ceil(scaled.preAllocatedVUs * vuMul));
        scaled.maxVUs = Math.max(scaled.preAllocatedVUs, Math.ceil(scaled.maxVUs * vuMul));
        scaled.stages = scaled.stages.map((stage) => ({
            ...stage,
            target: Math.ceil(stage.target * rateMul),
        }));
    } else if (scaled.executor === 'ramping-vus') {
        scaled.startVUs = Math.max(1, Math.ceil(scaled.startVUs * vuMul));
        scaled.stages = scaled.stages.map((stage) => ({
            ...stage,
            target: Math.ceil(stage.target * vuMul),
        }));
    } else if (scaled.executor === 'constant-vus') {
        scaled.vus = Math.max(1, Math.ceil(scaled.vus * vuMul));
    } else if (scaled.executor === 'per-vu-iterations') {
        scaled.vus = Math.max(1, Math.ceil(scaled.vus * vuMul));
    }

    return scaled;
}

function scaleByScenarioWeight(name, config) {
    const weight = SCENARIO_WEIGHTS[name];
    if (weight === undefined || Number.isNaN(weight)) {
        return config;
    }

    if (weight <= 0) {
        return null;
    }

    if (weight === 1) {
        return config;
    }

    const scaled = JSON.parse(JSON.stringify(config));

    if (scaled.executor === 'ramping-arrival-rate') {
        scaled.startRate = Math.max(1, Math.ceil(scaled.startRate * weight));
        scaled.preAllocatedVUs = Math.max(1, Math.ceil(scaled.preAllocatedVUs * weight));
        scaled.maxVUs = Math.max(scaled.preAllocatedVUs, Math.ceil(scaled.maxVUs * weight));
        scaled.stages = scaled.stages.map((stage) => ({
            ...stage,
            target: Math.max(1, Math.ceil(stage.target * weight)),
        }));
    } else if (scaled.executor === 'ramping-vus') {
        scaled.startVUs = Math.max(1, Math.ceil(scaled.startVUs * weight));
        scaled.stages = scaled.stages.map((stage) => ({
            ...stage,
            target: Math.max(1, Math.ceil(stage.target * weight)),
        }));
    } else if (scaled.executor === 'constant-vus') {
        scaled.vus = Math.max(1, Math.ceil(scaled.vus * weight));
    } else if (scaled.executor === 'per-vu-iterations') {
        scaled.vus = Math.max(1, Math.ceil(scaled.vus * weight));
    }

    return scaled;
}

function buildScenario(name) {
    const config = cloneConfig(name);
    const scaled = scaleByLoadProfile(config);
    return { [name]: scaled };
}

function buildMixedScenarios() {
    return MIXED_SCENARIOS.reduce((scenarios, scenario) => {
        if (!scenario.enabled()) {
            return scenarios;
        }

        const config = cloneConfig(scenario.name);
        const profileScaled = scaleByLoadProfile(config);
        const weightScaled = scaleByScenarioWeight(scenario.name, profileScaled);
        if (!weightScaled) {
            return scenarios;
        }

        return Object.assign(scenarios, { [scenario.name]: weightScaled });
    }, {});
}

export function buildScenarios() {
    if (TEST_MODE === 'mixed') {
        return buildMixedScenarios();
    }

    const scenarioName = SINGLE_SCENARIO_MODES[TEST_MODE];
    if (!scenarioName) {
        fail(`Unsupported TEST_MODE: ${TEST_MODE}`);
    }

    if (TEST_MODE === 'party_write' && (!HAS_AUTH_CREDENTIALS || !HAS_PARTY_WRITE_TARGETS || !ENABLE_PARTY_WRITE)) {
        fail('party_write mode requires credential source, party targets, and ENABLE_PARTY_WRITE=true.');
    }

    return buildScenario(scenarioName);
}
