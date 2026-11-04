import { ExternalLinkIcon, EyeIcon, Maximize2Icon, Minimize2Icon, MonitorIcon, SlidersHorizontalIcon, SmartphoneIcon, XIcon } from 'lucide-react';
import type { Ref } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

import styles from './PreviewHeader.module.css';

export interface PreviewHeaderProps {
    /** The id of the title, which names the preview column. */
    titleId: string;
    /** The page in the CMS, opened read-only in a new tab ("Open"). */
    url: string;
    /** The pages to choose from, when the session wrote several: their names by key, the shown one, and the choice. */
    pages: { names: Record<string, string>; current: string; onPick: (key: string) => void } | null;
    /** The selected part's name, with a button to clear the selection; `null` when nothing is selected. */
    selection: { name: string; onClear: () => void } | null;
    /** The page settings button: whether they are open and toggling them; `null` for none. */
    settings: { open: boolean; onToggle: () => void } | null;
    /** The page settings button, where the focus returns when they close. */
    settingsButtonRef: Ref<HTMLButtonElement>;
    isMobile: boolean;
    onMobileChange: (isMobile: boolean) => void;
    isFullScreen: boolean;
    onFullScreenChange: (isFullScreen: boolean) => void;
}

/**
 * The head of the preview column: the title and, with several pages, the page select; "Open"; the selected part with
 * a button to clear it; the page settings; desktop or mobile width; full screen. In a narrow column the buttons move to
 * a second line.
 */
export function PreviewHeader({ titleId, url, pages, selection, settings, settingsButtonRef, isMobile, onMobileChange, isFullScreen, onFullScreenChange }: PreviewHeaderProps) {
    const { t } = useTranslation();
    const fullScreenLabel = isFullScreen ? t('preview.leaveFullScreen') : t('preview.fullScreen');

    return (
        <header className={styles.head}>
            <div className={styles.lead}>
                <h2 id={titleId} className={styles.title}>
                    <EyeIcon size={16} className={styles.titleIcon} aria-hidden="true" />
                    {t('preview.title')}
                </h2>
                {pages && (
                    <div className={styles.pages}>
                        <Select items={pages.names} value={pages.current} onValueChange={(key) => key !== null && pages.onPick(key)}>
                            <SelectTrigger aria-label={t('preview.pages.label')}>
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {Object.entries(pages.names).map(([key, name]) => <SelectItem key={key} value={key}>{name}</SelectItem>)}
                            </SelectContent>
                        </Select>
                    </div>
                )}
            </div>
            {/* A link in the look of a button, as "New" in the left column. */}
            <Button
                size="sm"
                variant="ghost"
                nativeButton={false}
                role={undefined}
                render={<a href={url} target="_blank" rel="noopener noreferrer" />}
            >
                <ExternalLinkIcon size={14} aria-hidden="true" />
                {t('preview.open')}
            </Button>
            {selection && (
                <span className={styles.selection}>
                    {t('preview.selection.selected', { name: selection.name })}
                    <Button variant="ghost" size="icon-xs" aria-label={t('preview.selection.clear')} title={t('preview.selection.clear')} onClick={selection.onClear}>
                        <XIcon size={12} />
                    </Button>
                </span>
            )}
            {settings && (
                <Button ref={settingsButtonRef} variant="secondary" size="sm" aria-expanded={settings.open} onClick={settings.onToggle}>
                    <SlidersHorizontalIcon size={14} />
                    {t('preview.settings.open')}
                </Button>
            )}
            <div className={styles.viewports} role="group" aria-label={t('preview.viewport')}>
                <Button variant="ghost" size="icon" aria-label={t('preview.desktop')} title={t('preview.desktop')} aria-pressed={!isMobile} onClick={() => onMobileChange(false)}>
                    <MonitorIcon size={16} />
                </Button>
                <Button variant="ghost" size="icon" aria-label={t('preview.mobile')} title={t('preview.mobile')} aria-pressed={isMobile} onClick={() => onMobileChange(true)}>
                    <SmartphoneIcon size={16} />
                </Button>
                <Button variant="ghost" size="icon" aria-label={fullScreenLabel} title={fullScreenLabel} aria-pressed={isFullScreen} onClick={() => onFullScreenChange(!isFullScreen)}>
                    {isFullScreen ? <Minimize2Icon size={16} /> : <Maximize2Icon size={16} />}
                </Button>
            </div>
        </header>
    );
}
