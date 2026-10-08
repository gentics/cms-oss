import { ArrowDownIcon, ArrowUpIcon, CheckCheckIcon, CheckIcon, type LucideIcon, MinusIcon, PencilIcon, PlusIcon, RouteIcon, XIcon } from 'lucide-react';
import { type KeyboardEvent, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { PartCard } from '@/components/MessageParts/PartCard';
import { RefToken } from '@/components/MessageParts/RefToken';
import { Button } from '@/components/ui/button';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { planMessage, type PlanStepInput } from '@/helper/planMessage/planMessage';
import { useSendMessage } from '@/hooks/useGenaixQueries';
import type { Plan, PlanItem } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

import styles from './PlanCard.module.css';

type Status = PlanItem['status'];

// The marker icon per status; pending and running show the item's number instead.
const ICONS: Partial<Record<Status, LucideIcon>> = { done: CheckIcon, skipped: MinusIcon, failed: XIcon };
const STATUSES = new Set<string>(['pending', 'running', 'done', 'skipped', 'failed']);

// A status outside the contract's enum shows as pending (client rule: tolerate unknown enum values).
function statusOf(item: PlanItem): Status {
    return STATUSES.has(item.status) ? item.status : 'pending';
}

/** A step while the user adjusts the plan; `key` keeps a row's field while rows move. */
interface Row extends PlanStepInput {
    key: string;
}

function rowsOf(items: PlanItem[]): Row[] {
    return items.map(({ id, label, target }) => ({ key: id, label, target }));
}

// Whether the steps differ from the agent's plan: wording, order, a target, a step added or removed.
function isChanged(steps: PlanStepInput[], items: PlanItem[]): boolean {
    const signature = (step: PlanStepInput) => `${step.label}\u0000${step.target ? JSON.stringify(step.target) : ''}`;

    return steps.map(signature).join('\u0001') !== items.map(signature).join('\u0001');
}

// The steps as they would be sent: trimmed, empty ones dropped.
function stepsOf(rows: Row[]): Row[] {
    return rows.map((row) => ({ ...row, label: row.label.trim() })).filter(({ label }) => label !== '');
}

interface PlanEditorProps {
    rows: Row[];
    onChange: (update: (rows: Row[]) => Row[]) => void;
    onDone: () => void;
    onCancel: () => void;
}

/**
 * Adjusting the plan right in the card (draft `editPlan`): each step a field with move and remove
 * buttons, "Add a step" below. Enter adds a step after the current one, Backspace in an empty field
 * removes it, Esc cancels, Ctrl/Cmd+Enter is Done.
 */
function PlanEditor({ rows, onChange: setRows, onDone, onCancel }: PlanEditorProps) {
    const { t } = useTranslation();
    // The row whose field gets the focus after the next render: the first at the start, then the one
    // added, moved or before the one removed.
    const focusKey = useRef<string | undefined>(rows[0]?.key);
    const fields = useRef(new Map<string, HTMLInputElement>());

    useEffect(() => {
        if (focusKey.current) {
            fields.current.get(focusKey.current)?.focus();
            focusKey.current = undefined;
        }
    });

    function update(index: number, label: string) {
        setRows((current) => current.map((row, i) => (i === index ? { ...row, label } : row)));
    }

    function add(after: number) {
        const row = { key: crypto.randomUUID(), label: '' };

        setRows((current) => [...current.slice(0, after + 1), row, ...current.slice(after + 1)]);
        focusKey.current = row.key;
    }

    function remove(index: number) {
        setRows((current) => current.filter((_, i) => i !== index));
        focusKey.current = rows[index - 1]?.key ?? rows[index + 1]?.key;
    }

    function move(index: number, by: -1 | 1) {
        setRows((current) => {
            const next = [...current];
            const [row] = next.splice(index, 1);

            next.splice(index + by, 0, row!);

            return next;
        });
        focusKey.current = rows[index]?.key;
    }

    function handleKeyDown(event: KeyboardEvent<HTMLInputElement>, index: number) {
        if (event.key === 'Escape') {
            event.preventDefault();
            onCancel();
        } else if (event.key === 'Enter' && (event.metaKey || event.ctrlKey)) {
            event.preventDefault();
            onDone();
        } else if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            add(index);
        } else if (event.key === 'Backspace' && rows[index]?.label === '' && rows.length > 1) {
            event.preventDefault();
            remove(index);
        }
    }

    return (
        <>
            <ol className={styles.items}>
                {rows.map((row, index) => (
                    <li key={row.key} className={`${styles.item} ${styles.editing}`}>
                        <span className={styles.marker} aria-hidden>{index + 1}</span>
                        <span className={styles.text}>
                            <input
                                ref={(element) => {
                                    if (element) {
                                        fields.current.set(row.key, element);
                                    } else {
                                        fields.current.delete(row.key);
                                    }
                                }}
                                className={styles.input}
                                aria-label={t('parts.plan.step', { number: index + 1 })}
                                placeholder={t('parts.plan.placeholder')}
                                value={row.label}
                                maxLength={500}
                                onChange={(event) => update(index, event.target.value)}
                                onKeyDown={(event) => handleKeyDown(event, index)}
                            />
                            {row.target && <span><RefToken value={row.target} /></span>}
                        </span>
                        <span className={styles.rowActions}>
                            <Button variant="ghost" size="icon" aria-label={t('parts.plan.moveUp')} disabled={index === 0} onClick={() => move(index, -1)}>
                                <ArrowUpIcon size={14} />
                            </Button>
                            <Button variant="ghost" size="icon" aria-label={t('parts.plan.moveDown')} disabled={index === rows.length - 1} onClick={() => move(index, 1)}>
                                <ArrowDownIcon size={14} />
                            </Button>
                            <Button variant="ghost" size="icon" aria-label={t('parts.plan.remove')} disabled={rows.length === 1} onClick={() => remove(index)}>
                                <XIcon size={14} />
                            </Button>
                        </span>
                    </li>
                ))}
            </ol>
            <div className={styles.add}>
                <Button variant="ghost" size="sm" onClick={() => add(rows.length - 1)}>
                    <PlusIcon size={14} />
                    {t('parts.plan.add')}
                </Button>
            </div>
        </>
    );
}

