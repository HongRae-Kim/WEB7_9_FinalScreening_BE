import { BASE_URL } from '../core/env.js';
import { runGet, runPostJson } from '../core/requests.js';
import { getPartyReaderTarget, getWriteTarget } from '../core/runtime.js';

export function partyMembersRead(data) {
    const target = getPartyReaderTarget(data);

    runGet({
        url: `${BASE_URL}/api/v1/parties/${target.partyId}/members`,
        endpoint: 'party_members',
        checkName: 'party members status 200',
        credentials: target.credential,
    });
}

export function partyAddMembersWrite(data) {
    const target = getWriteTarget(data);

    runPostJson({
        url: `${BASE_URL}/api/v1/parties/${target.partyId}/members`,
        endpoint: 'party_add_members',
        body: {
            targetUserIds: target.targetUserIds,
        },
        checkName: 'party add members handled',
        allowedStatuses: [200, 400, 409],
        successStatuses: [200],
        businessStatusMap: {
            400: 'duplicate',
            409: 'conflict',
        },
        credentials: target.credential,
    });
}
