import { Link } from '@tanstack/react-router';
import { MoonIcon, PanelLeftCloseIcon, PanelLeftOpenIcon, SunIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { useThemeStore } from '@/store/useThemeStore';

import styles from './Topbar.module.css';

interface TopbarProps {
    /** Whether the left column (the sessions) is shown, for the toggle's icon and label. Default `true`. */
    isLeftColumnVisible?: boolean;
    /** Without it there is no toggle: the far left button that hides and shows the left column. */
    onToggleLeftColumn?: () => void;
}

/**
 * The bar above the workspace: at the far left the toggle of the left column (pages with one), the
 * brand, which leads home to the dashboard, the language switch (final_draft.html `#langBtn`) and
 * the light/dark toggle (icons per design.md §9). Phones (≤ 640 px) have no column toggle here; they
 * switch columns with the buttons in the columns (`WorkspaceLayout`).
 */
export function Topbar({ isLeftColumnVisible = true, onToggleLeftColumn }: TopbarProps) {
    const { t, i18n } = useTranslation();
    const theme = useThemeStore((state) => state.theme);
    const toggleTheme = useThemeStore((state) => state.toggleTheme);
    const themeLabel = t(theme === 'dark' ? 'topbar.lightMode' : 'topbar.darkMode');
    const leftColumnLabel = t(isLeftColumnVisible ? 'workspace.hideLeftColumn' : 'workspace.showLeftColumn');
    // The app is in English and German: the button shows the current one and switches to the other.
    const nextLanguage = i18n.language === 'de' ? 'en' : 'de';
    const languageLabel = t('topbar.switchLanguage', { language: t(`topbar.languages.${nextLanguage}`) });

    function switchLanguage() {
        document.documentElement.lang = nextLanguage;
        void i18n.changeLanguage(nextLanguage);
    }

    return (
        <header className={styles.topbar}>
            {onToggleLeftColumn && (
                <span className={styles.columnToggle}>
                    <Button variant="ghost" size="icon" aria-label={leftColumnLabel} title={leftColumnLabel} onClick={onToggleLeftColumn}>
                        {isLeftColumnVisible ? <PanelLeftCloseIcon size={20} /> : <PanelLeftOpenIcon size={20} />}
                    </Button>
                </span>
            )}
            {/* A link in the look of a ghost button: `nativeButton={false}` for the `<a>`, and no
                `role="button"`, which Base UI gives every other element. */}
            <Button variant="ghost" size="brand" nativeButton={false} role={undefined} render={<Link to="/" />} title={t('topbar.home')}>
                <i className={styles.logo} aria-hidden="true" />
                {t('topbar.brand')}
            </Button>

            <div className={styles.spacer} />

            <Button variant="ghost" size="icon" aria-label={languageLabel} title={languageLabel} onClick={switchLanguage}>
                <span className={styles.language}>{i18n.language.toUpperCase()}</span>
            </Button>

            <Button variant="ghost" size="icon" aria-label={themeLabel} title={themeLabel} onClick={toggleTheme}>
                {theme === 'dark' ? <SunIcon size={20} /> : <MoonIcon size={20} />}
            </Button>
        </header>
    );
}
