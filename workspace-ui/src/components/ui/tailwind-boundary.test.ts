// @vitest-environment node
import { ESLint } from 'eslint';
import { describe, expect, it } from 'vitest';

// Runs the project's real ESLint config (eslint.config.js) on source snippets.
async function lintRules(code: string, filePath: string) {
    const [result] = await new ESLint().lintText(code, { filePath });

    return result!.messages.map((message) => message.ruleId);
}

describe('Tailwind boundary (eslint.config.js)', () => {
    it('reports Tailwind classes outside src/components/ui', async () => {
        const code = [
            'export function A() {',
            '    return <div className="flex gap-8"><span className={`hover:bg-tint ${String(1)}`} /></div>;',
            '}',
            '',
        ].join('\n');

        expect(await lintRules(code, 'src/feature/A.tsx')).toEqual(['no-restricted-syntax', 'no-restricted-syntax']);
    });

    it('reports Tailwind tooling imported outside src/components/ui', async () => {
        const code = [
            "import { cva } from 'class-variance-authority';",
            '',
            "import { cn } from '@/components/ui/utils';",
            '',
            'export const classes = [cva, cn];',
            '',
        ].join('\n');

        expect(await lintRules(code, 'src/feature/a.ts')).toEqual(['no-restricted-imports', 'no-restricted-imports']);
    });

    it('accepts CSS Module classes and plain class names outside src/components/ui', async () => {
        const code = [
            "import styles from './A.module.css';",
            '',
            'export function A() {',
            '    return <div className={styles.root}><span className="hero" /></div>;',
            '}',
            '',
        ].join('\n');

        expect(await lintRules(code, 'src/feature/A.tsx')).toEqual([]);
    });

    it('allows Tailwind inside src/components/ui', async () => {
        const code = [
            "import { cn } from './utils';",
            '',
            'export function A() {',
            "    return <div className={cn('flex gap-8', 'hover:bg-tint')} />;",
            '}',
            '',
        ].join('\n');

        expect(await lintRules(code, 'src/components/ui/a.tsx')).toEqual([]);
    });
});
