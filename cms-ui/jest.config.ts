import { Config } from 'jest';
import { getJestProjects } from '@nx/jest';

const config: Config = {
    projects: getJestProjects(),
    setupFilesAfterEnv: [
        "jest-extended/all",
    ],
};

export default config;
