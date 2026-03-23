import { check } from 'k6';

import { BASE_URL } from '../core/env.js';
import { runGet } from '../core/requests.js';

export function postsListPublic() {
    const res = runGet({
        url: `${BASE_URL}/api/v1/posts?size=20`,
        endpoint: 'posts_list',
        checkName: 'posts list status 200',
    });

    check(res, {
        'posts list has body': (r) => !!r.body,
    });
}
