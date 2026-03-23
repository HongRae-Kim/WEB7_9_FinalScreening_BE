import { LOAD_PROFILE, RUN_ID, TEST_MODE } from '../core/env.js';
import { buildScenarios } from './scenarios.js';
import { thresholds } from './thresholds.js';
import { setup } from './setup.js';

export const options = {
    scenarios: buildScenarios(),
    thresholds,
    tags: {
        run_id: RUN_ID,
        test_mode: TEST_MODE,
        load_profile: LOAD_PROFILE,
    },
};

export { setup };
