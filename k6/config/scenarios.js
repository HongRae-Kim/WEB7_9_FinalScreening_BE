import { ACTIVE_PRESET, SCENARIO_FAMILY } from '../core/env.js';

const BASE_SCENARIO_CONFIG = {
    posts_list_public: {
        exec: 'postsListPublic',
        tags: { area: 'posts', kind: 'public-read' },
    },
    auth_login: {
        exec: 'loginOnly',
        tags: { area: 'auth', kind: 'login' },
    },
    chat_rooms_read: {
        exec: 'chatRoomsRead',
        tags: { area: 'chat', kind: 'auth-read' },
    },
    chat_messages_read: {
        exec: 'chatMessagesRead',
        tags: { area: 'chat', kind: 'auth-read' },
    },
    party_members_read: {
        exec: 'partyMembersRead',
        tags: { area: 'party', kind: 'auth-read' },
    },
    party_add_members_write: {
        exec: 'partyAddMembersWrite',
        tags: { area: 'party', kind: 'write' },
    },
};

function scaleTarget(target, weight) {
    if (target === 0 || weight === 0) {
        return 0;
    }

    return Math.max(1, Math.round(target * weight));
}

function scenarioTags(baseTags) {
    return {
        ...baseTags,
        scenario_family: SCENARIO_FAMILY,
    };
}

function deriveVuSizing(maxRate, weight = 1) {
    const normalizedRate = Math.max(1, Math.ceil(maxRate * weight));
    const preAllocatedVUs = Math.max(8, normalizedRate * 4);
    const maxVUs = Math.max(preAllocatedVUs + 16, normalizedRate * 8);

    return {
        preAllocatedVUs,
        maxVUs,
    };
}

function buildRampingScenario(name, weight) {
    if (weight <= 0) {
        return null;
    }

    const base = BASE_SCENARIO_CONFIG[name];
    const scaledStages = ACTIVE_PRESET.shape.stages.map((stage) => ({
        duration: stage.duration,
        target: scaleTarget(stage.target, weight),
    }));
    const maxRate = Math.max(...scaledStages.map((stage) => stage.target));
    const sizing = deriveVuSizing(maxRate);

    return {
        executor: 'ramping-arrival-rate',
        exec: base.exec,
        timeUnit: ACTIVE_PRESET.shape.timeUnit || '1s',
        startRate: scaledStages[0]?.target || 1,
        stages: scaledStages,
        preAllocatedVUs: sizing.preAllocatedVUs,
        maxVUs: sizing.maxVUs,
        tags: scenarioTags(base.tags),
    };
}

function buildConstantScenario(name, weight) {
    if (weight <= 0) {
        return null;
    }

    const base = BASE_SCENARIO_CONFIG[name];
    const rate = scaleTarget(ACTIVE_PRESET.shape.rate, weight);
    const sizing = deriveVuSizing(rate, 1.5);

    return {
        executor: 'constant-arrival-rate',
        exec: base.exec,
        timeUnit: ACTIVE_PRESET.shape.timeUnit || '1s',
        rate,
        duration: ACTIVE_PRESET.shape.duration,
        preAllocatedVUs: sizing.preAllocatedVUs,
        maxVUs: sizing.maxVUs,
        tags: scenarioTags(base.tags),
    };
}

function buildRealisticScenarios() {
    const scenarioNames = Object.keys(BASE_SCENARIO_CONFIG);

    return scenarioNames.reduce((scenarios, name) => {
        const weight = ACTIVE_PRESET.mix?.[name] || 0;
        const shapeType = ACTIVE_PRESET.shape.type;
        const config = shapeType === 'ramping-arrival-rate'
            ? buildRampingScenario(name, weight)
            : buildConstantScenario(name, weight);

        if (!config) {
            return scenarios;
        }

        return {
            ...scenarios,
            [name]: config,
        };
    }, {});
}

function buildContentionScenario() {
    const base = BASE_SCENARIO_CONFIG.party_add_members_write;

    return {
        party_add_members_write: {
            executor: 'per-vu-iterations',
            exec: base.exec,
            vus: ACTIVE_PRESET.shape.vus,
            iterations: ACTIVE_PRESET.shape.iterations,
            maxDuration: ACTIVE_PRESET.shape.maxDuration,
            tags: scenarioTags({
                ...base.tags,
                kind: 'write-contention',
            }),
        },
    };
}

export function buildScenarios() {
    if (ACTIVE_PRESET.shape.type === 'contention') {
        return buildContentionScenario();
    }

    return buildRealisticScenarios();
}
