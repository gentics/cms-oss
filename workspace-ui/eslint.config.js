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

// Class names that look like Tailwind utilities: a variant (`hover:`), an arbitrary value (`[`), a
// utility with a value (`px-12`, `bg-azure`) or a value-less utility (`flex`, `truncate`). Used
// as an esquery regex, so it must not contain `/`.
const tailwindClassPattern = `/${[
    '(^|\\s)(',
    '[\\w-]+:',
    '|[\\w-]*\\[',
    '|-?(p[xytrblse]?|m[xytrblse]?|gap(-[xy])?|space-[xy]|w|h|size|min-[wh]|max-[wh]|inset(-[xy])?|top|right|bottom|left|z',
    '|text|bg|border(-[xytrbl])?|rounded(-[a-z]+)?|shadow|ring|outline|opacity|font|leading|tracking',
    '|grid-cols|grid-rows|col|row|items|justify|self|place|content|overflow(-[xy])?|translate-[xy]|scale|rotate',
    '|duration|ease|delay|animate|transition|fill|stroke|cursor|select|origin|order|basis|line-clamp|decoration|divide)-[\\w.%]+',
    '|(flex|grid|block|inline|inline-flex|inline-block|inline-grid|hidden|contents|absolute|relative|fixed|sticky|static',
    '|truncate|sr-only|uppercase|lowercase|capitalize|italic|underline|border|rounded|shadow|transition|grow|shrink',
    '|isolate|visible|invisible|container)(?=\\s|$)',
    ')',
].join('')}/`;

export default defineConfig([
    // Files that should never be linted.
    {
        ignores: [
            'dist/**',
            'coverage/**',
            'playwright-report/**',
            'test-results/**',
            'node_modules/**',

            // Generated from the GenAIx contract by `npm run generate:api`.
            'src/services/apiService/genaix/schema.d.ts',
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

    // Tailwind only in the component layer (src/components/ui, see ui.css). Elsewhere a Tailwind
    // class would silently produce no CSS (`@source` in ui.css), so it is reported here instead.
    {
        files: ['src/**/*.{ts,tsx}'],

        ignores: ['src/components/ui/**'],

        rules: {
            'no-restricted-imports': [
                'error',
                {
                    paths: ['clsx', 'tailwind-merge', 'class-variance-authority'].map((name) => ({
                        name,
                        message: 'Tailwind tooling belongs in src/components/ui. Use a CSS Module here.',
                    })),
                    patterns: [
                        {
                            group: ['**/components/ui/utils', '**/components/ui/ui.css'],
                            message: 'Internal to the component layer. Use the components or a CSS Module here.',
                        },
                    ],
                },
            ],

            'no-restricted-syntax': [
                'error',
                {
                    selector: `JSXAttribute[name.name='className'] Literal[value=${tailwindClassPattern}]`,
                    message: 'Tailwind classes are only allowed in src/components/ui. Use a CSS Module here.',
                },
                {
                    selector: `JSXAttribute[name.name='className'] TemplateElement[value.raw=${tailwindClassPattern}]`,
                    message: 'Tailwind classes are only allowed in src/components/ui. Use a CSS Module here.',
                },
            ],
        },
    },

    // Node-based configuration files.
    {
        files: [
            'vite.config.{js,ts,mjs,mts}',
            'vite.catalogue.config.{js,ts,mjs,mts}',
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
