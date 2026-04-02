import { login } from '../core/auth.js';
import { getAuthUserTarget } from '../core/runtime.js';
import { thinkTime } from '../core/utils.js';

export function loginOnly(data) {
    const target = getAuthUserTarget(data);
    login(target.credential);
    thinkTime();
}
