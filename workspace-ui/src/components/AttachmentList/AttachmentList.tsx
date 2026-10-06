import { ArrowLeftRightIcon, FileTextIcon, LockIcon, XIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { IconButton } from '@/components/IconButton/IconButton';
import type { Attachment } from '@/hooks/useComposer';

import styles from './AttachmentList.module.css';

interface AttachmentListProps {
    attachments: Attachment[];
    onToggleMode: (id: string) => void;
    onRemove: (id: string) => void;
}

/** The files attached in a composer, each as a source or verbatim (draft `renderAtt`). */
export function AttachmentList({ attachments, onToggleMode, onRemove }: AttachmentListProps) {
    const { t } = useTranslation();

    if (attachments.length === 0) {
        return null;
    }

    return (
        <ul className={styles.attachments}>
            {attachments.map(({ id, file, mode }) => (
                <li key={id} className={`${styles.attachment} ${mode === 'verbatim' ? styles.verbatim : ''}`}>
                    {mode === 'verbatim' ? <LockIcon size={14} /> : <FileTextIcon size={14} />}
                    <span className={styles.name}>{file.name}</span>
                    <span className={styles.mode}>{t(`composer.attachment.${mode}`)}</span>
                    <IconButton
                        variant="ghost"
                        size="icon-xs"
                        label={`${t('composer.attachment.toggleMode')}: ${file.name}`}
                        onClick={() => onToggleMode(id)}
                    >
                        <ArrowLeftRightIcon size={12} />
                    </IconButton>
                    <IconButton
                        variant="ghost"
                        size="icon-xs"
                        label={`${t('composer.attachment.remove')}: ${file.name}`}
                        onClick={() => onRemove(id)}
                    >
                        <XIcon size={12} />
                    </IconButton>
                </li>
            ))}
        </ul>
    );
}