interface PlanCardProps {
    plan: Plan;
    sessionId: string;
    /** No run works on the session: the plan can be adjusted and accepted. */
    canAnswer: boolean;
}

/**
 * The agent's work plan (`plan.updated`) as a chat card (design.md §12 "Chat-Karte", "Plan-Schritte";
 * draft `partHTML` plan, `editPlan`): the summary, then each item with its number or state, its target
 * and its status word. While nothing of it has started and no run works, Adjust edits the steps in the
 * card and Accept plan sends the plan to the agent (`planMessage`): as proposed, or as adjusted. A later
 * version says why it changed (`reason`).
 */
export function PlanCard({ plan, sessionId, canAnswer }: PlanCardProps) {
    const { t } = useTranslation();
    const sendMessage = useSendMessage(sessionId);
    const items = plan.items ?? [];
    const [adjusted, setAdjusted] = useState<Row[]>();
    // The steps while the user adjusts them; none while the plan is only shown.
    const [draft, setDraft] = useState<Row[]>();
    const [isAccepted, setIsAccepted] = useState(false);
    const isPending = items.every((item) => statusOf(item) === 'pending');
    const canAct = isPending && canAnswer && !isAccepted;

    const isEditing = Boolean(draft) && canAct;
    const steps = stepsOf(draft ?? []);

    function handleDone() {
        if (steps.length > 0) {
            setAdjusted(isChanged(steps, items) ? steps : undefined);
            setDraft(undefined);
        }
    }

    function accept() {
        setIsAccepted(true);
        sendMessage.mutateAsync(planMessage(adjusted, { acceptText: t('parts.plan.acceptText'), intro: t('parts.plan.adjustedIntro') })).catch((error: unknown) => {
            setIsAccepted(false);
            useErrorNotificationStore.getState().addError({ messageKey: 'chat.sendFailed', detailKey: errorMessageKey(error) });
        });
    }

    // A version with a `reason` is the agent's answer to the user, no longer its proposal.
    const title = adjusted ? t('parts.plan.adjusted') : t(isPending && !plan.reason ? 'parts.plan.proposed' : 'parts.plan.title');
    let footer;

    if (isEditing) {
        footer = (
            <>
                <span className={styles.caption}>{t('parts.plan.keys')}</span>
                <Button variant="secondary" size="sm" onClick={() => setDraft(undefined)}>{t('parts.plan.cancel')}</Button>
                <Button variant="primary" size="sm" disabled={steps.length === 0} onClick={handleDone}>
                    <CheckIcon size={14} />
                    {t('parts.plan.done')}
                </Button>
            </>
        );
    } else if (canAct) {
        footer = (
            <>
                <span className={styles.caption}>{adjusted && t('parts.plan.adjustedByYou')}</span>
                <Button variant="secondary" size="sm" onClick={() => setDraft(adjusted ?? rowsOf(items))}>
                    <PencilIcon size={14} />
                    {t('parts.plan.adjust')}
                </Button>
                <Button variant="primary" size="sm" onClick={accept}>
                    <CheckCheckIcon size={14} />
                    {t('parts.plan.accept')}
                </Button>
            </>
        );
    } else if (plan.reason && !adjusted) {
        footer = <span className={styles.caption}>{plan.reason}</span>;
    }

    // One card for showing and adjusting, so switching does not play its entrance again.
    return (
        <div className={styles.plan}>
            <PartCard icon={RouteIcon} title={title} footer={footer}>
                {plan.summary && <p className={styles.summary}>{plan.summary}</p>}
                {isEditing && draft
                    ? <PlanEditor rows={draft} onChange={(update) => setDraft((current) => current && update(current))} onDone={handleDone} onCancel={() => setDraft(undefined)} />
                    : (
                        <ol className={styles.items}>
                            {(adjusted ?? items).map((step, index) => {
                                const status = 'status' in step ? statusOf(step) : 'pending';
                                const Icon = ICONS[status];

                                return (
                                    <li key={'key' in step ? step.key : step.id} className={`${styles.item} ${styles[status]}`}>
                                        <span className={styles.marker} aria-hidden>
                                            {Icon ? <Icon size={11} strokeWidth={2.4} /> : index + 1}
                                        </span>
                                        <span className={styles.text}>
                                            <span className={styles.label}>{step.label}</span>
                                            {step.target && <span><RefToken value={step.target} /></span>}
                                            {'detail' in step && step.detail && <span className={styles.detail}>{step.detail}</span>}
                                        </span>
                                        <span className={styles.status}>{t(`chat.steps.status.${status}`)}</span>
                                    </li>
                                );
                            })}
                        </ol>
                    )}
            </PartCard>
        </div>
    );
}
