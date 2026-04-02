import { BASE_URL } from '../core/env.js';
import { runGet } from '../core/requests.js';
import { getChatRoomTarget } from '../core/runtime.js';

export function chatRoomsRead(data) {
    const target = getChatRoomTarget(data);

    runGet({
        url: `${BASE_URL}/api/v1/chats?size=20`,
        endpoint: 'chat_rooms',
        checkName: 'chat rooms status 200',
        credentials: target.credential,
    });
}

export function chatMessagesRead(data) {
    const target = getChatRoomTarget(data);

    runGet({
        url: `${BASE_URL}/api/v1/chats/${target.chatRoomId}/messages?size=30`,
        endpoint: 'chat_messages',
        checkName: 'chat messages status 200',
        credentials: target.credential,
    });
}
