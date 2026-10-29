import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

function stubSystemTheme(theme: 'light' | 'dark') {
    vi.stubGlobal('matchMedia', (query: string) => ({ matches: theme === 'dark' && query === '(prefers-color-scheme: dark)' }));
}

// Each test imports a fresh store, so the start value is the one of a newly opened app.
async function importStore() {
    vi.resetModules();

    return (await import('./useThemeStore')).useThemeStore;
}

describe('useThemeStore', () => {
    beforeEach(() => {
        delete document.documentElement.dataset.theme;
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('starts light, even when the system is dark', async () => {
        stubSystemTheme('dark');

        const useThemeStore = await importStore();

        expect(useThemeStore.getState().theme).toBe('light');
    });

    it('toggles between light and dark and pins the choice on <html>', async () => {
        stubSystemTheme('light');

        const useThemeStore = await importStore();

        expect(useThemeStore.getState().theme).toBe('light');

        useThemeStore.getState().toggleTheme();

        expect(useThemeStore.getState().theme).toBe('dark');
        expect(document.documentElement).toHaveAttribute('data-theme', 'dark');

        useThemeStore.getState().toggleTheme();

        expect(useThemeStore.getState().theme).toBe('light');
        expect(document.documentElement).toHaveAttribute('data-theme', 'light');
    });
});
