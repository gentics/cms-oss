import { DatabaseIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import type { ApiCallLogPart } from '@/services/apiService/genaix/types';

import { PartCard } from './PartCard';
import type { PartViewProps } from './partProps';

import styles from './MessageParts.module.css';

/** `api_call_log`: the CMS calls a step made, so what was written stays visible. */
export function ApiCallLogPartView({ part }: PartViewProps<ApiCallLogPart>) {
    const { t } = useTranslation();

    return (
        <PartCard icon={DatabaseIcon} title={part.label || t('parts.apiCalls.title')}>
            <div className={styles.tableScroll}>
                <table className={styles.table}>
                    <thead>
                        <tr>
                            <th scope="col">{t('parts.apiCalls.call')}</th>
                            <th scope="col">{t('parts.apiCalls.status')}</th>
                            <th scope="col">{t('parts.apiCalls.summary')}</th>
                        </tr>
                    </thead>
                    <tbody>
                        {(part.calls ?? []).map((call, index) => (
                            <tr key={index}>
                                <td className={styles.mono}>{`${call.method} ${call.path}`}</td>
                                <td className={`${styles.number} ${call.status < 400 ? styles.ok : styles.bad}`}>{call.status}</td>
                                <td>{call.summary}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </PartCard>
    );
}
