import { login } from '../core/auth.js';
import { thinkTime } from '../core/utils.js';

export function loginOnly() {
    login();
    thinkTime();
}
