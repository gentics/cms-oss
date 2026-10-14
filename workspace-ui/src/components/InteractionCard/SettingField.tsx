import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { FolderSearchDialog } from '@/components/FolderSearchDialog/FolderSearchDialog';
import { Button } from '@/components/ui/button';
import { useCmsLanguages, useCmsNodes, useCmsTemplates } from '@/hooks/useCmsQueries';
import type { CmsFolder, CmsNode } from '@/services/cmsApiService/cmsApiService';

import { type Option, OptionSelect } from './OptionSelect';
import { withPath } from './withPath';

import styles from './InteractionCard.module.css';

/** One setting of a `settings_review`, as `SettingsReview` lists them. */
export interface SettingRow {
    key: string;
    /** The setting's own name, for a `missing` one. */
    name?: string;
    options?: Option[];
    source?: string;
    evidence?: string;
    /** The proposed value and its wording, prefilled; with the CMS object it names (`ref`). */
    proposed?: Option;
    /** For a `missing` one: the option GenAIx recommends, preselected. */
    recommended?: Option;
    /** For a `missing` one: why GenAIx recommends it. */
    reason?: string;
    /**
     * Whether a value off `options` may be given: always for a `proposed` one, whose options are a
     * shortcut, for a `missing` one only with `other_allowed` (contract `Interaction.missing`).
     */
    canChooseOther: boolean;
}

interface SettingFieldProps {
    /** The id of the field, for the `<label>` of the review. */
    id: string;
    row: SettingRow;
    value: Option | undefined;
    onChange: (value: Option) => void;
    disabled: boolean;
    /** The folder chosen in the review right now: its templates, and the languages of its node. */
    folder?: Option;
}

// The options to offer: GenAIx's first (with their reasons), then the others, each value once; the
// current value always among them, so a prefilled one shows even before a list has loaded.
function mergeOptions(value: Option | undefined, ...lists: Option[][]): Option[] {
    return [...lists.flat(), ...(value ? [value] : [])]
        .filter((option, index, all) => all.findIndex(({ value: other }) => other === option.value) === index);
}

// The option the select chose, handed to `onChange`; nothing for an empty or unknown value (the select
// reports one when the options it showed change under it).
function choose(options: Option[], value: string, onChange: (option: Option) => void) {
    const option = options.find((candidate) => candidate.value === value);

    if (option) {
        onChange(option);
    }
}

// A folder of the folder search as an option: its id as the value, its name as the wording, and the
// folder with its node and path as `ref`.
function folderOption(node: CmsNode, folder: CmsFolder): Option {
    const ref = { type: 'folder' as const, id: String(folder.id), node_id: node.id, label: folder.name, ...(folder.path && { path: folder.path }) };

    return { value: ref.id, label: folder.name, ref };
}

// `folder`: never typed. With options a select, else the chosen folder shown read-only; "Choose folder…"
// opens the folder search wherever a folder off the options may be given.
function FolderField({ id, row, value, onChange, disabled }: SettingFieldProps) {
    const { t } = useTranslation();
    const [isPicking, setIsPicking] = useState(false);
    // Folders chosen in the folder search, offered beside GenAIx's options from then on.
    const [picked, setPicked] = useState<Option[]>([]);
    const options = mergeOptions(undefined, row.options ?? [], picked);
    const canPick = row.canChooseOther || options.length === 0;

    function pick(node: CmsNode, folder: CmsFolder) {
        const option = folderOption(node, folder);

        setPicked((current) => [...current.filter(({ value: other }) => other !== option.value), option]);
        setIsPicking(false);
        onChange(option);
    }

    return (
        <div className={styles.picker}>
            {options.length > 0
                ? <OptionSelect id={id} options={options} value={value?.value ?? ''} onChange={(next) => choose(options, next, onChange)} disabled={disabled} />
                : (
                    <input
                        id={id}
                        className={`${styles.input} ${styles.picked}`}
                        value={value ? withPath(value.label, value.ref) : ''}
                        placeholder={t('chat.interaction.noFolderChosen')}
                        readOnly
                        onClick={() => !disabled && setIsPicking(true)}
                        disabled={disabled}
                    />
                )}
            {canPick && (
                <Button type="button" variant="secondary" size="sm" onClick={() => setIsPicking(true)} disabled={disabled}>
                    {options.length > 0 ? t('chat.interaction.otherFolder') : t('chat.interaction.chooseFolder')}
                </Button>
            )}
            {isPicking && <FolderSearchDialog onPick={pick} onCancel={() => setIsPicking(false)} />}
        </div>
    );
}

