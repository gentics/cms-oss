import { CircleAlertIcon, PlusIcon, XIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { RefToken } from '@/components/MessageParts/RefToken';
import { Button } from '@/components/ui/button';
import { DropdownMenu, DropdownMenuCheckboxItem, DropdownMenuContent, DropdownMenuGroup, DropdownMenuLabel, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { parseGuidelineSlug, unknownGuidelines } from '@/helper/guidelines/guidelines';
import { useCmsGuidelines } from '@/hooks/useCmsQueries';
import { useSession, useSetSessionGuidelines } from '@/hooks/useGenaixQueries';
import type { Plan } from '@/services/apiService/genaix/types';

import styles from './PlanGuidelines.module.css';

interface PlanGuidelinesProps {
    plan: Plan;
    sessionId: string;
}

/**
 * The guidelines the user added to the session (`SessionContext.guidelines`), in the plan card. Only
 * while the plan is proposed can the user add one of the available guidelines of the session's node
 * (the files without `.default`, `useCmsGuidelines`) or take one out again (`useSetSessionGuidelines`).
 * The default guidelines are GenAIx's business: they are neither listed nor offered, and kept as they
 * are in the context. A guideline that no longer exists, because it was removed or renamed, is named
 * with a warning; the UI tells that from the node's guidelines, so nothing is warned while they load.
 * Nothing is shown while there is nothing to list and nothing to add.
 */
export function PlanGuidelines({ plan, sessionId }: PlanGuidelinesProps) {
    const { t } = useTranslation();
    const context = useSession(sessionId).data?.context;
    const guidelines = useCmsGuidelines(context?.node_id).data;
    const setGuidelines = useSetSessionGuidelines(sessionId);
    const selected = context?.guidelines ?? [];
    const isEditable = plan.status === 'proposed';
    const available = (guidelines ?? []).filter(({ isDefault }) => !isDefault);
    const added = available.filter(({ slug }) => selected.includes(slug));
    const unknown = guidelines ? unknownGuidelines(selected, guidelines) : [];
    const canAdd = isEditable && available.length > 0;

    if (added.length === 0 && unknown.length === 0 && !canAdd) {
        return null;
    }

    function toggle(slug: string, isChecked: boolean) {
        setGuidelines.mutate(isChecked ? [...selected, slug] : selected.filter((id) => id !== slug));
    }

    function removeButton(slug: string, name: string) {
        return isEditable && (
            <Button
                variant="ghost"
                size="icon"
                aria-label={t('planGuidelines.remove', { name })}
                disabled={setGuidelines.isPending}
                onClick={() => toggle(slug, false)}
            >
                <XIcon size={14} />
            </Button>
        );
    }

    return (
        <section className={styles.guidelines} aria-label={t('planGuidelines.title')}>
            <span className={styles.title}>{t('planGuidelines.title')}</span>
            {(added.length > 0 || unknown.length > 0) && (
                <ul className={styles.list}>
                    {added.map(({ slug, name }) => (
                        <li key={slug} className={styles.item}>
                            <RefToken value={{ type: 'guideline', id: slug, label: name }} />
                            {removeButton(slug, name)}
                        </li>
                    ))}
                    {unknown.map((slug) => {
                        const name = parseGuidelineSlug(slug)?.name ?? slug;

                        return (
                            <li key={slug} className={styles.item}>
                                <span className={styles.missing}>
                                    <CircleAlertIcon size={14} aria-hidden />
                                    {t('planGuidelines.missing', { name })}
                                </span>
                                {removeButton(slug, name)}
                            </li>
                        );
                    })}
                </ul>
            )}
            {canAdd && (
                <DropdownMenu>
                    <DropdownMenuTrigger render={<Button variant="ghost" size="sm" disabled={setGuidelines.isPending} />}>
                        <PlusIcon size={14} aria-hidden />
                        {t('planGuidelines.add')}
                    </DropdownMenuTrigger>
                    <DropdownMenuContent side="top" align="start">
                        <DropdownMenuGroup>
                            <DropdownMenuLabel>{t('planGuidelines.available')}</DropdownMenuLabel>
                            {available.map(({ slug, name }) => (
                                <DropdownMenuCheckboxItem
                                    key={slug}
                                    checked={selected.includes(slug)}
                                    disabled={setGuidelines.isPending}
                                    onCheckedChange={(isChecked) => toggle(slug, isChecked)}
                                >
                                    {name}
                                </DropdownMenuCheckboxItem>
                            ))}
                        </DropdownMenuGroup>
                    </DropdownMenuContent>
                </DropdownMenu>
            )}
        </section>
    );
}
