import { BASE_URL, getChatRoomIdForVU } from '../core/env.js';
import { runGet } from '../core/requests.js';

export function chatRoomsRead() {
    runGet({
        url: `${BASE_URL}/api/v1/chats?size=20`,
        endpoint: 'chat_rooms',
        checkName: 'chat rooms status 200',
        authenticated: true,
    });
}

export function chatMessagesRead() {
    const chatRoomId = getChatRoomIdForVU(__VU);

    runGet({
        url: `${BASE_URL}/api/v1/chats/${chatRoomId}/messages?size=30`,
        endpoint: 'chat_messages',
        checkName: 'chat messages status 200',
        authenticated: true,
    });
}
