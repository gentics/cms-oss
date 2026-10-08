import { CircleCheckIcon, CircleQuestionMarkIcon, SlidersHorizontalIcon } from 'lucide-react';
import { type FormEvent, type ReactNode, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { PartCard } from '@/components/MessageParts/PartCard';
import { SelectableList } from '@/components/MessageParts/SelectableList';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { useAnswerInteraction } from '@/hooks/useGenaixQueries';
import type { Interaction, InteractionAnswer, UserSettingPart } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { useInteractionDraftStore } from '@/store/useInteractionDraftStore';

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

interface Option {
    value: string;
    label: string;
}

/** A layer `Select` over `options`, labelled by `id`'s `<label>`. */
function OptionSelect({ id, options, value, onChange, disabled }: { id: string; options: Option[]; value: string; onChange: (value: string) => void; disabled: boolean }) {
    const { t } = useTranslation();
    const items = Object.fromEntries(options.map((option) => [option.value, option.label]));

    return (
        <Select items={items} value={value || null} onValueChange={(next) => onChange(next === null ? '' : String(next))} disabled={disabled}>
            <SelectTrigger id={id}>
                <SelectValue placeholder={t('chat.interaction.choose')} />
            </SelectTrigger>
            <SelectContent>
                {options.map((option) => <SelectItem key={option.value} value={option.value}>{option.label}</SelectItem>)}
            </SelectContent>
        </Select>
    );
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

// `choice`: one option, several when `multi` (contract `ChoiceAnswer`, at least one id).
function Choice({ interaction, onSubmit, disabled }: KindProps) {
    const { t } = useTranslation();
    const [selected, setSelected] = useState<string[]>([]);

    useDraft(interaction.id, useMemo(() => (selected.length > 0 ? { selected } : undefined), [selected]));

    return (
        <div className={styles.fields}>
            <SelectableList
                items={interaction.options ?? []}
                multi={interaction.multi ?? false}
                value={selected}
                onChange={setSelected}
                label={interaction.prompt}
            />
            <div className={styles.actions}>
                <Button variant="primary" size="sm" disabled={disabled || selected.length === 0} onClick={() => onSubmit({ selected })}>
                    {t('chat.interaction.send')}
                </Button>
            </div>
        </div>
    );
}

// `confirm`: yes or no, with an optional comment (contract `ConfirmAnswer`).
function Confirm({ interaction, onSubmit, disabled }: KindProps) {
    const { t } = useTranslation();
    const [comment, setComment] = useState('');
    const id = `interaction-${interaction.id}-comment`;

    function answer(approved: boolean) {
        onSubmit(comment.trim() ? { approved, comment: comment.trim() } : { approved });
    }

    return (
        <div className={styles.fields}>
            <label className={styles.field} htmlFor={id}>
                <span className={styles.fieldLabel}>{t('chat.interaction.comment')}</span>
                <input id={id} className={styles.input} value={comment} maxLength={4000} onChange={(event) => setComment(event.target.value)} disabled={disabled} />
            </label>
            <div className={styles.actions}>
                <Button variant="secondary" size="sm" disabled={disabled} onClick={() => answer(false)}>{t('chat.interaction.reject')}</Button>
                <Button variant="primary" size="sm" disabled={disabled} onClick={() => answer(true)}>{t('chat.interaction.approve')}</Button>
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

interface SettingRow {
    key: string;
    /** The setting's own name, for a `missing` one. */
    name?: string;
    options?: Option[];
    source?: string;
    evidence?: string;
    /** The proposed value and its wording, prefilled. */
    proposed?: Option;
}

// `settings_review` (contract §9.1): every `proposed` setting prefilled and every `missing` one to
// fill, answered with one `setting` part per key.
function SettingsReview({ interaction, onSubmit, disabled }: KindProps) {
    const { t } = useTranslation();
    const rows = useMemo((): SettingRow[] => [
        ...(interaction.proposed ?? []).map((item) => ({
            key: item.key,
            options: item.options,
            source: item.source,
            evidence: item.evidence,
            proposed: { value: item.value, label: item.label },
        })),
        ...(interaction.missing ?? []).map((item) => ({ key: item.key, name: item.label, options: item.options })),
    ], [interaction]);
    // The chosen value per key, and for free text what was typed (value and wording at once).
    const [values, setValues] = useState<Record<string, Option>>(() => Object.fromEntries(
        rows.flatMap((row) => (row.proposed ? [[row.key, row.proposed]] : [])),
    ));

    const isComplete = rows.every((row) => (values[row.key]?.value ?? '') !== '');
    const answer = useMemo(() => ({
        settings: rows.map((row): UserSettingPart => ({ type: 'setting', key: row.key, value: values[row.key]?.value ?? '', label: values[row.key]?.label ?? '' })),
    }), [rows, values]);

    useDraft(interaction.id, isComplete ? answer : undefined);

    function choose(row: SettingRow, value: string) {
        const option = row.options?.find((item) => item.value === value);

        setValues((current) => ({ ...current, [row.key]: { value, label: option?.label ?? value } }));
    }

    function handleSubmit(event: FormEvent) {
        event.preventDefault();
        onSubmit(answer);
    }

    return (
        <form onSubmit={handleSubmit} className={styles.fields}>
            {rows.map((row) => {
                const id = `interaction-${interaction.id}-${row.key}`;
                const value = values[row.key];

                return (
                    <div key={row.key} className={styles.field}>
                        <label className={styles.fieldLabel} htmlFor={id}>
                            {row.name || t(`chat.interaction.setting.${row.key}`, { defaultValue: row.key })}
                            {!row.proposed && <span className={styles.required} aria-hidden>*</span>}
                        </label>
                        {row.options?.length
                            ? <OptionSelect id={id} options={row.options} value={value?.value ?? ''} onChange={(next) => choose(row, next)} disabled={disabled} />
                            : (
                                <input
                                    id={id}
                                    className={styles.input}
                                    value={value?.label ?? ''}
                                    onChange={(event) => setValues((current) => ({ ...current, [row.key]: { value: event.target.value, label: event.target.value } }))}
                                    disabled={disabled}
                                />
                            )}
                        {row.source && (
                            <span className={styles.hint}>
                                {t(`chat.interaction.source.${row.source}`)}
                                {row.evidence && ` · „${row.evidence}“`}
                            </span>
                        )}
                    </div>
                );
            })}
            <div className={styles.actions}>
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
 * stream, whose `interaction.resolved` removes the card. A refused answer (`409` already answered,
 * `410` expired) is shown as an error notification.
 */
export function InteractionCard({ interaction, sessionId }: { interaction: Interaction; sessionId: string }) {
    const { t } = useTranslation();
    const answer = useAnswerInteraction(sessionId);
    const Kind = KINDS[interaction.kind];

    useEffect(() => () => useInteractionDraftStore.getState().clearDraft(interaction.id), [interaction.id]);

    function submit(value: InteractionAnswer) {
        answer.mutate({ interactionId: interaction.id, answer: value }, {
            onError: (error) => {
                useErrorNotificationStore.getState().addError({ messageKey: 'chat.interaction.answerFailed', detailKey: errorMessageKey(error) });
            },
        });
    }

    if (!Kind) {
        return null;
    }

    return (
        <div className={styles.card}>
            <PartCard
                icon={interaction.kind === 'settings_review' ? SlidersHorizontalIcon : CircleQuestionMarkIcon}
                title={t(`chat.interaction.title.${interaction.kind}`)}
            >
                <p className={styles.prompt}>{interaction.prompt}</p>
                {/* Stays disabled after an accepted answer until `interaction.resolved` removes the card. */}
                <Kind interaction={interaction} onSubmit={submit} disabled={answer.isPending || answer.isSuccess} />
            </PartCard>
        </div>
    );
}