// `template`: GenAIx's options, and the templates of the chosen folder besides them, also once another
// folder was chosen (the template chosen so far stays). Without either there is nothing to offer.
function TemplateField({ id, row, value, onChange, disabled, folder }: SettingFieldProps) {
    const { t } = useTranslation();
    const genaixOptions = row.options ?? [];
    const folderId = folder ? Number(folder.value) : Number.NaN;
    const nodeId = folder?.ref?.node_id;
    const needsCms = genaixOptions.length === 0 || row.canChooseOther;
    const templates = useCmsTemplates(needsCms && Number.isInteger(folderId) ? folderId : undefined, nodeId);
    const cmsOptions = (templates.data ?? []).map((template): Option => ({
        value: String(template.id),
        label: template.name,
        ref: { type: 'template', id: String(template.id), ...(nodeId !== undefined && { node_id: nodeId }) },
    }));
    const options = mergeOptions(value, genaixOptions, cmsOptions);
    let placeholder: string | undefined;

    if (options.length === 0) {
        placeholder = folder ? t('chat.interaction.loadingOptions') : t('chat.interaction.chooseFolderFirst');
    }

    return (
        <OptionSelect
            id={id}
            options={options}
            value={value?.value ?? ''}
            onChange={(next) => choose(options, next, onChange)}
            disabled={disabled || options.length === 0}
            placeholder={placeholder}
        />
    );
}

// `language`: the languages of the chosen folder's node, of every node while that is not known; GenAIx's
// options first. The value is the language code.
function LanguageField({ id, row, value, onChange, disabled, folder }: SettingFieldProps) {
    const nodes = useCmsNodes();
    const folderNodeId = folder?.ref?.node_id;
    const languages = useCmsLanguages(folderNodeId !== undefined ? [folderNodeId] : (nodes.data ?? []).map((node) => node.id));
    const options = mergeOptions(value, row.options ?? [], languages.languages.map((language) => ({ value: language.code, label: language.name })));

    return <OptionSelect id={id} options={options} value={value?.value ?? ''} onChange={(next) => choose(options, next, onChange)} disabled={disabled} />;
}

// `node`: the nodes the user can see. The value is the node id.
function NodeField({ id, row, value, onChange, disabled }: SettingFieldProps) {
    const nodes = useCmsNodes();
    const options = mergeOptions(value, row.options ?? [], (nodes.data ?? []).map((node) => ({ value: String(node.id), label: node.name })));

    return <OptionSelect id={id} options={options} value={value?.value ?? ''} onChange={(next) => choose(options, next, onChange)} disabled={disabled} />;
}

// `YYYY-MM-DDTHH:mm` in local time, as a `datetime-local` field holds it; '' for a value that is no date.
function toLocalInput(iso: string): string {
    const date = new Date(iso);

    if (Number.isNaN(date.getTime())) {
        return '';
    }

    const pad = (part: number) => String(part).padStart(2, '0');

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

// `publish_at`: the browser's date and time picker; the value is the moment as an ISO `date-time`.
function PublishAtField({ id, value, onChange, disabled }: SettingFieldProps) {
    const { i18n } = useTranslation();

    function change(local: string) {
        const date = new Date(local);

        if (local && !Number.isNaN(date.getTime())) {
            onChange({ value: date.toISOString(), label: date.toLocaleString(i18n.language) });
        }
    }

    return <input id={id} type="datetime-local" className={styles.input} value={toLocalInput(value?.value ?? '')} onChange={(event) => change(event.target.value)} disabled={disabled} />;
}

/**
 * The field of one `settings_review` setting. `node`, `folder`, `template` and `language` are chosen,
 * never typed: the value is the CMS id or the language code, which the agent needs (a name is `422`,
 * contract `UserSettingPart.value`). `publish_at` uses the date picker. Any other key, such as
 * `filename`, is a constraint in the user's words and stays free text; so does a key with GenAIx options
 * that none of these is, as a select over them.
 */
export function SettingField(props: SettingFieldProps) {
    const { id, row, value, onChange, disabled } = props;

    switch (row.key) {
        case 'folder':
            return <FolderField {...props} />;
        case 'template':
            return <TemplateField {...props} />;
        case 'language':
            return <LanguageField {...props} />;
        case 'node':
            return <NodeField {...props} />;
        case 'publish_at':
            return <PublishAtField {...props} />;
        default:
            break;
    }

    if (row.options?.length) {
        return <OptionSelect id={id} options={row.options} value={value?.value ?? ''} onChange={(next) => choose(row.options!, next, onChange)} disabled={disabled} />;
    }

    return (
        <input
            id={id}
            className={styles.input}
            value={value?.label ?? ''}
            onChange={(event) => onChange({ value: event.target.value, label: event.target.value })}
            disabled={disabled}
        />
    );
}
