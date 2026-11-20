import { Undo2Icon } from 'lucide-react';
import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { requestedBy } from '@/helper/taskDisplay/taskDisplay';
import { useAssignBackTask, useCmsUsers } from '@/hooks/useCmsTasks';
import type { CmsTask } from '@/services/cmsApiService/cmsTaskApi';

import styles from './AssignBackDialog.module.css';

interface AssignBackDialogProps {
    task: CmsTask;
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

/** Hands a page back for revision, like "Assign" in the CMS UI. */
export function AssignBackDialog({ task, open, onOpenChange }: AssignBackDialogProps) {
    const { t } = useTranslation();
    const recipientsId = useId();
    const messageId = useId();
    const users = useCmsUsers(open);
    const assignBack = useAssignBackTask();
    const requester = requestedBy(task);
    const [selected, setSelected] = useState<number[]>(requester ? [requester.id] : []);
    const [message, setMessage] = useState('');

    const listed = users.data?.map((user) => ({
        id: user.id,
        name: [user.firstName, user.lastName].filter(Boolean).join(' ') || user.login || String(user.id),
    })) ?? [];
    const recipients = listed.length > 0 ? listed : requester ? [requester] : [];

    function toggle(id: number, checked: boolean) {
        setSelected((current) => (checked ? [...current, id] : current.filter((selectedId) => selectedId !== id)));
    }

    function submit() {
        assignBack.mutate({ task, userIds: selected, message }, { onSuccess: () => onOpenChange(false) });
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>{t('tasks.assignBack.title')}</DialogTitle>
                </DialogHeader>
                <DialogBody>
                    <div className={styles.field} role="group" aria-labelledby={recipientsId}>
                        <span id={recipientsId} className={styles.label}>{t('tasks.assignBack.recipients')}</span>
                        <div className={styles.recipients}>
                            {recipients.map((recipient) => (
                                <Label key={recipient.id}>
                                    <Checkbox
                                        checked={selected.includes(recipient.id)}
                                        onCheckedChange={(checked) => toggle(recipient.id, checked)}
                                    />
                                    {recipient.name}
                                </Label>
                            ))}
                        </div>
                    </div>
                    <div className={styles.field}>
                        <Label htmlFor={messageId}>{t('tasks.assignBack.message')}</Label>
                        <Textarea id={messageId} value={message} onChange={(event) => setMessage(event.target.value)} rows={3} />
                    </div>
                </DialogBody>
                <DialogFooter>
                    <Button variant="secondary" size="sm" onClick={() => onOpenChange(false)}>{t('tasks.assignBack.cancel')}</Button>
                    <Button variant="primary" size="sm" disabled={selected.length === 0 || assignBack.isPending} onClick={submit}>
                        <Undo2Icon size={14} />
                        {t('tasks.actions.assignBack')}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
