import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { FolderSearchDialog } from '@/components/FolderSearchDialog/FolderSearchDialog';
import { Button } from '@/components/ui/button';
import { SelectChevron } from '@/components/ui/select';
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
    /** Opened by a click on the value: its list or the folder search opens, its text field takes the focus, at once. */
    opensAtOnce?: boolean;
    /** The user is done with it: chose, cancelled, or left its text field. */
    onDone?: () => void;
    /** The user began typing into its text field. */
    onEdit?: () => void;
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

// The entry of a folder's list that opens the folder search ("Other folder…"); never a folder id.
const OTHER_FOLDER = '\u0000other-folder';

// `folder`: never typed, and no button of its own. With options a select, whose last entry "Other
// folder…" opens the folder search wherever a folder off the options may be given; else the chosen
// folder as a value, which opens the folder search when clicked.
function FolderField({ id, row, value, onChange, disabled, opensAtOnce = false, onDone }: SettingFieldProps) {
    const { t } = useTranslation();
    // Folders chosen in the folder search, offered beside GenAIx's options from then on.
    const [picked, setPicked] = useState<Option[]>([]);
    const options = mergeOptions(undefined, row.options ?? [], picked);
    const canPick = row.canChooseOther || options.length === 0;
    // Without options to list, the click that opened the field opens the folder search.
    const [isPicking, setIsPicking] = useState(opensAtOnce && options.length === 0);
    // "Other folder…" was chosen: the list closing then does not end the editing, the folder search follows.
    const opensSearchRef = useRef(false);

    function pick(node: CmsNode, folder: CmsFolder) {
        const option = folderOption(node, folder);

        setPicked((current) => [...current.filter(({ value: other }) => other !== option.value), option]);
        setIsPicking(false);
        onChange(option);
        onDone?.();
    }

    function cancel() {
        setIsPicking(false);
        onDone?.();
    }

    function chooseFromList(next: string) {
        if (next === OTHER_FOLDER) {
            opensSearchRef.current = true;
            setIsPicking(true);
        } else {
            choose(options, next, onChange);
        }
    }

    // After the select reported the choice, whichever of the two it reports first.
    function closeList() {
        setTimeout(() => {
            if (!opensSearchRef.current) {
                onDone?.();
            }

            opensSearchRef.current = false;
        });
    }

    return (
        <>
            {options.length > 0
                ? (
                    <OptionSelect
                        id={id}
                        options={canPick ? [...options, { value: OTHER_FOLDER, label: t('chat.interaction.otherFolder') }] : options}
                        value={value?.value ?? ''}
                        onChange={chooseFromList}
                        onClose={closeList}
                        defaultOpen={opensAtOnce}
                        disabled={disabled}
                    />
                )
                : (
                    <Button id={id} variant="input" size="field" onClick={() => setIsPicking(true)} disabled={disabled}>
                        <span className={value ? styles.valueText : styles.valuePlaceholder}>
                            {value ? withPath(value.label, value.ref) : t('chat.interaction.chooseFolder')}
                        </span>
                        <span className={styles.chevron}><SelectChevron /></span>
                    </Button>
                )}
            {isPicking && <FolderSearchDialog onPick={pick} onCancel={cancel} />}
        </>
    );
}

// `template`: GenAIx's options, and the templates of the chosen folder besides them, also once another
// folder was chosen (the template chosen so far stays). Without either there is nothing to offer.
function TemplateField({ id, row, value, onChange, disabled, folder, opensAtOnce, onDone }: SettingFieldProps) {
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
            defaultOpen={opensAtOnce}
            onClose={onDone}
        />
    );
}

