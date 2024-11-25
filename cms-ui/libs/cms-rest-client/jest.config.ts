import { Config } from 'jest';
import { createDefaultConfig } from '../../jest.preset';

const config: Config = {
    ...createDefaultConfig('libs', 'cms-rest-client'),
};

export default config;
