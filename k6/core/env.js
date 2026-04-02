import {
    ACTIVE_PRESET,
    ACTIVE_PRESET_NAME,
    PLAN_BASE_URL,
} from './plan.js';

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

function uniqueCredentials(credentials) {
    const seen = new Set();

    return credentials.filter((credential) => {
        if (seen.has(credential.email)) {
            return false;
        }

        seen.add(credential.email);
        return true;
    });
}

export const BASE_URL = __ENV.BASE_URL || PLAN_BASE_URL;
export const RUN_ID = __ENV.RUN_ID || `${ACTIVE_PRESET_NAME}_manual`;
export const SCENARIO_FAMILY = ACTIVE_PRESET.scenarioFamily || ACTIVE_PRESET_NAME;
export const IS_CONTENTION_PRESET = ACTIVE_PRESET.shape.type === 'contention';
export const PARTY_WRITE_PAIR_SIZE = ACTIVE_PRESET.partyWritePairSize || 2;
export const MIN_WRITE_SUCCESS_SAMPLES = ACTIVE_PRESET.minWriteSuccessSamples || 0;

export const LEADER_CREDENTIALS = uniqueCredentials(parseCredentials(__ENV.LEADER_CREDENTIALS || ''));
export const MEMBER_CREDENTIALS = uniqueCredentials(parseCredentials(__ENV.MEMBER_CREDENTIALS || ''));
export const ALL_CREDENTIALS = uniqueCredentials([...LEADER_CREDENTIALS, ...MEMBER_CREDENTIALS]);

export {
    ACTIVE_PRESET,
    ACTIVE_PRESET_NAME,
};
