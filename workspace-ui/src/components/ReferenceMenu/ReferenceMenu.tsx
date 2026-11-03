import type { Editor, JSONContent, Range } from '@tiptap/core';
import { exitSuggestion } from '@tiptap/suggestion';
import { AtSignIcon, FileTextIcon, FolderIcon, FolderTreeIcon, GlobeIcon, HashIcon, ImageIcon, LanguagesIcon, LayoutTemplateIcon, type LucideIcon, PaperclipIcon, XIcon } from 'lucide-react';
import { type ReactNode, type RefObject, useEffect, useId, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';

import { followReferenceTrigger, followSettingTrigger, type ReferenceTrigger, referenceTriggerKey, reopenReferenceMenu, settingTriggerKey } from '@/components/ComposerTextInput/composerExtensions';
import { FolderSearchDialog } from '@/components/FolderSearchDialog/FolderSearchDialog';
import { withPath } from '@/components/InteractionCard/withPath';
import { Button } from '@/components/ui/button';
import { FILE_REF_NODE, REFERENCE_NODE, SETTING_NODE } from '@/helper/composerParts/composerParts';
import { formatMegabytes } from '@/helper/uploadLimits/uploadLimits';
import { useCmsLanguages, useCmsNodes, useCmsSearch, useCmsTemplates } from '@/hooks/useCmsQueries';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { useSessionUploads } from '@/hooks/useGenaixQueries';
import { CMS_ITEM_TYPES, type CmsFolder, type CmsItemType, type CmsNode } from '@/services/cmsApiService/cmsApiService';

import styles from './ReferenceMenu.module.css';

/** The groups of the @-menu, in the order shown (draft `GROUPS`, without guidelines). */
const GROUPS = ['page', 'folder', 'image', 'file', 'project'] as const;

/** The groups of the #-menu: the settings a chip can set, in the order shown. */
const SETTING_GROUPS = ['folder', 'template', 'language'] as const;

type Group = typeof GROUPS[number] | typeof SETTING_GROUPS[number];

/** What the menu inserts: references (`@`) or setting chips (`#`, contract `UserSettingPart`). */
export type ReferenceMenuMode = 'reference' | 'setting';

const MODE_GROUPS: Record<ReferenceMenuMode, readonly Group[]> = { reference: GROUPS, setting: SETTING_GROUPS };

const GROUP_ICONS: Record<Group, LucideIcon> = {
    page: FileTextIcon,
    folder: FolderIcon,
    image: ImageIcon,
    file: PaperclipIcon,
    project: GlobeIcon,
    template: LayoutTemplateIcon,
    language: LanguagesIcon,
};

// The prefixes that narrow the menu to one group, typed as `@page:…` (draft `SCOPEMAP`).
const SCOPE_PREFIXES: Record<string, Group> = {
    page: 'page',
    pages: 'page',
    folder: 'folder',
    folders: 'folder',
    image: 'image',
    images: 'image',
    img: 'image',
    file: 'file',
    files: 'file',
    project: 'project',
    node: 'project',
    nodes: 'project',
    template: 'template',
    templates: 'template',
    language: 'language',
    languages: 'language',
    lang: 'language',
};

// How long the query has to stay the same before the CMS is searched, as in `SessionSearch`.
const SEARCH_DELAY_MS = 250;

/** One entry of the menu and the token it inserts. */
interface MenuItem {
    key: string;
    group: Group;
    name: string;
    detail: string;
    token: JSONContent;
}

// The query after `@` or `#`: a prefix of one of `groups` (`page:`), if any, and the text to search for.
function parseQuery(query: string, groups: readonly Group[]): { scope: Group | null; text: string; prefixLength: number } {
    const [, prefix = '', rest = ''] = /^([a-z]+):(.*)$/i.exec(query) ?? [];
    const scope = SCOPE_PREFIXES[prefix.toLowerCase()];

    return scope && groups.includes(scope) ? { scope, text: rest, prefixLength: prefix.length + 1 } : { scope: null, text: query, prefixLength: 0 };
}

// The CMS object types searched within `scope`: all of them without one (only folders for setting
// chips), none for a group that is not in the CMS search (session files, projects, templates, languages).
function searchedTypes(scope: Group | null, mode: ReferenceMenuMode): readonly CmsItemType[] {
    const types = mode === 'setting' ? CMS_ITEM_TYPES.filter((type) => type === 'folder') : CMS_ITEM_TYPES;

    return scope === null ? types : types.filter((type) => type === scope);
}

// A setting chip (`#[key: value]`): the value the agent needs (an id or a language code) and the wording shown.
function settingToken(key: string, value: string, label: string): JSONContent {
    return { type: SETTING_NODE, attrs: { key, value, label } };
}

// The folder of the field's `#[folder: …]` chip, if there is one: its templates are the ones to offer.
function chosenFolderId(editor: Editor): number | undefined {
    let folderId: number | undefined;

    editor.state.doc.descendants((node) => {
        if (node.type.name === SETTING_NODE && node.attrs.key === 'folder') {
            folderId = Number(node.attrs.value);
        }
    });

    return folderId !== undefined && Number.isInteger(folderId) ? folderId : undefined;
}

// Whether `name` contains `text`, ignoring case; every name contains the empty text.
function contains(name: string, text: string): boolean {
    return name.toLowerCase().includes(text.toLowerCase());
}

// `name` with the first occurrence of `text` marked.
function highlight(name: string, text: string): ReactNode {
    const index = text ? name.toLowerCase().indexOf(text.toLowerCase()) : -1;

    if (index < 0) {
        return name;
    }

    return (
        <>
            {name.slice(0, index)}
            <mark>{name.slice(index, index + text.length)}</mark>
            {name.slice(index + text.length)}
        </>
    );
}

interface ReferenceMenuProps {
    editor: Editor;
    /** The session whose uploaded files the menu offers; without one there is no Files group. */
    sessionId?: string;
    /** `setting` for the #-menu of setting chips; the @-menu of references by default. */
    mode?: ReferenceMenuMode;
}

/**
 * The @-menu of a composer's field (draft `#pop`). A bare `@` offers the groups; Enter, Tab or a
 * click on one narrows the menu to it and shows what it holds (CMS objects: those on the nodes' root
 * levels), and Backspace goes back to the groups. Text after `@` finds CMS pages, folders and images
 * (only the narrowed type, if any), the session's uploaded files and the CMS nodes, grouped.
 * Arrows, Home and End move, Enter or a click inserts the token in place of `@query`, Tab narrows to
 * the active entry's group, Esc closes and keeps the text. The editor places it at the `@`
 * (`ReferenceTriggerExtension`). Narrowed to folders, its last entry opens the advanced folder search
 * (`FolderSearchDialog`) with the typed text; the folder chosen there replaces `@query`, and Cancel
 * goes back to the menu.
 *
 * With `mode="setting"` it is the #-menu (`SettingTriggerExtension`): the groups folder, template and
 * language, and what it inserts are setting chips with the CMS id or language code as their value, which
 * GenAIx takes as the user's decision and asks no more about (contract `UserSettingPart`). Templates are
 * those of the folder chip already in the field.
 */
export function ReferenceMenu({ editor, sessionId, mode = 'reference' }: ReferenceMenuProps) {
    const triggerKey = mode === 'setting' ? settingTriggerKey : referenceTriggerKey;

    // The open menu as the editor reports it (query, where `@query` sits, the element to render
    // into), `null` while it is closed.
    const [openMenu, setOpenMenu] = useState<ReferenceTrigger | null>(null);
    // The key handling of the open list; while the menu is open, the editor offers it every key first.
    const listKeyHandlerRef = useRef<((event: KeyboardEvent) => boolean) | null>(null);
    // The advanced folder search while open: where `@query` sits, and the text it started with.
    const [advanced, setAdvanced] = useState<{ range: Range; query: string } | null>(null);
    // The group the menu opens narrowed to: folders, when Cancel in the folder search goes back to it.
    const [reopenScope, setReopenScope] = useState<Group | null>(null);

    // Follows the editor's @-trigger while mounted (`ReferenceTriggerExtension`).
    useEffect(() => (mode === 'setting' ? followSettingTrigger : followReferenceTrigger)(editor, {
        onChange: (trigger) => {
            setOpenMenu(trigger);

            if (!trigger) {
                setReopenScope(null);
            }
        },
        onKeyDown: (event) => listKeyHandlerRef.current?.(event) ?? false,
    }), [editor, mode]);

    // Closes the menu for the folder search; `@query` stays in the field until a folder is chosen.
    function openAdvanced(query: string) {
        if (!openMenu) {
            return;
        }

        setAdvanced({ range: openMenu.range, query });
        exitSuggestion(editor.view, triggerKey);
    }

    // Replaces `@query` with the folder's token and a space to go on typing.
    function pickFolder(node: CmsNode, folder: CmsFolder) {
        if (!advanced) {
            return;
        }

        const ref = { type: 'folder' as const, id: String(folder.id), node_id: node.id, label: folder.name, ...(folder.path && { path: folder.path }) };
        const token = mode === 'setting' ? settingToken('folder', ref.id, withPath(folder.name, ref)) : { type: REFERENCE_NODE, attrs: { ref } };

        setAdvanced(null);
        editor.chain().focus().insertContentAt(advanced.range, [token, { type: 'text', text: ' ' }]).run();
    }

    // Back to the menu, narrowed to folders, with the caret at the end of `@query`.
    function cancelAdvanced() {
        if (!advanced) {
            return;
        }

        setAdvanced(null);
        setReopenScope('folder');
        reopenReferenceMenu(editor, advanced.range.to);
    }

    return (
        <>
            {openMenu && createPortal(
                // A new `@` starts a new menu, without the selection or narrowing of the last one.
                <MenuList
                    key={openMenu.range.from}
                    editor={editor}
                    openMenu={openMenu}
                    sessionId={sessionId}
                    mode={mode}
                    initialScope={reopenScope}
                    listKeyHandlerRef={listKeyHandlerRef}
                    onAdvancedSearch={openAdvanced}
                />,
                openMenu.host,
            )}
            {advanced && <FolderSearchDialog initialQuery={advanced.query} onPick={pickFolder} onCancel={cancelAdvanced} />}
        </>
    );
}

interface MenuListProps {
    editor: Editor;
    openMenu: ReferenceTrigger;
    sessionId?: string;
    mode: ReferenceMenuMode;
    /** The group the menu starts narrowed to, if any. */
    initialScope: Group | null;
    /** Set to the list's key handling while it is shown. */
    listKeyHandlerRef: RefObject<((event: KeyboardEvent) => boolean) | null>;
    /** Opens the advanced folder search with the typed text. */
    onAdvancedSearch: (query: string) => void;
}

function MenuList({ editor, openMenu, sessionId, mode, initialScope, listKeyHandlerRef, onAdvancedSearch }: MenuListProps) {
    const { t, i18n } = useTranslation();
    const listId = useId();
    const [active, setActive] = useState(0);
    const listRef = useRef<HTMLDivElement>(null);
    // Whether the active entry changed by keyboard, so that the list scrolls it into view.
    const followsKeyboardRef = useRef(false);
    // The group chosen with Tab; a prefix typed in the query wins over it.
    const [tabScope, setTabScope] = useState<Group | null>(initialScope);
    const modeGroups = MODE_GROUPS[mode];
    const typed = parseQuery(openMenu.query, modeGroups);
    const scope = typed.scope ?? tabScope;
    const text = typed.text.trim();
    const searchText = useDebouncedValue(text, SEARCH_DELAY_MS);
    // Without text, the nodes' root levels: loaded with the bare `@`, so a group chosen next shows
    // them at once. Text is searched once it has stayed the same for a moment.
    const query = text === '' ? '' : searchText;
    const types = searchedTypes(scope, mode);
    const nodes = useCmsNodes();
    const search = useCmsSearch(nodes.data ?? [], query, types);
    const isSetting = mode === 'setting';
    const uploads = useSessionUploads(isSetting ? undefined : sessionId);
    // Setting chips: the templates of the folder chip in the field, the languages of every node.
    const folderId = isSetting ? chosenFolderId(editor) : undefined;
    const templates = useCmsTemplates(folderId, undefined);
    const languages = useCmsLanguages(isSetting ? (nodes.data ?? []).map((node) => node.id) : []);
    // A bare `@` or `#` offers the groups to narrow to first; the Files group only in a session.
    const isChoosingGroup = scope === null && text === '';
    const groupChoices = modeGroups.filter((group) => group !== 'file' || sessionId !== undefined);
    // CMS objects found for other text than typed: narrowed to the typed text until the answer.
    const isStale = text !== query || search.isStale;

    const items: MenuItem[] = [
        ...search.results.flatMap(({ node, items: found }) => found.filter((item) => !isStale || contains(item.name, text)).map((item): MenuItem => {
            // `path` tells same-named objects apart for GenAIx too (contract `ContextReference.path`).
            const ref = { type: item.type, id: String(item.id), node_id: node.id, label: item.name, ...(item.path && { path: item.path }) };

            return {
                key: `${node.id}-${item.type}-${item.id}`,
                group: item.type,
                name: item.name,
                detail: item.path ?? node.name,
                token: isSetting ? settingToken('folder', ref.id, withPath(item.name, ref)) : { type: REFERENCE_NODE, attrs: { ref } },
            };
        })),
        ...(uploads.data ?? []).filter((file) => contains(file.name, text)).map((file): MenuItem => ({
            key: `file-${file.id}`,
            group: 'file',
            name: file.name,
            detail: `${t(`composer.attachment.${file.mode}`)} · ${formatMegabytes(file.size, i18n.language)}`,
            token: { type: FILE_REF_NODE, attrs: { fileId: file.id, mode: file.mode, name: file.name } },
        })),
        ...(isSetting ? [] : nodes.data ?? []).filter((node) => contains(node.name, text)).map((node): MenuItem => ({
            key: `node-${node.id}`,
            group: 'project',
            name: node.name,
            detail: '',
            token: { type: REFERENCE_NODE, attrs: { ref: { type: 'node', id: String(node.id), node_id: node.id, label: node.name } } },
        })),
        ...(templates.data ?? []).filter((template) => contains(template.name, text)).map((template): MenuItem => ({
            key: `template-${template.id}`,
            group: 'template',
            name: template.name,
            detail: '',
            token: settingToken('template', String(template.id), template.name),
        })),
        ...languages.languages.filter((language) => contains(language.name, text) || contains(language.code, text)).map((language): MenuItem => ({
            key: `language-${language.code}`,
            group: 'language',
            name: language.name,
            detail: language.code,
            token: settingToken('language', language.code, language.name),
        })),
    ];
    const groups = modeGroups
        .filter((group) => scope === null || group === scope)
        .map((group) => ({ group, items: items.filter((item) => item.group === group) }))
        .filter(({ items: inGroup }) => inGroup.length > 0);
    // The entries in the order shown; the keyboard moves through these.
    const shown = groups.flatMap(({ items: inGroup }) => inGroup);
    // Narrowed to folders, the advanced folder search is the last entry, after those found.
    const offersAdvanced = scope === 'folder';
    const optionCount = isChoosingGroup ? groupChoices.length : shown.length + (offersAdvanced ? 1 : 0);
    const activeIndex = Math.min(active, Math.max(0, optionCount - 1));
    const activeItem = isChoosingGroup ? undefined : shown[activeIndex];
    const activeGroup = isChoosingGroup ? groupChoices[activeIndex] : undefined;
    const isAdvancedActive = offersAdvanced && activeIndex === shown.length;
    const optionId = (index: number) => `${listId}-${index}`;
    // Searching from the first key on: also while the text waits out the delay, so that the menu
    // does not say "nothing found" before the CMS was asked.
    const isSearching = types.length > 0 && (text !== query || search.isLoading);
    const isLoading = nodes.isLoading || isSearching || uploads.isLoading || templates.isLoading || (isSetting && languages.isPending);
    // Narrowed to templates without a folder chip: there is no folder to take them from yet.
    const needsFolder = scope === 'template' && folderId === undefined;

    // Replaces `@query` with the entry's token and a space to go on typing.
    function pick(item: MenuItem) {
        editor.chain().focus().insertContentAt(openMenu.range, [item.token, { type: 'text', text: ' ' }]).run();
    }

    // Closes the menu; the typed `@query` stays as text.
    function keepAsText() {
        exitSuggestion(editor.view, isSetting ? settingTriggerKey : referenceTriggerKey);
        editor.commands.focus();
    }

    // Makes the entry at `index` the active one from the keyboard: the list scrolls to it.
    function moveTo(index: number) {
        followsKeyboardRef.current = true;
        setActive(index);
    }

    // Narrows the menu to `group`, chosen from the groups, with Tab or the right arrow.
    function narrowTo(group: Group) {
        setTabScope(group);
        moveTo(0);
    }

    // Drops the narrowing: a typed prefix is taken out of the query, a Tab narrowing is forgotten.
    function clearScope() {
        if (typed.scope) {
            const from = openMenu.range.from + 1;

            editor.chain().focus().deleteRange({ from, to: from + typed.prefixLength }).run();
        }

        setTabScope(null);
        moveTo(0);
    }

    function handleKeyDown(event: KeyboardEvent): boolean {
        const count = optionCount;

        switch (event.key) {
            case 'ArrowDown':
                moveTo(count ? (activeIndex + 1) % count : 0);

                return true;
            case 'ArrowUp':
                moveTo(count ? (activeIndex - 1 + count) % count : 0);

                return true;
            case 'Home':
                moveTo(0);

                return true;
            case 'End':
                moveTo(Math.max(0, count - 1));

                return true;
            case 'ArrowRight': {
                // Into the active group, or the active entry's group; otherwise the caret moves.
                const group = activeGroup ?? (scope === null ? activeItem?.group : undefined);

                if (group) {
                    narrowTo(group);

                    return true;
                }

                return false;
            }
            case 'ArrowLeft':
                // Out of a group with nothing typed in it, back to the groups.
                if (scope !== null && text === '') {
                    clearScope();

                    return true;
                }

                return false;
            case 'Enter':
                if (activeGroup) {
                    narrowTo(activeGroup);
                } else if (isAdvancedActive) {
                    onAdvancedSearch(text);
                } else if (activeItem) {
                    pick(activeItem);
                }

                // Never sends the message while the menu is open.
                return true;
            case 'Tab': {
                const group = activeGroup ?? activeItem?.group;

                if (scope === null && group) {
                    narrowTo(group);
                }

                return true;
            }
            case 'Backspace':
                // Back from a chosen group to the groups, instead of deleting the `@`.
                if (tabScope !== null && openMenu.query === '') {
                    clearScope();

                    return true;
                }

                return false;
            default:
                return false;
        }
    }

    useEffect(() => {
        listKeyHandlerRef.current = handleKeyDown;
    });

    useEffect(() => () => {
        listKeyHandlerRef.current = null;
    }, [listKeyHandlerRef]);

    // The field is the textbox that controls the list and names its active entry.
    const activeId = activeItem || activeGroup || isAdvancedActive ? optionId(activeIndex) : null;

    // Keeps the entry the keyboard moved to in view; the first one with the list's top, its group's
    // head included, and any other with the head of its group if it is the first there.
    useEffect(() => {
        const list = listRef.current;

        if (!followsKeyboardRef.current || !list) {
            return;
        }

        followsKeyboardRef.current = false;

        const option = activeId ? document.getElementById(activeId) : null;

        if (!option || activeIndex === 0) {
            list.scrollTop = 0;

            return;
        }

        if (option.previousElementSibling?.getAttribute('role') !== 'option') {
            option.previousElementSibling?.scrollIntoView({ block: 'nearest' });
        }

        option.scrollIntoView({ block: 'nearest' });
    });

    useEffect(() => {
        const field = editor.view.dom;

        field.setAttribute('aria-controls', listId);
        field.setAttribute('aria-expanded', 'true');

        if (activeId) {
            field.setAttribute('aria-activedescendant', activeId);
        } else {
            field.removeAttribute('aria-activedescendant');
        }

        return () => {
            field.removeAttribute('aria-controls');
            field.removeAttribute('aria-expanded');
            field.removeAttribute('aria-activedescendant');
        };
    }, [editor, listId, activeId]);

    return (
        <div className={styles.menu}>
            <div className={styles.head}>
                {isSetting ? <HashIcon size={14} aria-hidden /> : <AtSignIcon size={14} aria-hidden />}
                {scope && (
                    <span className={styles.scope}>
                        {t(`composer.mention.groups.${scope}`)}
                        <Button
                            variant="ghost"
                            size="icon-xs"
                            aria-label={t('composer.mention.clearScope')}
                            title={t('composer.mention.clearScope')}
                            onMouseDown={(event) => event.preventDefault()}
                            onClick={clearScope}
                        >
                            <XIcon size={12} aria-hidden />
                        </Button>
                    </span>
                )}
                <span className={styles.query}>{text ? `“${text}”` : t('composer.mention.search')}</span>
            </div>

            <div
                ref={listRef}
                id={listId}
                role="listbox"
                aria-label={t(isSetting ? 'composer.setting.label' : 'composer.mention.label')}
                // The advanced folder search below the states of the search belongs to the list.
                aria-owns={offersAdvanced ? optionId(shown.length) : undefined}
                className={styles.list}
            >
                {isChoosingGroup && groupChoices.map((group, position) => {
                    const Icon = GROUP_ICONS[group];

                    return (
                        <div
                            key={group}
                            id={optionId(position)}
                            role="option"
                            aria-selected={position === activeIndex}
                            className={styles.item}
                            // Keeps the caret in the field.
                            onMouseDown={(event) => event.preventDefault()}
                            onMouseMove={() => setActive(position)}
                            onClick={() => narrowTo(group)}
                        >
                            <span className={styles.icon}><Icon size={16} aria-hidden /></span>
                            <span className={styles.text}>
                                <span className={styles.name}>{t(`composer.mention.groups.${group}`)}</span>
                            </span>
                        </div>
                    );
                })}
                {!isChoosingGroup && groups.map(({ group, items: inGroup }) => (
                    <div key={group} role="group" aria-labelledby={`${listId}-${group}`}>
                        <div id={`${listId}-${group}`} className={styles.groupHead}>
                            {t(`composer.mention.groups.${group}`)} · {inGroup.length}
                        </div>
                        {inGroup.map((item) => {
                            const position = shown.indexOf(item);
                            const Icon = GROUP_ICONS[item.group];

                            return (
                                <div
                                    key={item.key}
                                    id={optionId(position)}
                                    role="option"
                                    aria-selected={position === activeIndex}
                                    className={styles.item}
                                    // Keeps the caret in the field.
                                    onMouseDown={(event) => event.preventDefault()}
                                    onMouseMove={() => setActive(position)}
                                    onClick={() => pick(item)}
                                >
                                    <span className={styles.icon}><Icon size={16} aria-hidden /></span>
                                    <span className={styles.text}>
                                        <span className={styles.name}>{highlight(item.name, text)}</span>
                                        {item.detail && <span className={styles.detail}>{item.detail}</span>}
                                    </span>
                                </div>
                            );
                        })}
                    </div>
                ))}
            </div>

            {!isChoosingGroup && isLoading && (
                // Rows in the shape of entries: three in place of an empty list, one below found ones.
                <div className={styles.loading} role="status" aria-label={t('composer.mention.loading')}>
                    {Array.from({ length: shown.length === 0 ? 3 : 1 }, (_, index) => (
                        <div key={index} className={styles.skeletonRow} aria-hidden>
                            <span className={styles.skeletonIcon} />
                            <span className={styles.skeletonText}>
                                <span className={styles.skeletonLine} />
                                <span className={styles.skeletonLine} />
                            </span>
                        </div>
                    ))}
                </div>
            )}

            {!isChoosingGroup && search.isError && <p className={styles.failed}>{t('composer.mention.failed')}</p>}

            {!isChoosingGroup && !isLoading && shown.length === 0 && (
                <div className={styles.empty}>
                    <p>{needsFolder ? t('composer.setting.chooseFolderFirst') : text ? t('composer.mention.empty', { query: text }) : t('composer.mention.emptyNoQuery')}</p>
                    {text && (
                        <Button variant="secondary" size="sm" onMouseDown={(event) => event.preventDefault()} onClick={keepAsText}>
                            {t('composer.mention.asText', { query: openMenu.query })}
                        </Button>
                    )}
                </div>
            )}

            {offersAdvanced && (
                <div
                    id={optionId(shown.length)}
                    role="option"
                    aria-selected={isAdvancedActive}
                    className={styles.advanced}
                    // Keeps the caret in the field.
                    onMouseDown={(event) => event.preventDefault()}
                    onMouseMove={() => setActive(shown.length)}
                    onClick={() => onAdvancedSearch(text)}
                >
                    <span className={styles.icon}><FolderTreeIcon size={16} aria-hidden /></span>
                    <span className={styles.text}>
                        <span className={styles.name}>{t('composer.mention.advanced')}</span>
                        <span className={styles.detail}>{t('composer.mention.advancedDetail')}</span>
                    </span>
                </div>
            )}
        </div>
    );
}
