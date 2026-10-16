import { DownloadIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { sessionFileContentUrl } from '@/services/apiService/apiService';
import type { FileRefPart } from '@/services/apiService/genaix/types';

import type { PartViewProps } from './partProps';

import styles from './MessageParts.module.css';

/** `file_ref`: a download chip for a session file, through the content route. */
export function FileRefPartView({ part, sessionId }: PartViewProps<FileRefPart>) {
    const { t } = useTranslation();
    const name = part.name || t('parts.file.unnamed');

    return (
        <a className={styles.file} href={sessionFileContentUrl(sessionId, part.file_id)} download={part.name ?? true}>
            <DownloadIcon size={14} aria-hidden />
            <span>{name}</span>
            <span className={styles.meta}>{t('parts.file.download')}</span>
        </a>
    );
}
