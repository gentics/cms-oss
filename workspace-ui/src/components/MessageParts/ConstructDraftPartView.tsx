import { CircleAlertIcon, CircleCheckIcon, InfoIcon, PuzzleIcon, TriangleAlertIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

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

/** `construct_draft`: the construct GenAIx drafted, its editable parts, the Handlebars template and its validation. */
export function ConstructDraftPartView({ part }: PartViewProps<ConstructDraftPart>) {
    const { t, i18n } = useTranslation();
    const language = i18n.language.split('-')[0] ?? 'en';
    const issues = part.validation?.issues ?? [];

    return (
        <PartCard icon={PuzzleIcon} title={localised(part.name, language) || part.keyword}>
            <div className={styles.section}>
                <span className={`${styles.mono} ${styles.meta}`}>{part.keyword}</span>
            </div>
            {(part.parts ?? []).length > 0 && (
                <div className={styles.section}>
                    <h4 className={styles.sectionTitle}>{t('parts.construct.parts')}</h4>
                    <ul className={styles.blocks}>
                        {part.parts.map((item) => (
                            <li key={item.keyword} className={styles.block}>
                                <span className={styles.blockText}>
                                    <span>{localised(item.name, language) || item.keyword}</span>
                                    <span className={`${styles.mono} ${styles.meta}`}>{item.keyword}</span>
                                </span>
                                {item.mandatory && <span className={styles.badge}>{t('parts.construct.mandatory')}</span>}
                            </li>
                        ))}
                    </ul>
                </div>
            )}
            {part.template && (
                <div className={styles.section}>
                    <h4 className={styles.sectionTitle}>{t('parts.construct.template')}</h4>
                    <pre className={styles.code}><code>{part.template}</code></pre>
                </div>
            )}
            {part.validation && (
                <div className={styles.section}>
                    <h4 className={styles.sectionTitle}>{t('parts.construct.validation')}</h4>
                    <ul className={styles.issues}>
                        {part.validation.ok && (
                            <li className={styles.issue}>
                                <CircleCheckIcon size={14} className={styles.ok} aria-hidden />
                                <span>{t('parts.construct.valid')}</span>
                            </li>
                        )}
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
                </div>
            )}
        </PartCard>
    );
}
