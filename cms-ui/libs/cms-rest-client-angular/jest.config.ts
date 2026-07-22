import { defineConfig } from 'jest';
import { createAngularConfig } from '../../jest.preset';

export default defineConfig({
    ...createAngularConfig('libs', 'cms-rest-client-angular'),
});
