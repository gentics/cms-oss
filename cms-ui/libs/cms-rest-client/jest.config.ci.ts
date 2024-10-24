import { Config } from 'jest';
import { createCIConfig } from '../../jest.preset';

const config: Config = {
    ...createCIConfig('libs', 'cms-rest-client'),
};

export default config;
