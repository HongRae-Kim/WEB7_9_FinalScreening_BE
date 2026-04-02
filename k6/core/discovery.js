import http from 'k6/http';
import { fail } from 'k6';

import { createDiscoverySession } from './auth.js';
import {
    BASE_URL,
    LEADER_CREDENTIALS,
    MEMBER_CREDENTIALS,
    PARTY_WRITE_PAIR_SIZE,
} from './env.js';
import { safeJson, uniqueBy } from './utils.js';

function cookieParams(session, endpoint) {
    return {
        headers: {
            Cookie: session.authCookieHeader,
        },
        tags: {
            endpoint,
            phase: 'setup',
        },
    };
}

function authedGetJson(session, url, endpoint, label) {
    const res = http.get(url, cookieParams(session, endpoint));

    if (res.status !== 200) {
        fail(`${label} failed: status=${res.status}, body=${res.body}`);
    }

    const json = safeJson(res);
    if (!json) {
        fail(`${label} returned a non-JSON response.`);
    }

    return json;
}

function fetchProfile(session) {
    return authedGetJson(
        session,
        `${BASE_URL}/api/v1/users/me`,
        'setup_user_profile',
        `profile lookup for ${session.email}`,
    );
}

function fetchChatRooms(session) {
    const response = authedGetJson(
        session,
        `${BASE_URL}/api/v1/chats?size=100`,
        'setup_chat_rooms',
        `chat room discovery for ${session.email}`,
    );

    return response.chatRooms || [];
}

function fetchMyParties(session) {
    const response = authedGetJson(
        session,
        `${BASE_URL}/api/v1/users/me/parties`,
        'setup_my_parties',
        `party discovery for ${session.email}`,
    );

    return response.data?.parties || [];
}

function fetchPartyMembers(session, partyId) {
    const response = authedGetJson(
        session,
        `${BASE_URL}/api/v1/parties/${partyId}/members`,
        'setup_party_members',
        `party member discovery for party ${partyId}`,
    );

    return response.data?.members || [];
}

function fetchPostDetail(session, postId) {
    const response = authedGetJson(
        session,
        `${BASE_URL}/api/v1/posts/${postId}`,
        'setup_post_detail',
        `post detail discovery for post ${postId}`,
    );

    return {
        recruitCount: Number(response.recruitCount || 0),
        currentParticipants: Number(response.currentParticipants || 0),
    };
}

function buildDisjointTargetUserIds(availableUserIds, pairSize, maxSuccesses) {
    if (pairSize <= 0 || maxSuccesses <= 0) {
        return [];
    }

    const normalizedUserIds = [...availableUserIds]
        .filter((userId) => userId !== null && userId !== undefined)
        .sort((left, right) => Number(left) - Number(right));

    const disjointTargets = [];

    for (let successIndex = 0; successIndex < maxSuccesses; successIndex += 1) {
        const start = successIndex * pairSize;
        const targetUserIds = normalizedUserIds.slice(start, start + pairSize);

        if (targetUserIds.length === pairSize) {
            disjointTargets.push(targetUserIds);
        }
    }

    return disjointTargets;
}

function buildCredentialRecord(credential, source) {
    const session = createDiscoverySession(credential);
    const profile = fetchProfile(session);
    const chatRooms = fetchChatRooms(session);
    const parties = fetchMyParties(session);

    return {
        source,
        credential: {
            email: credential.email,
            password: credential.password,
        },
        userId: profile.id,
        chatRoomIds: uniqueBy(
            chatRooms
                .map((chatRoom) => chatRoom.chatRoomId)
                .filter((chatRoomId) => chatRoomId !== null && chatRoomId !== undefined),
            (chatRoomId) => String(chatRoomId),
        ),
        parties: uniqueBy(
            parties.map((party) => ({
                partyId: party.partyId,
                postId: party.postId,
                myRole: party.myRole,
            })),
            (party) => String(party.partyId),
        ),
    };
}

function buildAuthUsers(leaderRecords, memberRecords) {
    return uniqueBy(
        [...leaderRecords, ...memberRecords].map((record) => ({
            credential: record.credential,
            userId: record.userId,
            chatRoomIds: record.chatRoomIds,
            partyIds: record.parties.map((party) => party.partyId),
        })),
        (record) => record.credential.email,
    );
}

function buildChatTargets(authUsers) {
    return authUsers.flatMap((user) =>
        user.chatRoomIds.map((chatRoomId) => ({
            credential: user.credential,
            chatRoomId,
        })),
    );
}

function buildPartyReaderTargets(authUsers) {
    return authUsers.flatMap((user) =>
        user.partyIds.map((partyId) => ({
            credential: user.credential,
            partyId,
        })),
    );
}

function buildWriteTargets(leaderRecords, memberRecords) {
    const memberUserIds = uniqueBy(
        memberRecords
            .map((record) => record.userId)
            .filter((userId) => userId !== null && userId !== undefined),
        (userId) => String(userId),
    );
    const targets = [];

    leaderRecords.forEach((leader) => {
        const session = createDiscoverySession(leader.credential);
        const leaderParties = leader.parties.filter((party) => party.myRole === 'LEADER');

        leaderParties.forEach((party) => {
            const partyMembers = fetchPartyMembers(session, party.partyId);
            const postDetail = fetchPostDetail(session, party.postId);
            const joinedUserIds = new Set(
                partyMembers.map((member) => member.userId).filter((userId) => userId !== null && userId !== undefined),
            );

            const availableUserIds = memberUserIds.filter((userId) =>
                userId !== leader.userId && !joinedUserIds.has(userId),
            );
            const openSlots = Math.max(0, postDetail.recruitCount - postDetail.currentParticipants);
            const maxSuccesses = Math.min(
                Math.floor(openSlots / PARTY_WRITE_PAIR_SIZE),
                Math.floor(availableUserIds.length / PARTY_WRITE_PAIR_SIZE),
            );

            buildDisjointTargetUserIds(availableUserIds, PARTY_WRITE_PAIR_SIZE, maxSuccesses).forEach((targetUserIds) => {
                targets.push({
                    credential: leader.credential,
                    leaderUserId: leader.userId,
                    partyId: party.partyId,
                    postId: party.postId,
                    targetUserIds,
                });
            });
        });
    });

    return uniqueBy(
        targets,
        (target) => `${target.partyId}:${target.targetUserIds.join('-')}`,
    );
}

export function discoverTestData() {
    const leaderRecords = LEADER_CREDENTIALS.map((credential) => buildCredentialRecord(credential, 'leader'));
    const memberRecords = MEMBER_CREDENTIALS.map((credential) => buildCredentialRecord(credential, 'member'));
    const authUsers = buildAuthUsers(leaderRecords, memberRecords);
    const chatTargets = buildChatTargets(authUsers);
    const partyReaderTargets = buildPartyReaderTargets(authUsers);
    const writeTargets = buildWriteTargets(leaderRecords, memberRecords);

    return {
        leaders: leaderRecords,
        members: memberRecords,
        authUsers,
        chatTargets,
        partyReaderTargets,
        writeTargets,
        contentionTargets: writeTargets.length > 0 ? [writeTargets[0]] : [],
        pools: {
            leaders: leaderRecords.length,
            members: memberRecords.length,
            authUsers: authUsers.length,
            chatTargets: chatTargets.length,
            partyReaderTargets: partyReaderTargets.length,
            writeTargets: writeTargets.length,
        },
    };
}
