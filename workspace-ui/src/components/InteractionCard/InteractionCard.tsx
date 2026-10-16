import { CheckIcon, CircleCheckIcon, CircleQuestionMarkIcon, MessageSquareIcon, SlidersHorizontalIcon, SparklesIcon, TimerOffIcon, XIcon } from 'lucide-react';
import { type FormEvent, type ReactNode, useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { PartCard } from '@/components/MessageParts/PartCard';
import { SelectableList } from '@/components/MessageParts/SelectableList';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { SelectChevron } from '@/components/ui/select';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { useAnswerInteraction } from '@/hooks/useGenaixQueries';
import { lockAfterFailure, useInteractionLock } from '@/hooks/useInteractionLock';
import type { Interaction, InteractionAnswer, UserSettingPart } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { useInteractionDraftStore } from '@/store/useInteractionDraftStore';

import { interactionCardId } from './focusInteractionCard';
import { type Option, OptionSelect } from './OptionSelect';
import { SettingField, type SettingRow } from './SettingField';
import { withPath } from './withPath';

import styles from './InteractionCard.module.css';

type Submit = (answer: InteractionAnswer) => void;

// Shares the answer filled in so far (none while incomplete), so the composer can send it as
// `reply_to_interaction`. `answer` must keep its identity while the fields do not change.
function useDraft(interactionId: string, answer: InteractionAnswer | undefined) {
    useEffect(() => {
        useInteractionDraftStore.getState().setDraft(interactionId, answer);
    }, [interactionId, answer]);
}

interface KindProps {
    interaction: Interaction;
    onSubmit: Submit;
    disabled: boolean;
}

// `ask_user`: free text, at most 4000 characters (contract `TextAnswer`).
function AskUser({ interaction, onSubmit, disabled }: KindProps) {
    const { t } = useTranslation();
    const [text, setText] = useState('');
    const id = `interaction-${interaction.id}-text`;

    useDraft(interaction.id, useMemo(() => (text.trim() ? { text: text.trim() } : undefined), [text]));

    return (
        <form onSubmit={(event) => { event.preventDefault(); onSubmit({ text: text.trim() }); }} className={styles.fields}>
            <label className={styles.field} htmlFor={id}>
                <span className={styles.fieldLabel}>{t('chat.interaction.answer')}</span>
                <textarea id={id} className={styles.input} value={text} maxLength={4000} onChange={(event) => setText(event.target.value)} disabled={disabled} />
            </label>
            <div className={styles.actions}>
                <Button type="submit" variant="primary" size="sm" disabled={disabled || text.trim() === ''}>{t('chat.interaction.send')}</Button>
            </div>
        </form>
    );
}

// The option ids GenAIx recommends that the choice offers, one unless `multi` (rc.5 addition).
function recommendedOf(interaction: Interaction): string[] {
    const offered = new Set((interaction.options ?? []).map(({ id }) => id));
    const recommended = (interaction.recommended ?? []).filter((id) => offered.has(id));

    return interaction.multi ? recommended : recommended.slice(0, 1);
}

// `choice`: one option, several when `multi` (contract `ChoiceAnswer`). Like the settings review (rc.5
// addition): what GenAIx recommends first, preselected, with a "Recommended" badge, and every option with
// why it is offered (`reason`). With `other_allowed`, "Something else" takes the user's own text, sent as
// `other`: instead of the option in a single choice (`selected` then empty), besides those chosen in a
// multiple one.
function Choice({ interaction, onSubmit, disabled }: KindProps) {
    const { t } = useTranslation();
    const recommended = useMemo(() => recommendedOf(interaction), [interaction]);
    const items = useMemo(() => {
        const options = (interaction.options ?? []).map((option) => ({
            ...option,
            ...(recommended.includes(option.id) && { badge: t('chat.interaction.recommended') }),
        }));

        return [...options.filter(({ id }) => recommended.includes(id)), ...options.filter(({ id }) => !recommended.includes(id))];
    }, [interaction, recommended, t]);
    const [selected, setSelected] = useState<string[]>(recommended);
    const [isOther, setIsOther] = useState(false);
    const [otherText, setOtherText] = useState('');
    const isMulti = interaction.multi ?? false;
    const other = interaction.other_allowed && isOther ? otherText.trim() : '';
    const answer = useMemo(() => {
        if (other) {
            return { selected, other };
        }

        return selected.length > 0 ? { selected } : undefined;
    }, [selected, other]);

    useDraft(interaction.id, answer);

    function selectOther(isSelected: boolean) {
        setIsOther(isSelected);

        if (isSelected && !isMulti) {
            setSelected([]);
        }
    }

    return (
        <div className={styles.fields}>
            <SelectableList
                items={items}
                multi={isMulti}
                value={selected}
                onChange={setSelected}
                label={interaction.prompt}
                other={interaction.other_allowed
                    ? {
                        label: t('chat.interaction.other'),
                        fieldLabel: t('chat.interaction.otherAnswer'),
                        placeholder: t('chat.interaction.otherPlaceholder'),
                        isSelected: isOther,
                        text: otherText,
                        onSelect: selectOther,
                        onTextChange: setOtherText,
                        disabled,
                    }
                    : undefined}
            />
            <div className={styles.actions}>
                <Button variant="primary" size="sm" disabled={disabled || !answer} onClick={() => answer && onSubmit(answer)}>
                    {t('chat.interaction.send')}
                </Button>
            </div>
        </div>
    );
}

// `confirm`: yes or no (contract `ConfirmAnswer`), as two buttons in their full size on the right, yes
// last, as the main action of a card's foot (design.md §12 "Chat-Karte"). The optional
// comment is asked for only when the user wants to add one ("Add a comment"), which opens its field
// above them; yes or no then send it along.
function Confirm({ interaction, onSubmit, disabled }: KindProps) {
    const { t } = useTranslation();
    const [comment, setComment] = useState('');
    const [isCommenting, setIsCommenting] = useState(false);
    const commentRef = useRef<HTMLTextAreaElement>(null);
    const id = `interaction-${interaction.id}-comment`;

    // The opened field takes the focus, for the user to write right away.
    useEffect(() => {
        if (isCommenting) {
            commentRef.current?.focus();
        }
    }, [isCommenting]);

    function answer(approved: boolean) {
        onSubmit(comment.trim() ? { approved, comment: comment.trim() } : { approved });
    }

    return (
        <div className={styles.fields}>
            {isCommenting && (
                <label className={styles.field} htmlFor={id}>
                    <span className={styles.fieldLabel}>{t('chat.interaction.comment')}</span>
                    <textarea ref={commentRef} id={id} className={styles.input} value={comment} maxLength={4000} onChange={(event) => setComment(event.target.value)} disabled={disabled} />
                </label>
            )}
            <div className={styles.decision}>
                {!isCommenting && (
                    <Button variant="quiet" size="text" disabled={disabled} onClick={() => setIsCommenting(true)}>
                        <MessageSquareIcon size={12} />
                        {t('chat.interaction.addComment')}
                    </Button>
                )}
                <Button variant="secondary" disabled={disabled} onClick={() => answer(false)}>
                    <XIcon size={16} />
                    {t('chat.interaction.reject')}
                </Button>
                <Button variant="primary" disabled={disabled} onClick={() => answer(true)}>
                    <CheckIcon size={16} />
                    {t('chat.interaction.approve')}
                </Button>
            </div>
        </div>
    );
}

/** One property of a flat `form` schema (contract `Interaction.schema`: one property, one field). */
interface SchemaProperty {
    type?: string;
    title?: string;
    description?: string;
    enum?: unknown[];
}

function schemaOf(interaction: Interaction): { properties: [string, SchemaProperty][]; required: string[] } {
    const schema = interaction.schema ?? {};
    const properties = typeof schema.properties === 'object' && schema.properties !== null ? schema.properties as Record<string, SchemaProperty> : {};
    const required = Array.isArray(schema.required) ? schema.required.filter((key): key is string => typeof key === 'string') : [];

    return { properties: Object.entries(properties), required };
}

// `form`: one field per property of the flat schema (contract `FormAnswer`); GenAIx validates the values.
function Form({ interaction, onSubmit, disabled }: KindProps) {
    const { t } = useTranslation();
    const { properties, required } = useMemo(() => schemaOf(interaction), [interaction]);
    const [values, setValues] = useState<Record<string, string | boolean>>({});

    function set(key: string, value: string | boolean) {
        setValues((current) => ({ ...current, [key]: value }));
    }

    const isComplete = required.every((key) => {
        const value = values[key];

        return value !== undefined && value !== '';
    });
    const answer = useMemo(() => ({
        values: Object.fromEntries(properties.flatMap(([key, property]) => {
            const value = values[key];

            if (value === undefined || value === '') {
                return [];
            }

            const isNumber = property.type === 'number' || property.type === 'integer';

            return [[key, isNumber && typeof value === 'string' ? Number(value) : value]];
        })),
    }), [properties, values]);

    useDraft(interaction.id, isComplete ? answer : undefined);

    function handleSubmit(event: FormEvent) {
        event.preventDefault();
        onSubmit(answer);
    }

    return (
        <form onSubmit={handleSubmit} className={styles.fields}>
            {properties.map(([key, property]) => {
                const id = `interaction-${interaction.id}-${key}`;
                const label = property.title || key;
                const isRequired = required.includes(key);
                const mark = isRequired ? <span className={styles.required} aria-hidden>*</span> : null;

                if (property.type === 'boolean') {
                    return (
                        <label key={key} className={styles.checkboxField}>
                            <Checkbox checked={values[key] === true} onCheckedChange={(checked) => set(key, checked)} disabled={disabled} />
                            <span className={styles.fieldLabel}>{label}{mark}</span>
                        </label>
                    );
                }

                const value = typeof values[key] === 'string' ? values[key] : '';

                return (
                    <div key={key} className={styles.field}>
                        <label className={styles.fieldLabel} htmlFor={id}>{label}{mark}</label>
                        {property.enum
                            ? (
                                <OptionSelect
                                    id={id}
                                    options={property.enum.map((option) => ({ value: String(option), label: String(option) }))}
                                    value={value}
                                    onChange={(next) => set(key, next)}
                                    disabled={disabled}
                                />
                            )
                            : (
                                <input
                                    id={id}
                                    className={styles.input}
                                    type={property.type === 'number' || property.type === 'integer' ? 'number' : 'text'}
                                    value={value}
                                    required={isRequired}
                                    onChange={(event) => set(key, event.target.value)}
                                    disabled={disabled}
                                />
                            )}
                        {property.description && <span className={styles.hint}>{property.description}</span>}
                    </div>
                );
            })}
            <div className={styles.actions}>
                <Button type="submit" variant="primary" size="sm" disabled={disabled || !isComplete}>{t('chat.interaction.send')}</Button>
            </div>
        </form>
    );
}

// The keys chosen from a list or a picker; any other key without options is typed (`SettingField`).
const CHOSEN_KEYS: ReadonlySet<string> = new Set(['node', 'folder', 'file_folder', 'image_folder', 'template', 'language', 'publish_at']);

// `settings_review` (contract §9.1): every `proposed` setting prefilled, every `missing` one to fill or
// preselected with what GenAIx recommends, answered with one `setting` part per key. Always the same list,
// one setting per row: its name; its value in the look of a field with a chevron (`Button` `input`), which
// a click opens for changing in its own field (`SettingField`, opened at once) until the user chose or
// left it; and its state. A proposed value says where it came from under the field, a recommended one has
// "Recommended" in the field (its reason the tooltip), and one still to choose shows "Choose…", a
// `--warning` border and "Selection required". Confirm is enabled once every setting has a value; until
// then the foot says which are still to choose. A different folder keeps the chosen template; its field
// then offers the new folder's templates beside it.
function SettingsReview({ interaction, onSubmit, disabled }: KindProps) {
    const { t } = useTranslation();
    const rows = useMemo((): SettingRow[] => [
        ...(interaction.proposed ?? []).map((item) => ({
            key: item.key,
            options: item.options,
            source: item.source,
            evidence: item.evidence,
            proposed: { value: item.value, label: item.label, ref: item.ref },
            canChooseOther: true,
        })),
        ...(interaction.missing ?? []).map((item) => ({
            key: item.key,
            name: item.label,
            options: item.options,
            recommended: item.options?.find((option) => option.value === item.recommended),
            reason: item.reason,
            canChooseOther: item.other_allowed ?? false,
        })),
    ], [interaction]);
    // The chosen value per key: the CMS id or code with its wording, and for a folder the folder (`ref`).
    const [values, setValues] = useState<Record<string, Option>>(() => Object.fromEntries(
        rows.flatMap((row) => {
            const initial = row.proposed ?? row.recommended;

            return initial ? [[row.key, { value: initial.value, label: initial.label, ref: initial.ref }]] : [];
        }),
    ));
    // The setting the user opened for changing, if any.
    const [editing, setEditing] = useState<string>();

    const nameOf = (row: SettingRow) => row.name || t(`chat.interaction.setting.${row.key}`, { defaultValue: row.key });
    const toChoose = rows.filter((row) => (values[row.key]?.value ?? '') === '');
    const isComplete = toChoose.length === 0;
    const answer = useMemo(() => ({
        settings: rows.map((row): UserSettingPart => ({ type: 'setting', key: row.key, value: values[row.key]?.value ?? '', label: values[row.key]?.label ?? '' })),
    }), [rows, values]);

    useDraft(interaction.id, isComplete ? answer : undefined);

    function choose(row: SettingRow, option: Option) {
        setValues((current) => ({ ...current, [row.key]: option }));
    }

    // One setting: its name, its value as a field (or its own field while it is changed), its state.
    function settingRow(row: SettingRow) {
        const id = `interaction-${interaction.id}-${row.key}`;
        const value = values[row.key];
        const isEditing = editing === row.key;
        const isMissing = !value?.value;
        // Where the proposed value came from, while it is still that value.
        const source = row.source && value?.value === row.proposed?.value
            ? `${t(`chat.interaction.source.${row.source}`)}${row.evidence ? ` · „${row.evidence}“` : ''}`
            : undefined;
        const isRecommended = row.recommended !== undefined && value?.value === row.recommended.value;
        const placeholder = t(CHOSEN_KEYS.has(row.key) || row.options?.length ? 'chat.interaction.choose' : 'chat.interaction.enterValue');

        return (
            <div key={row.key} className={styles.setting}>
                {isEditing
                    ? <label className={styles.settingName} htmlFor={id}>{nameOf(row)}</label>
                    : <span id={`${id}-name`} className={styles.settingName}>{nameOf(row)}</span>}
                {isEditing
                    ? (
                        <SettingField
                            id={id}
                            row={row}
                            value={value}
                            onChange={(option) => choose(row, option)}
                            disabled={disabled}
                            folder={values.folder}
                            opensAtOnce
                            onDone={() => setEditing(undefined)}
                            onEdit={() => setEditing(row.key)}
                        />
                    )
                    : (
                        <Button
                            id={id}
                            variant="input"
                            size="field"
                            data-attention={isMissing ? '' : undefined}
                            aria-labelledby={`${id}-name ${id}-value`}
                            aria-describedby={isRecommended || isMissing ? `${id}-state` : undefined}
                            disabled={disabled}
                            onClick={() => setEditing(row.key)}
                        >
                            <span id={`${id}-value`} className={isMissing ? styles.valuePlaceholder : styles.valueText}>
                                {isMissing ? placeholder : withPath(value.label || value.value, value.ref)}
                            </span>
                            {isRecommended && (
                                <span id={`${id}-state`} className={styles.recommended} title={row.reason}>
                                    <SparklesIcon size={11} aria-hidden />
                                    {t('chat.interaction.recommended')}
                                </span>
                            )}
                            {isMissing && <span id={`${id}-state`} className={styles.requiredBadge}>{t('chat.interaction.required')}</span>}
                            <span className={styles.chevron}><SelectChevron /></span>
                        </Button>
                    )}
                {source && (
                    <span className={styles.source}>
                        <SparklesIcon size={12} aria-hidden />
                        {source}
                    </span>
                )}
            </div>
        );
    }

    function handleSubmit(event: FormEvent) {
        event.preventDefault();
        onSubmit(answer);
    }

    return (
        <form onSubmit={handleSubmit} className={styles.fields}>
            <div className={styles.settings}>{rows.map(settingRow)}</div>
            <div className={styles.foot}>
                <span className={styles.footCaption}>
                    {!isComplete && t('chat.interaction.stillToChoose', { names: toChoose.map(nameOf).join(', ') })}
                </span>
                <Button type="submit" variant="primary" size="sm" disabled={disabled || !isComplete}>{t('chat.interaction.confirm')}</Button>
            </div>
        </form>
    );
}

/**
 * A `settings_review` the user confirmed, from the user message GenAIx stored for it
 * (`Message.interaction_id`): the review's card, read-only, with the confirmed settings and the status
 * as icon and word (design.md §3.3). That message holds only the `setting` parts, not the question.
 */
export function ConfirmedSettingsCard({ settings }: { settings: UserSettingPart[] }) {
    const { t } = useTranslation();

    return (
        <div className={styles.card}>
            <PartCard
                icon={SlidersHorizontalIcon}
                title={t('chat.interaction.title.settings_review')}
                footer={(
                    <span className={styles.confirmed}>
                        <CircleCheckIcon size={14} aria-hidden />
                        {t('chat.interaction.confirmed')}
                    </span>
                )}
            >
                <dl className={styles.fields}>
                    {settings.map((setting) => (
                        <div key={setting.key} className={styles.field}>
                            <dt className={styles.fieldLabel}>{t(`chat.interaction.setting.${setting.key}`, { defaultValue: setting.key })}</dt>
                            <dd className={styles.settingValue}>{setting.label || setting.value}</dd>
                        </div>
                    ))}
                </dl>
            </PartCard>
        </div>
    );
}

// Why the card is locked, as icon and word (design.md §3.3).
function LockStatus({ lock }: { lock: 'answered' | 'expired' }) {
    const { t } = useTranslation();

    if (lock === 'expired') {
        return (
            <span className={styles.expired}>
                <TimerOffIcon size={14} aria-hidden />
                {t('chat.interaction.expired')}
            </span>
        );
    }

    return (
        <span className={styles.confirmed}>
            <CircleCheckIcon size={14} aria-hidden />
            {t('chat.interaction.answered')}
        </span>
    );
}

const KINDS: Record<Interaction['kind'], (props: KindProps) => ReactNode> = {
    ask_user: AskUser,
    choice: Choice,
    confirm: Confirm,
    form: Form,
    settings_review: SettingsReview,
};

/**
 * A pending interaction (contract §9): the agent's question as a chat card, with the controls its
 * `kind` needs, answered through `POST …/interactions/{interaction_id}`, or by a message from the
 * composer that carries what the card holds as `reply_to_interaction` (`useInteractionDraftStore`). The run continues on the
 * stream, whose `interaction.resolved` removes the card.
 *
 * It is answered once: the controls lock while an answer from the card or the composer is on its way,
 * and stay locked after it was accepted (`Answered`) or once `expires_at` has passed (`Expired`), until
 * `interaction.resolved` comes (`useInteractionLock`). A refused answer is shown as an error
 * notification; `409` already answered and `410` expired keep the card locked, any other failure
 * unlocks it.
 */
export function InteractionCard({ interaction, sessionId }: { interaction: Interaction; sessionId: string }) {
    const { t } = useTranslation();
    const answer = useAnswerInteraction(sessionId);
    const lock = useInteractionLock(interaction);
    const Kind = KINDS[interaction.kind];

    useEffect(() => () => useInteractionDraftStore.getState().clearDraft(interaction.id), [interaction.id]);

    function submit(value: InteractionAnswer) {
        const drafts = useInteractionDraftStore.getState();

        // The store, not `lock`: a second click can come before the card re-renders.
        if (lock || drafts.locks[interaction.id]) {
            return;
        }

        drafts.lock(interaction.id, 'sending');
        answer.mutate({ interactionId: interaction.id, answer: value }, {
            onSuccess: () => useInteractionDraftStore.getState().lock(interaction.id, 'answered'),
            onError: (error) => {
                lockAfterFailure(interaction.id, error);
                useErrorNotificationStore.getState().addError({ messageKey: 'chat.interaction.answerFailed', detailKey: errorMessageKey(error) });
            },
        });
    }

    if (!Kind) {
        return null;
    }

    // Focusable from script only, for "Answer" on the plan (`focusInteractionCard`).
    return (
        <div id={interactionCardId(interaction.id)} className={styles.card} tabIndex={-1}>
            <PartCard
                icon={interaction.kind === 'settings_review' ? SlidersHorizontalIcon : CircleQuestionMarkIcon}
                title={t(`chat.interaction.title.${interaction.kind}`)}
                footer={lock === 'answered' || lock === 'expired' ? <LockStatus lock={lock} /> : undefined}
            >
                {/* A settings review is its fields alone; its prompt is not shown. */}
                {interaction.kind !== 'settings_review' && <p className={styles.prompt}>{interaction.prompt}</p>}
                <Kind interaction={interaction} onSubmit={submit} disabled={Boolean(lock)} />
            </PartCard>
        </div>
    );
}
