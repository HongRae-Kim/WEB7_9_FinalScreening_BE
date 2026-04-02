import { check, fail, sleep } from 'k6';

export function safeJson(res) {
    try {
        return res.json();
    } catch (e) {
        return null;
    }
}

export function ensureEnv(value, name) {
    if (!value) {
        fail(`${name} is required`);
    }
}

export function logUnexpected(name, res, context = {}) {
    console.error(`[k6-failure] ${JSON.stringify({
        endpoint: name,
        status: res.status,
        body: res.body,
        ...context,
    })}`);
}

export function thinkTime(minSeconds = 0.5, maxSeconds = 1.5) {
    sleep(minSeconds + Math.random() * (maxSeconds - minSeconds));
}

export function runStatusChecks(res, endpoint, checkName, allowedStatuses, context = {}) {
    const ok = check(res, {
        [checkName]: (r) => allowedStatuses.includes(r.status),
    });

    if (!ok) {
        logUnexpected(endpoint, res, context);
    }

    return res;
}

export function uniqueBy(items, getKey) {
    const seen = new Set();

    return items.filter((item) => {
        const key = getKey(item);
        if (seen.has(key)) {
            return false;
        }

        seen.add(key);
        return true;
    });
}

export function buildCombinations(values, combinationSize) {
    if (combinationSize <= 0 || values.length < combinationSize) {
        return [];
    }

    if (combinationSize === 1) {
        return values.map((value) => [value]);
    }

    const combinations = [];

    for (let index = 0; index <= values.length - combinationSize; index += 1) {
        const head = values[index];
        const tailCombinations = buildCombinations(values.slice(index + 1), combinationSize - 1);

        tailCombinations.forEach((tail) => {
            combinations.push([head, ...tail]);
        });
    }

    return combinations;
}
