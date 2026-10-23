import { LayersIcon, LockIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import type { PageStructurePart } from '@/services/apiService/genaix/types';

import { PartCard } from './PartCard';
import type { PartViewProps } from './partProps';

import styles from './MessageParts.module.css';

/**
 * `page_structure`: the blocks of the page being built, each with its construct and how far it got.
 * Streamed as merge patches, so the whole `blocks` array is replaced whenever one block moves on.
 */
export function PageStructurePartView({ part }: PartViewProps<PageStructurePart>) {
    const { t } = useTranslation();

    return (
        <PartCard icon={LayersIcon} title={part.label || t('parts.pageStructure.title')} footer={part.page_ref?.name}>
            <ul className={styles.blocks}>
                {(part.blocks ?? []).map((block) => (
                    <li key={block.id} className={styles.block}>
                        {block.verbatim && <LockIcon size={14} role="img" aria-label={t('parts.pageStructure.verbatim')} />}
                        <span className={styles.blockText}>
                            <span>{block.label}</span>
                            <span className={styles.meta}>
                                {[block.construct_keyword, block.tag_keyword, block.note].filter(Boolean).join(' · ')}
                            </span>
                        </span>
                        <span className={styles.badge} data-status={block.status}>{t(`parts.pageStructure.status.${block.status}`)}</span>
                    </li>
                ))}
            </ul>
        </PartCard>
    );
}
