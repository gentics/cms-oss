import { useTranslation } from 'react-i18next';

import { useThemeStore } from '@/store/useThemeStore';

import styles from './Topbar.module.css';

// Icons: the inline paths of maybe_final_draft.html (`left_panel_close/open`, `dark_mode`, `light_mode`);
// stroke width per design.md §9.
const iconProps = {
    viewBox: '0 0 24 24',
    width: 20,
    height: 20,
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.7,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    'aria-hidden': true,
} as const;

interface TopbarProps {
    isLeftColumnVisible: boolean;
    onToggleLeftColumn: () => void;
}

/** The bar above the workspace: left-column toggle, brand, light/dark toggle. */
export function Topbar({ isLeftColumnVisible, onToggleLeftColumn }: TopbarProps) {
    const { t } = useTranslation();
    const theme = useThemeStore((state) => state.theme);
    const toggleTheme = useThemeStore((state) => state.toggleTheme);
    const leftColumnLabel = t(isLeftColumnVisible ? 'topbar.hideLeftColumn' : 'topbar.showLeftColumn');
    const themeLabel = t(theme === 'dark' ? 'topbar.lightMode' : 'topbar.darkMode');

    return (
        <header className={styles.topbar}>
            <button
                type="button"
                className={styles.iconButton}
                aria-label={leftColumnLabel}
                title={leftColumnLabel}
                onClick={onToggleLeftColumn}
            >
                <svg {...iconProps}>
                    {isLeftColumnVisible
                        ? <path d="M4 5h16v14H4zM9.8 5v14M15 9.6L12.6 12l2.4 2.4" />
                        : <path d="M4 5h16v14H4zM9.8 5v14M12.6 9.6L15 12l-2.4 2.4" />}
                </svg>
            </button>

            <div className={styles.brand}>
                <i className={styles.logo} aria-hidden="true" />
                <span>{t('topbar.brand')}</span>
            </div>

            <div className={styles.spacer} />

            <button
                type="button"
                className={styles.iconButton}
                aria-label={themeLabel}
                title={themeLabel}
                onClick={toggleTheme}
            >
                <svg {...iconProps}>
                    {theme === 'dark'
                        ? (
                            <>
                                <circle cx="12" cy="12" r="4" />
                                <path d="M12 2.4v2.2M12 19.4v2.2M2.4 12h2.2M19.4 12h2.2M5.2 5.2l1.6 1.6M17.2 17.2l1.6 1.6M18.8 5.2l-1.6 1.6M6.8 17.2l-1.6 1.6" />
                            </>
                        )
                        : <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />}
                </svg>
            </button>
        </header>
    );
}
