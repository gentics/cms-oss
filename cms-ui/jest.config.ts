import { getJestProjectsAsync } from '@nx/jest';
import type { Config } from 'jest';

export default async () => ({
    projects: await getJestProjectsAsync(),
    setupFilesAfterEnv: [
        "jest-extended/all",
    ],
} as Config);
