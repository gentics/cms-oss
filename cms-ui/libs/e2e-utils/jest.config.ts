import { defineConfig } from 'jest';
import { createSWCConfig } from '../../jest.preset';

export default defineConfig({
    ...createSWCConfig('libs', 'e2e-utils'),
});
