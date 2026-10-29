import { describe, expect, it } from 'vitest';

/**
 * Values a CSS Module must take from the tokens in src/index.css instead (design.md §1 "Nur Tokens",
 * §5 and §14 for the weights). `0ms` / `0s` stay allowed, for `prefers-reduced-motion` (§10).
 */
const rules: { name: string; pattern: RegExp }[] = [
    { name: 'hex colour (use a colour token)', pattern: /#[\da-f]{3,8}\b/gi },
    { name: 'literal rgb()/hsl() (use rgba(var(--…-rgb), a))', pattern: /\b(?:rgba?|hsla?)\(\s*[\d.]/gi },
    { name: 'duration (use --duration-state, --duration-popover or --duration-card)', pattern: /(?<![\w.-])(?!0+m?s\b)\d*\.?\d+m?s\b/g },
    { name: 'cubic-bezier() (use --easing)', pattern: /cubic-bezier\(/g },
    { name: 'font weight above 500 (design.md §5: only 400 and 500)', pattern: /font-weight:\s*(?:[6-9]00|bold|bolder)\b/g },
];

/** Every rule broken in `css`, as `line: rule (match)`. Comments are ignored. */
function findViolations(css: string): string[] {
    const withoutComments = css.replace(/\/\*[\s\S]*?\*\//g, (comment) => comment.replace(/[^\n]/g, ' '));

    return withoutComments.split('\n').flatMap((line, index) => rules.flatMap(({ name, pattern }) =>
        [...line.matchAll(pattern)].map((match) => `${index + 1}: ${name} (${match[0]})`)));
}

const modules = import.meta.glob<string>('/src/**/*.module.css', { query: '?raw', import: 'default', eager: true });

describe('Design tokens in CSS Modules', () => {
    it('finds CSS Modules to check', () => {
        expect(Object.keys(modules).length).toBeGreaterThan(0);
    });

    it.each(Object.entries(modules))('%s uses only tokens', (_path, css) => {
        expect(findViolations(css)).toEqual([]);
    });

    it('reports values that bypass the tokens, and accepts tokens, zero durations and comments', () => {
        const css = [
            '/* #fff, 120ms and cubic-bezier( in a comment are fine */',
            '.a { color: #3e4850; background: rgba(0, 0, 0, 0.1); transition: color 120ms cubic-bezier(0.2, 0.8, 0.2, 1); }',
            '.b { font-weight: 600; animation-duration: .4s; }',
            '.c { color: var(--fg-primary); background: rgba(var(--interactive-rgb), 0.1); transition: color var(--duration-state) var(--easing); }',
            '.d { font-weight: 500; transition-duration: 0ms; width: 12px; }',
        ].join('\n');

        expect(findViolations(css)).toEqual([
            '2: hex colour (use a colour token) (#3e4850)',
            '2: literal rgb()/hsl() (use rgba(var(--…-rgb), a)) (rgba(0)',
            '2: duration (use --duration-state, --duration-popover or --duration-card) (120ms)',
            '2: cubic-bezier() (use --easing) (cubic-bezier()',
            '3: duration (use --duration-state, --duration-popover or --duration-card) (.4s)',
            '3: font weight above 500 (design.md §5: only 400 and 500) (font-weight: 600)',
        ]);
    });
});
