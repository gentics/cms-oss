import { Config } from 'jest';
import nxPreset from '@nx/jest/preset';

export function createDefaultConfig(type: 'apps' | 'libs', name: string): Config {
    return {
        ...nxPreset,
        displayName: name,
        coverageDirectory: `../../coverage/${type}/${name}`,
        collectCoverage: true,
        coverageReporters: ['text-summary'],
        // Lodash-ES would need to be transpiled, which we skip by just using regular lodash instead.
        moduleNameMapper: {
            '^lodash-es': 'lodash',
        },
        moduleFileExtensions: ['ts', 'js', 'html'],
        testEnvironment: 'node',
        transform: {
            '^.+\\.[tj]s$': ['ts-jest', { tsconfig: '<rootDir>/tsconfig.spec.json' }],
        },
    };
}

export function createCIConfig(type: 'apps' | 'libs', name: string): Config {
    return {
        ...createDefaultConfig(type, name),
        coverageReporters: ['lcovonly'],
        ci: true,
        reporters: [
            'default',
            ['jest-junit', {
                outputFile: `.reports/${type}/${name}/JEST-report.xml`,
                classNameTemplate: `ui.unit.${name}.{classname}`,
                suiteNameTemplate: `UI Unit Test ${name}: {title}`,
            }],
        ],
    }
}