// `language`: the languages of the chosen folder's node, of every node while that is not known; GenAIx's
// options first. The value is the language code.
function LanguageField({ id, row, value, onChange, disabled, folder, opensAtOnce, onDone }: SettingFieldProps) {
    const nodes = useCmsNodes();
    const folderNodeId = folder?.ref?.node_id;
    const languages = useCmsLanguages(folderNodeId !== undefined ? [folderNodeId] : (nodes.data ?? []).map((node) => node.id));
    const options = mergeOptions(value, row.options ?? [], languages.languages.map((language) => ({ value: language.code, label: language.name })));

    return <OptionSelect id={id} options={options} value={value?.value ?? ''} onChange={(next) => choose(options, next, onChange)} disabled={disabled} defaultOpen={opensAtOnce} onClose={onDone} />;
}

// `node`: the nodes the user can see. The value is the node id.
function NodeField({ id, row, value, onChange, disabled, opensAtOnce, onDone }: SettingFieldProps) {
    const nodes = useCmsNodes();
    const options = mergeOptions(value, row.options ?? [], (nodes.data ?? []).map((node) => ({ value: String(node.id), label: node.name })));

    return <OptionSelect id={id} options={options} value={value?.value ?? ''} onChange={(next) => choose(options, next, onChange)} disabled={disabled} defaultOpen={opensAtOnce} onClose={onDone} />;
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

// The ref of a text field, which takes the focus when the click on its value opened it.
function useTextFieldFocus({ opensAtOnce = false }: SettingFieldProps) {
    const ref = useRef<HTMLInputElement>(null);

    useEffect(() => {
        if (opensAtOnce) {
            ref.current?.focus();
        }
    }, [opensAtOnce]);

    return ref;
}

// `publish_at`: the browser's date and time picker; the value is the moment as an ISO `date-time`.
function PublishAtField(props: SettingFieldProps) {
    const { id, value, onChange, disabled, onEdit, onDone } = props;
    const { i18n } = useTranslation();
    const ref = useTextFieldFocus(props);

    function change(local: string) {
        const date = new Date(local);

        if (local && !Number.isNaN(date.getTime())) {
            onChange({ value: date.toISOString(), label: date.toLocaleString(i18n.language) });
        }
    }

    return (
        <input
            ref={ref}
            id={id}
            type="datetime-local"
            className={styles.input}
            value={toLocalInput(value?.value ?? '')}
            onChange={(event) => change(event.target.value)}
            onFocus={onEdit}
            onBlur={onDone}
            disabled={disabled}
        />
    );
}

// Any other key: a constraint in the user's words, as free text.
function TextField(props: SettingFieldProps) {
    const { id, value, onChange, disabled, onEdit, onDone } = props;
    const ref = useTextFieldFocus(props);

    return (
        <input
            ref={ref}
            id={id}
            className={styles.input}
            value={value?.label ?? ''}
            onChange={(event) => onChange({ value: event.target.value, label: event.target.value })}
            onFocus={onEdit}
            onBlur={onDone}
            disabled={disabled}
        />
    );
}

/**
 * The field of one `settings_review` setting. `node`, `folder` (and `file_folder`, `image_folder`, the
 * folders uploads go into, rc.5), `template` and `language` are chosen, never typed: the value is the CMS id or the language code, which the agent needs (a name is `422`,
 * contract `UserSettingPart.value`). `publish_at` uses the date picker. Any other key, such as
 * `filename`, is a constraint in the user's words and stays free text; so does a key with GenAIx options
 * that none of these is, as a select over them.
 *
 * Opened by a click on its value (`opensAtOnce`), a list opens, the folder search opens or the text field
 * takes the focus at once; `onDone` once the user chose, cancelled or left the field.
 */
export function SettingField(props: SettingFieldProps) {
    const { id, row, value, onChange, disabled, opensAtOnce, onDone } = props;

    switch (row.key) {
        // `file_folder`, `image_folder`: the advisory folders an upload goes into (rc.5), `recommended` preselected.
        case 'folder':
        case 'file_folder':
        case 'image_folder':
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
        return <OptionSelect id={id} options={row.options} value={value?.value ?? ''} onChange={(next) => choose(row.options!, next, onChange)} disabled={disabled} defaultOpen={opensAtOnce} onClose={onDone} />;
    }

    return <TextField {...props} />;
}
