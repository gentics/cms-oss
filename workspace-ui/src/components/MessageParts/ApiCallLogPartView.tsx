import { ChevronDownIcon, ChevronUpIcon } from 'lucide-react';
import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import type { ApiCallLogPart } from '@/services/apiService/genaix/types';

import type { PartViewProps } from './partProps';

import styles from './MessageParts.module.css';

/**
 * `api_call_log`: the CMS calls a step made, as quiet meta text like the sources (no card). Collapsed
 * to the number of calls (and of failed ones) with a chevron; opened, each call with its status and
 * what it did, so what was written stays visible.
 */
export function ApiCallLogPartView({ part }: PartViewProps<ApiCallLogPart>) {
    const { t } = useTranslation();
    const [isOpen, setIsOpen] = useState(false);
    const listId = useId();
    const calls = part.calls ?? [];
    const failed = calls.filter((call) => call.status >= 400).length;

    return (
        <section className={styles.sources} aria-label={part.label || t('parts.apiCalls.title')}>
            <Button variant="quiet" size="text" aria-expanded={isOpen} aria-controls={listId} onClick={() => setIsOpen((open) => !open)}>
                {t('parts.apiCalls.toggle', { count: calls.length })}
                {failed > 0 && <>{' '}<span className={styles.bad}>{t('parts.apiCalls.failed', { count: failed })}</span></>}
                {isOpen ? <ChevronUpIcon size={12} /> : <ChevronDownIcon size={12} />}
            </Button>
            <div id={listId} className={styles.apiCalls} hidden={!isOpen}>
                <table className={styles.table}>
                    <thead>
                        <tr>
                            <th scope="col">{t('parts.apiCalls.call')}</th>
                            <th scope="col">{t('parts.apiCalls.status')}</th>
                            <th scope="col">{t('parts.apiCalls.summary')}</th>
                        </tr>
                    </thead>
                    <tbody>
                        {calls.map((call, index) => (
                            <tr key={index}>
                                <td className={styles.mono}>{`${call.method} ${call.path}`}</td>
                                <td className={`${styles.number} ${call.status < 400 ? styles.ok : styles.bad}`}>{call.status}</td>
                                <td>{call.summary}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </section>
    );
}
