import { ensureLeaderAuthenticated } from '../core/auth.js';
import { BASE_URL, getPartyIdForVU, TARGET_USER_IDS } from '../core/env.js';
import { runGet, runPostJson } from '../core/requests.js';

export function partyMembersRead() {
    const partyId = getPartyIdForVU(__VU);

    runGet({
        url: `${BASE_URL}/api/v1/parties/${partyId}/members`,
        endpoint: 'party_members',
        checkName: 'party members status 200',
        authenticated: true,
    });
}

export function partyAddMembersWrite() {
    const partyId = getPartyIdForVU(__VU);

    ensureLeaderAuthenticated();

    runPostJson({
        url: `${BASE_URL}/api/v1/parties/${partyId}/members`,
        endpoint: 'party_add_members',
        body: {
            targetUserIds: TARGET_USER_IDS,
        },
        checkName: 'party add members handled',
        allowedStatuses: [200, 400, 409],
        ensureAuth: false,
    });
}
