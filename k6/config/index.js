import { ACTIVE_PRESET_NAME, RUN_ID, SCENARIO_FAMILY } from '../core/env.js';
import { buildScenarios } from './scenarios.js';
import { thresholds } from './thresholds.js';
import { setup } from './setup.js';

export const options = {
    scenarios: buildScenarios(),
    thresholds,
    tags: {
        run_id: RUN_ID,
        preset: ACTIVE_PRESET_NAME,
        scenario_family: SCENARIO_FAMILY,
    },
};

export { setup };
