import exec from 'k6/execution';
import { fail } from 'k6';

import { IS_CONTENTION_PRESET } from './env.js';

function discoveryFromSetup(data) {
    const discovery = data?.discovery;

    if (!discovery) {
        fail('setup discovery payload is missing.');
    }

    return discovery;
}

function pickTarget(targets, index, label) {
    if (!targets || targets.length === 0) {
        fail(`${label} is empty.`);
    }

    const normalizedIndex = Math.abs(Number(index) || 0) % targets.length;
    return targets[normalizedIndex];
}

function scenarioIteration() {
    return Number(exec.scenario.iterationInTest || 0);
}

function vuId() {
    return Math.max(1, Number(exec.vu.idInTest || 1));
}

export function getAuthUserTarget(data) {
    return pickTarget(discoveryFromSetup(data).authUsers, scenarioIteration(), 'auth user targets');
}

export function getChatRoomTarget(data) {
    return pickTarget(discoveryFromSetup(data).chatTargets, scenarioIteration(), 'chat room targets');
}

export function getPartyReaderTarget(data) {
    return pickTarget(discoveryFromSetup(data).partyReaderTargets, scenarioIteration(), 'party reader targets');
}

export function getWriteTarget(data) {
    const discovery = discoveryFromSetup(data);

    if (IS_CONTENTION_PRESET) {
        return pickTarget(discovery.contentionTargets, vuId() - 1, 'contention write targets');
    }

    return pickTarget(discovery.writeTargets, scenarioIteration(), 'write targets');
}
