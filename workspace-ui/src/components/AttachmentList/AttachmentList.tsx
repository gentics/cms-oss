import { ArrowLeftRightIcon, FileTextIcon, LockIcon, XIcon } from 'lucide-react';
import type { CSSProperties } from 'react';
import { useTranslation } from 'react-i18next';

import { IconButton } from '@/components/IconButton/IconButton';
import type { Attachment } from '@/hooks/useComposer';

import styles from './AttachmentList.module.css';

interface AttachmentListProps {
    attachments: Attachment[];
    onToggleMode: (id: string) => void;
    onRemove: (id: string) => void;
    /** While the message is sent: the files are uploaded, so each shows its progress and stays as it is. */
    isSending?: boolean;
    /** Share uploaded per attachment id, 0 to 1; a file not in it is still waiting for its turn. */
    progress?: Record<string, number>;
}

/**
 * The files attached in a composer, each as a source or verbatim (draft `renderAtt`). While sending,
 * each shows how much of it is uploaded, as a percentage and a bar along its bottom edge.
 */
export function AttachmentList({ attachments, onToggleMode, onRemove, isSending = false, progress = {} }: AttachmentListProps) {
    const { t } = useTranslation();

    if (attachments.length === 0) {
        return null;
    }

    return (
        <ul className={styles.attachments}>
            {attachments.map(({ id, file, mode }) => {
                const share = progress[id];

                return (
                    <li key={id} className={`${styles.attachment} ${mode === 'verbatim' ? styles.verbatim : ''}`}>
                        {mode === 'verbatim' ? <LockIcon size={14} /> : <FileTextIcon size={14} />}
                        <span className={styles.name}>{file.name}</span>
                        <span className={styles.mode}>{t(`composer.attachment.${mode}`)}</span>
                        {isSending && (
                            <span className={styles.progress}>
                                {share === undefined
                                    ? t('composer.attachment.waiting')
                                    : t('composer.attachment.progress', { percent: Math.round(share * 100) })}
                            </span>
                        )}
                        <IconButton
                            variant="ghost"
                            size="icon-xs"
                            label={`${t('composer.attachment.toggleMode')}: ${file.name}`}
                            disabled={isSending}
                            onClick={() => onToggleMode(id)}
                        >
                            <ArrowLeftRightIcon size={12} />
                        </IconButton>
                        <IconButton
                            variant="ghost"
                            size="icon-xs"
                            label={`${t('composer.attachment.remove')}: ${file.name}`}
                            disabled={isSending}
                            onClick={() => onRemove(id)}
                        >
                            <XIcon size={12} />
                        </IconButton>
                        {isSending && share !== undefined && (
                            <span
                                className={styles.bar}
                                role="progressbar"
                                aria-label={file.name}
                                aria-valuemin={0}
                                aria-valuemax={100}
                                aria-valuenow={Math.round(share * 100)}
                                style={{ '--share': share } as CSSProperties}
                            />
                        )}
                    </li>
                );
            })}
        </ul>
    );
}
