import js from '@eslint/js';
import stylistic from '@stylistic/eslint-plugin';
import pluginQuery from '@tanstack/eslint-plugin-query';
import { defineConfig } from 'eslint/config';
import playwright from 'eslint-plugin-playwright';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import simpleImportSort from 'eslint-plugin-simple-import-sort';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default defineConfig([
    // Files that should never be linted.
    {
        ignores: [
            'dist/**',
            'coverage/**',
            'playwright-report/**',
            'test-results/**',
            'node_modules/**',
        ],
    },

    // JavaScript.
    js.configs.recommended,

    // TypeScript.
    ...tseslint.configs.recommended,

    // TanStack Query.
    ...pluginQuery.configs['flat/recommended'],

    // Formatting and import sorting.
    {
        files: ['**/*.{js,mjs,cjs,ts,mts,cts,jsx,tsx}'],

        plugins: {
            '@stylistic': stylistic,
            'simple-import-sort': simpleImportSort
        },

        rules: {
            // Import sorting.
            'simple-import-sort/imports': [
                'error',
                {
                    groups: [
                        // Node.js built-ins.
                        ['^node:'],

                        // External packages.
                        ['^@?\\w'],

                        // Internal aliases.
                        ['^@/'],

                        // Relative imports.
                        ['^\\.'],

                        // Styles.
                        ['^.+\\.s?css$'],
                    ],
                },
            ],

            'simple-import-sort/exports': 'error',
            // Formatting
            '@stylistic/arrow-parens': ['warn', 'always'],

            '@stylistic/brace-style': [
                'error',
                '1tbs',
                {
                    allowSingleLine: true,
                },
            ],

            '@stylistic/indent': ['warn', 4],

            '@stylistic/member-delimiter-style': [
                'error',
                {
                    multiline: {
                        delimiter: 'semi',
                        requireLast: true,
                    },
                    singleline: {
                        delimiter: 'semi',
                        requireLast: false,
                    },
                    multilineDetection: 'brackets',
                },
            ],

            '@stylistic/padded-blocks': 'off',

            '@stylistic/quotes': [
                'error',
                'single',
                {
                    avoidEscape: true,
                },
            ],

            '@stylistic/semi': ['error', 'always'],
        },
    },

    // Application source.
    {
        files: ['src/**/*.{ts,tsx}'],

        languageOptions: {
            globals: {
                ...globals.browser,
            },
        },

        plugins: {
            'react-hooks': reactHooks,
            'react-refresh': reactRefresh,
        },

        rules: {
            // React Hooks.
            ...reactHooks.configs.recommended.rules,

            // React Fast Refresh / Vite.
            'react-refresh/only-export-components': [
                'warn',
                {
                    allowConstantExport: true,
                },
            ],

            // TypeScript.
            '@typescript-eslint/no-explicit-any': 'error',

            '@typescript-eslint/no-non-null-assertion': 'off',

            '@typescript-eslint/no-unused-vars': [
                'warn',
                {
                    varsIgnorePattern: '^_',
                    argsIgnorePattern: '^_',
                    destructuredArrayIgnorePattern: '^_',
                    caughtErrors: 'none',
                },
            ],

            // Browser application.
            'no-console': [
                'warn',
                {
                    allow: ['warn', 'error'],
                },
            ],
        },
    },

    // Node-based configuration files.
    {
        files: [
            'vite.config.{js,ts,mjs,mts}',
            'vitest.config.{js,ts,mjs,mts}',
            'playwright.config.{js,ts,mjs,mts}',
            'eslint.config.{js,ts,mjs,mts}',
        ],

        languageOptions: {
            globals: {
                ...globals.node,
            },
        },
    },

    // Playwright E2E tests.
    {
        files: ['e2e/**/*.{ts,tsx}'],

        extends: [playwright.configs['flat/recommended']],
    },

    // Vitest tests.
    {
        files: ['**/*.test.{ts,tsx}'],

        rules: {
            '@typescript-eslint/no-unused-vars': 'off',
            '@typescript-eslint/no-non-null-assertion': 'off',
        },
    },
]);
