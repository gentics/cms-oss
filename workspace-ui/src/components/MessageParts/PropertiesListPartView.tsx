import { SlidersHorizontalIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import type { PropertiesListPart } from '@/services/apiService/genaix/types';

import { PartCard } from './PartCard';
import type { PartViewProps } from './partProps';
import { RefToken } from './RefToken';

import styles from './MessageParts.module.css';

/**
 * `properties_list`: key/value pairs (draft `.propcard`). A value with a `ref` shows as its token.
 * `editable` is not offered here: the contract has no route that takes an edit of a transcript part.
 */
export function PropertiesListPartView({ part }: PartViewProps<PropertiesListPart>) {
    const { t } = useTranslation();

    return (
        <PartCard icon={SlidersHorizontalIcon} title={part.label || t('parts.properties.title')}>
            <dl className={styles.properties}>
                {(part.items ?? []).map((item) => (
                    <div key={item.key} className={styles.property}>
                        <dt className={styles.propertyKey}>{item.label}</dt>
                        <dd className={styles.propertyValue}>
                            {item.ref ? <RefToken value={{ ...item.ref, label: item.ref.label || item.value || undefined }} /> : (item.value ?? '–')}
                        </dd>
                    </div>
                ))}
            </dl>
        </PartCard>
    );
}
