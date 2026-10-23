import { ChevronDownIcon, ChevronUpIcon, CircleAlertIcon, CircleCheckIcon, CircleDashedIcon, InfoIcon, PuzzleIcon, TriangleAlertIcon } from 'lucide-react';
import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import type { ConstructDraftPart } from '@/services/apiService/genaix/types';

import { PartCard } from './PartCard';
import type { PartViewProps } from './partProps';

import styles from './MessageParts.module.css';

const SEVERITY_ICONS = { error: CircleAlertIcon, warning: TriangleAlertIcon, info: InfoIcon };

// A localised name (keyed by language code) in the UI language, else English, else the first one.
function localised(names: Record<string, string> | undefined, language: string): string | undefined {
    if (!names) {
        return undefined;
    }

    return names[language] ?? names.en ?? Object.values(names)[0];
}

/**
 * `construct_draft`: a new construct (tag type), in one line: its name and whether it was created in
 * the CMS (`created_ref`, set once `create_construct` ran; before, the draft waits for the user's
 * confirmation, contract §14 S3), with the number of the validation's findings when there are some.
 * "Details" opens the rest: keyword and validity, the editable parts, the findings, and the Handlebars
 * template, folded once more until opened.
 */
export function ConstructDraftPartView({ part }: PartViewProps<ConstructDraftPart>) {
    const { t, i18n } = useTranslation();
    const language = i18n.language.split('-')[0] ?? 'en';
    const issues = part.validation?.issues ?? [];
    const parts = part.parts ?? [];
    const [isOpen, setIsOpen] = useState(false);
    const [isTemplateOpen, setIsTemplateOpen] = useState(false);
    const detailsId = useId();
    const templateId = useId();
    const isValid = part.validation?.ok;
    const isCreated = part.created_ref !== undefined;
    const name = localised(part.name, language) || part.keyword;

    return (
        <PartCard icon={PuzzleIcon} title={t('parts.construct.title')}>
            <div className={styles.constructLine}>
                <span className={styles.constructName}>{name}</span>
                <span className={isCreated ? styles.ok : styles.meta}>
                    {isCreated ? <CircleCheckIcon size={12} aria-hidden /> : <CircleDashedIcon size={12} aria-hidden />}
                    {t(isCreated ? 'parts.construct.created' : 'parts.construct.drafted')}
                </span>
                {issues.length > 0 && <span className={styles.meta}>{t('parts.construct.findings', { count: issues.length })}</span>}
                <Button variant="ghost" size="sm" aria-expanded={isOpen} aria-controls={detailsId} onClick={() => setIsOpen((open) => !open)}>
                    {t('parts.construct.details')}
                    {isOpen ? <ChevronUpIcon size={14} /> : <ChevronDownIcon size={14} />}
                </Button>
            </div>
            <div id={detailsId} className={styles.constructDetails} hidden={!isOpen}>
                <p className={styles.constructSummary}>
                    <span className={styles.mono}>{part.keyword}</span>
                    {part.validation && (
                        <span className={isValid ? styles.ok : styles.bad}>
                            {isValid ? <CircleCheckIcon size={12} aria-hidden /> : <CircleAlertIcon size={12} aria-hidden />}
                            {t(isValid ? 'parts.construct.valid' : 'parts.construct.invalid')}
                        </span>
                    )}
                </p>
                {parts.length > 0 && (
                    <ul className={styles.constructParts} aria-label={t('parts.construct.parts')}>
                        {parts.map((item) => (
                            <li key={item.keyword} className={styles.constructPart}>
                                <span>{localised(item.name, language) || item.keyword}</span>
                                <span className={`${styles.mono} ${styles.meta}`}>{item.keyword}</span>
                                {item.mandatory && <span className={styles.meta}>{t('parts.construct.mandatory')}</span>}
                            </li>
                        ))}
                    </ul>
                )}
                {issues.length > 0 && (
                    <ul className={styles.issues} aria-label={t('parts.construct.validation')}>
                        {issues.map((issue, index) => {
                            const Icon = SEVERITY_ICONS[issue.severity];

                            return (
                                <li key={index} className={styles.issue} data-severity={issue.severity}>
                                    <Icon size={14} role="img" aria-label={t(`parts.construct.severity.${issue.severity}`)} />
                                    <span>
                                        {issue.message}
                                        {issue.line !== undefined && <span className={styles.meta}>{` · ${t('parts.construct.line', { line: issue.line })}`}</span>}
                                    </span>
                                </li>
                            );
                        })}
                    </ul>
                )}
                {part.template && (
                    <div className={styles.constructTemplate}>
                        <Button variant="ghost" size="sm" aria-expanded={isTemplateOpen} aria-controls={templateId} onClick={() => setIsTemplateOpen((open) => !open)}>
                            {t('parts.construct.template')}
                            {isTemplateOpen ? <ChevronUpIcon size={14} /> : <ChevronDownIcon size={14} />}
                        </Button>
                        <pre id={templateId} className={styles.code} hidden={!isTemplateOpen}><code>{part.template}</code></pre>
                    </div>
                )}
            </div>
        </PartCard>
    );
}
