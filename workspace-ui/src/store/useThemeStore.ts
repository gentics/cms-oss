import { create } from 'zustand';

export type Theme = 'light' | 'dark';

interface ThemeState {
    theme: Theme;
    /** Switches between light and dark and pins the choice on `<html data-theme>` (src/index.css). */
    toggleTheme: () => void;
}

/** Light or dark. Starts light, whatever the system says (`index.html` pins `data-theme="light"`). */
export const useThemeStore = create<ThemeState>((set, get) => ({
    theme: 'light',

    toggleTheme: () => {
        const theme = get().theme === 'dark' ? 'light' : 'dark';

        document.documentElement.dataset.theme = theme;
        set({ theme });
    },
}));
