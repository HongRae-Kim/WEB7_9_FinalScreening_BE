const DEFAULT_PLAN_PATH = '../test-plan.json';

const SUPPORTED_SHAPE_TYPES = new Set([
    'ramping-arrival-rate',
    'constant-arrival-rate',
    'contention',
]);

function assert(condition, message) {
    if (!condition) {
        throw new Error(message);
    }
}

function loadTestPlan() {
    const raw = open(DEFAULT_PLAN_PATH);
    const parsed = JSON.parse(raw);

    assert(parsed && typeof parsed === 'object', 'k6/test-plan.json must contain a JSON object.');
    assert(parsed.presets && typeof parsed.presets === 'object', 'k6/test-plan.json must define presets.');
    assert(parsed.preset, 'k6/test-plan.json must define a default preset.');

    const preset = parsed.presets[parsed.preset];
    assert(preset, `Preset "${parsed.preset}" is not defined in k6/test-plan.json.`);
    assert(preset.shape && typeof preset.shape === 'object', `Preset "${parsed.preset}" must define shape.`);
    assert(
        SUPPORTED_SHAPE_TYPES.has(preset.shape.type),
        `Unsupported shape type "${preset.shape.type}" for preset "${parsed.preset}".`,
    );

    if (preset.shape.type !== 'contention') {
        assert(preset.mix && typeof preset.mix === 'object', `Preset "${parsed.preset}" must define mix.`);
    }

    return parsed;
}

export const TEST_PLAN = loadTestPlan();
export const ACTIVE_PRESET_NAME = TEST_PLAN.preset;
export const ACTIVE_PRESET = TEST_PLAN.presets[ACTIVE_PRESET_NAME];
export const PLAN_BASE_URL = TEST_PLAN.baseUrl || 'http://localhost:8080';
export const OUTPUT_PREFIX = TEST_PLAN.outputPrefix || ACTIVE_PRESET_NAME;
