import nxPreset from '@nx/jest/preset';
import { Config } from 'jest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

export function getNxPreset(): Partial<Config> {
    return nxPreset;
}

export function createSWCConfig(type: 'apps' | 'libs', name: string, swcFile?: string): Partial<Config> {
    let swcJestConfig: any;
    if (swcFile) {
        swcJestConfig = JSON.parse(readFileSync(swcFile, 'utf-8'));
    } else {
        swcJestConfig = JSON.parse(readFileSync(`${__dirname}/jest.swcrc`, 'utf-8'));
    }

    // Reading the SWC compilation config and remove the "exclude"
    // for the test files to be compiled by SWC
    delete swcJestConfig.exclude;

    // disable .swcrc look-up by SWC core because we're passing in swcJestConfig ourselves.
    // If we do not disable this, SWC Core will read .swcrc and won't transform our test files due to "exclude"
    if (swcJestConfig.swcrc === undefined) {
        swcJestConfig.swcrc = false;
    }

    return {
        ...getNxPreset(),
        displayName: name,
        transform: {
            '^.+\\.[tj]s$': ['@swc/jest', swcJestConfig],
        },
        moduleFileExtensions: ['ts', 'js', 'html'],
        testEnvironment: 'node',
        collectCoverage: true,
        coverageDirectory: resolve(__dirname, `coverage/${type}/${name}`),
    };
}

export function createAngularConfig(type: 'apps' | 'libs', name: string): Partial<Config> {
    return {
        ...getNxPreset(),
        displayName: name,
        setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
        coverageDirectory: resolve(__dirname, `coverage/${type}/${name}`),
        transform: {
            '^.+\\.(ts|mjs|js|html)$': [
                'jest-preset-angular',
                {
                    tsconfig: '<rootDir>/tsconfig.spec.json',
                    stringifyContentPathRegex: '\\.(html|svg)$',
                },
            ],
        },
        transformIgnorePatterns: ['node_modules/(?!.*\\.mjs$)'],
        snapshotSerializers: [
            'jest-preset-angular/build/serializers/no-ng-attributes',
            'jest-preset-angular/build/serializers/ng-snapshot',
            'jest-preset-angular/build/serializers/html-comment',
        ],
    };
}

export function createCIReporters(type: 'apps' | 'libs', name: string): Partial<Config> {
    return {
        reporters: [
            'default',
            ['jest-junit', {
                outputFile: resolve(__dirname, `.reports/${type}/${name}/JEST-report.xml`),
                classNameTemplate: 'ui.unit.{displayname}.{classname}',
                suiteNameTemplate: 'UI Unit Test {displayName}: {title}',
            }],
        ],
    };
}
