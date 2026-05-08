import { Config } from 'jest';
import { createDefaultConfig } from '../../jest.preset';

const config: Config = {
    ...createDefaultConfig('libs', 'form-grid'),
};

export default config;
