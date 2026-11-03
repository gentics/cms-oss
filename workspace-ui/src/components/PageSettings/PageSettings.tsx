import { CheckIcon, SlidersHorizontalIcon, XIcon } from 'lucide-react';
import { type FormEvent, type KeyboardEvent, type Ref, useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import type { PageSettingsChange } from '@/helper/composerParts/composerParts';
import { useCmsPage, useCmsTemplates } from '@/hooks/useCmsQueries';
import type { CmsPage } from '@/services/cmsApiService/cmsApiService';

import styles from './PageSettings.module.css';

// The settings that can be changed, as the CMS tool `update_page_properties` takes them, and their labels.
const TEXT_FIELDS = [
    { key: 'name', label: 'preview.settings.name' },
    { key: 'fileName', label: 'preview.settings.fileName' },
    { key: 'description', label: 'preview.settings.description' },
    { key: 'priority', label: 'preview.settings.priority' },
    { key: 'niceUrl', label: 'preview.settings.niceUrl' },
] as const;

type TextField = (typeof TEXT_FIELDS)[number]['key'];

type Values = Record<TextField | 'templateId', string>;

// The page's settings as the form holds them: every value as text, a missing one empty.
function valuesOf(page: CmsPage): Values {
    return {
        name: page.name,
        fileName: page.fileName ?? '',
        description: page.description ?? '',
        priority: page.priority === undefined ? '' : String(page.priority),
        niceUrl: page.niceUrl ?? '',
        templateId: page.templateId === undefined ? '' : String(page.templateId),
    };
}

export interface PageSettingsProps {
    /** The page whose settings are shown, and its node, if known. */
    pageId: number;
    nodeId?: number;
    /** Whether the changes can be sent now: not while a run is working or waiting. */
    canSend: boolean;
    onClose: () => void;
    /** Sends the changed settings; rejects when they were not sent, so the sheet stays open. */
    onApply: (changes: PageSettingsChange[]) => Promise<void>;
    /** The sheet, to move the focus into it when it opens. */
    ref?: Ref<HTMLElement>;
}

/**
 * The page settings (final_draft.html "PAGE SETTINGS"): a sheet over the preview with the page's
 * settings as the CMS has them now (`useCmsPage`, loaded each time the sheet opens). Name, filename,
 * description, priority, nice URL and template can be changed; language and the dates are shown only.
 * Apply hands on the changed settings, each with its label, the template as a `template` reference.
 * Escape closes the sheet, as its close button does.
 */
export function PageSettings({ pageId, nodeId, canSend, onClose, onApply, ref }: PageSettingsProps) {
    const { t } = useTranslation();
    const titleId = useId();
    const page = useCmsPage(pageId, nodeId);
    // Only the page as loaded for this sheet, not an older one from the cache.
    const loaded = page.isFetchedAfterMount ? page.data : undefined;

    // Escape closes the sheet, unless something in it (an open select) already handled it; full screen stays.
    function handleKeyDown(event: KeyboardEvent<HTMLElement>) {
        if (event.key === 'Escape' && !event.defaultPrevented) {
            event.preventDefault();
            onClose();
        }
    }

    return (
        <section ref={ref} className={styles.sheet} aria-labelledby={titleId} tabIndex={-1} onKeyDown={handleKeyDown}>
            <div className={styles.head}>
                <SlidersHorizontalIcon size={16} className={styles.icon} aria-hidden="true" />
                <h3 id={titleId} className={styles.title}>{t('preview.settings.title')}</h3>
                <Button variant="ghost" size="icon" aria-label={t('preview.settings.close')} title={t('preview.settings.close')} onClick={onClose}>
                    <XIcon size={16} />
                </Button>
            </div>
            {loaded
                ? <PageSettingsForm page={loaded} nodeId={nodeId} canSend={canSend} onApply={onApply} />
                : <p className={styles.status} role="status">{t(page.isError ? 'preview.settings.failed' : 'preview.settings.loading')}</p>}
        </section>
    );
}

function PageSettingsForm({ page, nodeId, canSend, onApply }: { page: CmsPage; nodeId?: number; canSend: boolean; onApply: PageSettingsProps['onApply'] }) {
    const { t, i18n } = useTranslation();
    const id = useId();
    const [original] = useState(() => valuesOf(page));
    const [values, setValues] = useState(original);
    const [isSending, setIsSending] = useState(false);
    const templates = useCmsTemplates(page.folderId, nodeId);
    const templateItems = Object.fromEntries((templates.data ?? []).map((template) => [String(template.id), template.name]));
    const changedText = TEXT_FIELDS.filter(({ key }) => values[key].trim() !== original[key].trim());
    const isTemplateChanged = values.templateId !== original.templateId && values.templateId !== '';
    const canApply = canSend && !isSending && (changedText.length > 0 || isTemplateChanged);

    function set(key: keyof Values, value: string) {
        setValues((current) => ({ ...current, [key]: value }));
    }

    function date(seconds: number): string {
        return new Date(seconds * 1000).toLocaleDateString(i18n.language);
    }

    async function apply(event: FormEvent) {
        event.preventDefault();

        if (!canApply) {
            return;
        }

        const changes: PageSettingsChange[] = changedText.map(({ key, label }) => ({ label: t(label), value: values[key].trim() }));

        if (isTemplateChanged) {
            changes.push({
                label: t('preview.settings.template'),
                value: { type: 'template', id: values.templateId, ...(nodeId !== undefined ? { node_id: nodeId } : {}), label: templateItems[values.templateId] ?? values.templateId },
            });
        }

        setIsSending(true);

        try {
            await onApply(changes);
        } catch {
            // Not sent: the caller has said why; the sheet and its values stay for another try.
        } finally {
            setIsSending(false);
        }
    }

    return (
        <form className={styles.form} onSubmit={(event) => void apply(event)}>
            <div className={styles.fields}>
                {TEXT_FIELDS.map(({ key, label }) => (
                    <div key={key} className={`${styles.field} ${key === 'description' ? styles.wide : ''}`}>
                        <Label htmlFor={`${id}-${key}`}>{t(label)}</Label>
                        {key === 'description'
                            ? <Textarea id={`${id}-${key}`} value={values[key]} onChange={(event) => set(key, event.target.value)} />
                            : (
                                <Input
                                    id={`${id}-${key}`}
                                    type={key === 'priority' ? 'number' : 'text'}
                                    min={key === 'priority' ? 1 : undefined}
                                    value={values[key]}
                                    onChange={(event) => set(key, event.target.value)}
                                />
                            )}
                    </div>
                ))}
                <div className={styles.field}>
                    <Label id={`${id}-template-label`}>{t('preview.settings.template')}</Label>
                    <Select items={templateItems} value={values.templateId || null} onValueChange={(next) => set('templateId', next === null ? '' : String(next))} disabled={!templates.isSuccess}>
                        <SelectTrigger aria-labelledby={`${id}-template-label`}>
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {(templates.data ?? []).map((template) => <SelectItem key={template.id} value={String(template.id)}>{template.name}</SelectItem>)}
                        </SelectContent>
                    </Select>
                </div>
            </div>
            {/* Kept by the CMS: shown, not changed here. */}
            <dl className={styles.readOnly}>
                <div>
                    <dt>{t('preview.settings.language')}</dt>
                    <dd>{page.language ?? '—'}</dd>
                </div>
                <div>
                    <dt>{t('preview.settings.created')}</dt>
                    <dd>{date(page.cdate)}</dd>
                </div>
                <div>
                    <dt>{t('preview.settings.edited')}</dt>
                    <dd>{date(page.edate)}</dd>
                </div>
            </dl>
            <div className={styles.foot}>
                <Button type="submit" variant="primary" size="sm" disabled={!canApply}>
                    <CheckIcon size={14} />
                    {t('preview.settings.apply')}
                </Button>
            </div>
        </form>
    );
}
