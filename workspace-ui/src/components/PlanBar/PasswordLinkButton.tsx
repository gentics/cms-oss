import { useMutation } from '@tanstack/react-query';
import { CopyIcon, LinkIcon } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { useToast } from '@/components/ui/use-toast';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { createdUsersOf, isFinished } from '@/helper/planActions/planActions';
import { usePlanState } from '@/hooks/useGenaixQueries';
import type { CmsObjectRef, Plan } from '@/services/apiService/genaix/types';
import { type CmsPasswordLink, cmsPasswordLinkUrl, createCmsPasswordLink } from '@/services/cmsApiService/cmsApiService';

import styles from './PlanBar.module.css';

/**
 * "Show password link": one button for each CMS user the finished plan created (`createdUsersOf` on its
 * `PlanState`). `create_user` gives GenAIx only the link's expiry, so the link itself is asked for here, from the
 * browser with the user's own CMS session (`createCmsPasswordLink`), never through GenAIx.
 */
export function PasswordLinkButton({ sessionId, plan }: { sessionId: string; plan: Plan }) {
    const state = usePlanState(sessionId, isFinished(plan.status) ? plan.id : undefined);
    const users = createdUsersOf(state.data?.cms_objects ?? []);

    return users.map((user) => <PasswordLinkDialog key={user.id} user={user} withName={users.length > 1} />);
}

/**
 * Issues a new link for `user` each time it opens, which replaces the user's earlier one in the CMS, and shows it
 * to copy. The link is kept in this component's state only and dropped when the dialog closes.
 */
function PasswordLinkDialog({ user, withName }: { user: CmsObjectRef; withName: boolean }) {
    const { t, i18n } = useTranslation();
    const toast = useToast();
    const [isOpen, setOpen] = useState(false);
    const [link, setLink] = useState<CmsPasswordLink>();
    // gcTime 0: the token does not stay in the mutation cache once the dialog is closed.
    const passwordLink = useMutation({ mutationFn: () => createCmsPasswordLink(user.id), gcTime: 0, onSuccess: setLink });
    const url = link ? cmsPasswordLinkUrl(link.token) : undefined;
    const name = user.name ?? String(user.id);

    function onOpenChange(open: boolean) {
        setOpen(open);
        setLink(undefined);
        passwordLink.reset();

        if (open) {
            passwordLink.mutate();
        }
    }

    async function copy() {
        if (url) {
            await navigator.clipboard.writeText(url);
            toast.add({ type: 'success', title: t('planBar.passwordLink.copied') });
        }
    }

    return (
        <Dialog open={isOpen} onOpenChange={onOpenChange}>
            <DialogTrigger render={<Button variant="secondary" size="sm" />}>
                <LinkIcon size={14} aria-hidden />
                {withName ? t('planBar.passwordLink.showFor', { name }) : t('planBar.passwordLink.show')}
            </DialogTrigger>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>{t('planBar.passwordLink.title', { name })}</DialogTitle>
                </DialogHeader>
                <DialogBody>
                    {passwordLink.isPending && <p>{t('planBar.passwordLink.loading')}</p>}
                    {passwordLink.isError && <p role="alert">{t(errorMessageKey(passwordLink.error))}</p>}
                    {link && url && (
                        <div className={styles.passwordLink}>
                            <code className={styles.passwordLinkUrl}>{url}</code>
                            <p>
                                {t('planBar.passwordLink.expires', {
                                    time: new Intl.DateTimeFormat(i18n.language, { dateStyle: 'medium', timeStyle: 'short' }).format(link.expiresAt * 1000),
                                })}
                            </p>
                        </div>
                    )}
                </DialogBody>
                <DialogFooter>
                    <Button variant="primary" size="sm" disabled={!url} onClick={() => void copy()}>
                        <CopyIcon size={14} aria-hidden />
                        {t('planBar.passwordLink.copy')}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
