import http from 'k6/http';
import { check, fail } from 'k6';

import {
    BASE_URL,
    IS_CONTENTION_PRESET,
    LEADER_CREDENTIALS,
    MEMBER_CREDENTIALS,
    MIN_WRITE_SUCCESS_SAMPLES,
} from '../core/env.js';
import { discoverTestData } from '../core/discovery.js';

export function setup() {
    const res = http.get(`${BASE_URL}/actuator/health`);

    const serverIsUp = check(res, {
        'server is up': (r) => r.status === 200,
    });

    if (!serverIsUp) {
        fail(`health check failed: status=${res.status}, body=${res.body}`);
    }

    if (LEADER_CREDENTIALS.length === 0) {
        fail('LEADER_CREDENTIALS is required.');
    }

    if (MEMBER_CREDENTIALS.length === 0) {
        fail('MEMBER_CREDENTIALS is required.');
    }

    const discovery = discoverTestData();

    if (discovery.authUsers.length === 0) {
        fail('No authenticated test users could be discovered from the configured credentials.');
    }

    if (discovery.chatTargets.length === 0) {
        fail('No chat room targets were discovered. Check seeded chat rooms for the configured accounts.');
    }

    if (discovery.partyReaderTargets.length === 0) {
        fail('No party reader targets were discovered. Check seeded parties for the configured accounts.');
    }

    if (IS_CONTENTION_PRESET && discovery.contentionTargets.length === 0) {
        fail('No contention write targets were discovered. Check leader/member seed data.');
    }

    if (!IS_CONTENTION_PRESET && discovery.writeTargets.length === 0) {
        fail('No realistic write targets were discovered. Check leader/member seed data.');
    }

    if (!IS_CONTENTION_PRESET && MIN_WRITE_SUCCESS_SAMPLES > 0 && discovery.writeTargets.length < MIN_WRITE_SUCCESS_SAMPLES) {
        fail(
            `Insufficient realistic write targets: discovered=${discovery.writeTargets.length}, `
            + `required=${MIN_WRITE_SUCCESS_SAMPLES}. Re-seed the write target bank before running.`,
        );
    }

    return {
        startedAt: new Date().toISOString(),
        discovery,
    };
}
