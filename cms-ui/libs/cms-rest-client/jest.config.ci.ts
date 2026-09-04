import { createCIReporters } from '../../jest.preset';
import config from './jest.config';

export default {
    ...config,
    ...createCIReporters('libs', 'cms-rest-client'),
};
