import type { Editor, JSONContent } from '@tiptap/core';
import { exitSuggestion } from '@tiptap/suggestion';
import { AtSignIcon, FileTextIcon, FolderIcon, GlobeIcon, ImageIcon, type LucideIcon, PaperclipIcon, XIcon } from 'lucide-react';
import { type ReactNode, type RefObject, useEffect, useId, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';

import { followReferenceTrigger, type ReferenceTrigger, referenceTriggerKey } from '@/components/ComposerTextInput/composerExtensions';
import { Button } from '@/components/ui/button';
import { FILE_REF_NODE, REFERENCE_NODE } from '@/helper/composerParts/composerParts';
import { formatMegabytes } from '@/helper/uploadLimits/uploadLimits';
import { useCmsNodes, useCmsSearch } from '@/hooks/useCmsQueries';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { useSessionUploads } from '@/hooks/useGenaixQueries';

import styles from './ReferenceMenu.module.css';

/** The groups of the menu, in the order shown (draft `GROUPS`, without guidelines). */
const GROUPS = ['page', 'folder', 'image', 'file', 'project'] as const;

type Group = typeof GROUPS[number];

const GROUP_ICONS: Record<Group, LucideIcon> = {
    page: FileTextIcon,
    folder: FolderIcon,
    image: ImageIcon,
    file: PaperclipIcon,
    project: GlobeIcon,
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

// The query after `@`: a group prefix (`page:`), if any, and the text to search for.
function parseQuery(query: string): { scope: Group | null; text: string; prefixLength: number } {
    const [, prefix = '', rest = ''] = /^([a-z]+):(.*)$/i.exec(query) ?? [];
    const scope = SCOPE_PREFIXES[prefix.toLowerCase()];

    return scope ? { scope, text: rest, prefixLength: prefix.length + 1 } : { scope: null, text: query, prefixLength: 0 };
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
}

/**
 * The @-menu of a composer's field (draft `#pop`): CMS pages, folders and images found for the text
 * after `@`, the session's uploaded files and the CMS nodes, grouped. Arrows, Home and End move,
 * Enter or a click inserts the token in place of `@query`, Tab narrows to the active entry's group,
 * Esc closes and keeps the text. The editor places it at the `@` (`ReferenceTriggerExtension`).
 */
export function ReferenceMenu({ editor, sessionId }: ReferenceMenuProps) {
    // The open menu as the editor reports it (query, where `@query` sits, the element to render
    // into), `null` while it is closed.
    const [openMenu, setOpenMenu] = useState<ReferenceTrigger | null>(null);
    // The key handling of the open list; while the menu is open, the editor offers it every key first.
    const listKeyHandlerRef = useRef<((event: KeyboardEvent) => boolean) | null>(null);

    // Follows the editor's @-trigger while mounted (`ReferenceTriggerExtension`).
    useEffect(() => followReferenceTrigger(editor, {
        onChange: setOpenMenu,
        onKeyDown: (event) => listKeyHandlerRef.current?.(event) ?? false,
    }), [editor]);

    if (!openMenu) {
        return null;
    }

    // A new `@` starts a new menu, without the selection or narrowing of the last one.
    return createPortal(
        <MenuList key={openMenu.range.from} editor={editor} openMenu={openMenu} sessionId={sessionId} listKeyHandlerRef={listKeyHandlerRef} />,
        openMenu.host,
    );
}

interface MenuListProps {
    editor: Editor;
    openMenu: ReferenceTrigger;
    sessionId?: string;
    /** Set to the list's key handling while it is shown. */
    listKeyHandlerRef: RefObject<((event: KeyboardEvent) => boolean) | null>;
}

function MenuList({ editor, openMenu, sessionId, listKeyHandlerRef }: MenuListProps) {
    const { t, i18n } = useTranslation();
    const listId = useId();
    const [active, setActive] = useState(0);
    // The group chosen with Tab; a prefix typed in the query wins over it.
    const [tabScope, setTabScope] = useState<Group | null>(null);
    const typed = parseQuery(openMenu.query);
    const scope = typed.scope ?? tabScope;
    const searchText = useDebouncedValue(typed.text.trim(), SEARCH_DELAY_MS);
    const nodes = useCmsNodes();
    const search = useCmsSearch(nodes.data ?? [], searchText);
    const uploads = useSessionUploads(sessionId);
    const text = typed.text.trim();

    const items: MenuItem[] = [
        ...search.results.flatMap(({ node, items: found }) => found.map((item): MenuItem => ({
            key: `${node.id}-${item.type}-${item.id}`,
            group: item.type,
            name: item.name,
            detail: item.path ?? node.name,
            token: { type: REFERENCE_NODE, attrs: { ref: { type: item.type, id: String(item.id), node_id: node.id, label: item.name } } },
        }))),
        ...(uploads.data ?? []).filter((file) => contains(file.name, text)).map((file): MenuItem => ({
            key: `file-${file.id}`,
            group: 'file',
            name: file.name,
            detail: `${t(`composer.attachment.${file.mode}`)} · ${formatMegabytes(file.size, i18n.language)}`,
            token: { type: FILE_REF_NODE, attrs: { fileId: file.id, mode: file.mode, name: file.name } },
        })),
        ...(nodes.data ?? []).filter((node) => contains(node.name, text)).map((node): MenuItem => ({
            key: `node-${node.id}`,
            group: 'project',
            name: node.name,
            detail: '',
            token: { type: REFERENCE_NODE, attrs: { ref: { type: 'node', id: String(node.id), node_id: node.id, label: node.name } } },
        })),
    ];
    const groups = GROUPS
        .filter((group) => scope === null || group === scope)
        .map((group) => ({ group, items: items.filter((item) => item.group === group) }))
        .filter(({ items: inGroup }) => inGroup.length > 0);
    // The entries in the order shown; the keyboard moves through these.
    const shown = groups.flatMap(({ items: inGroup }) => inGroup);
    const activeIndex = Math.min(active, Math.max(0, shown.length - 1));
    const activeItem = shown[activeIndex];
    const optionId = (index: number) => `${listId}-${index}`;
    const isLoading = nodes.isLoading || (searchText !== '' && search.isLoading) || uploads.isLoading;

    // Replaces `@query` with the entry's token and a space to go on typing.
    function pick(item: MenuItem) {
        editor.chain().focus().insertContentAt(openMenu.range, [item.token, { type: 'text', text: ' ' }]).run();
    }

    // Closes the menu; the typed `@query` stays as text.
    function keepAsText() {
        exitSuggestion(editor.view, referenceTriggerKey);
        editor.commands.focus();
    }

    // Drops the narrowing: a typed prefix is taken out of the query, a Tab narrowing is forgotten.
    function clearScope() {
        if (typed.scope) {
            const from = openMenu.range.from + 1;

            editor.chain().focus().deleteRange({ from, to: from + typed.prefixLength }).run();
        }

        setTabScope(null);
        setActive(0);
    }

    function handleKeyDown(event: KeyboardEvent): boolean {
        const count = shown.length;

        switch (event.key) {
            case 'ArrowDown':
                setActive(count ? (activeIndex + 1) % count : 0);

                return true;
            case 'ArrowUp':
                setActive(count ? (activeIndex - 1 + count) % count : 0);

                return true;
            case 'Home':
                setActive(0);

                return true;
            case 'End':
                setActive(Math.max(0, count - 1));

                return true;
            case 'Enter':
                if (activeItem) {
                    pick(activeItem);
                }

                // Never sends the message while the menu is open.
                return true;
            case 'Tab':
                if (scope === null && activeItem) {
                    setTabScope(activeItem.group);
                    setActive(0);
                }

                return true;
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
    const activeId = activeItem ? optionId(activeIndex) : null;

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
                <AtSignIcon size={14} aria-hidden />
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

            <div id={listId} role="listbox" aria-label={t('composer.mention.label')} className={styles.list}>
                {groups.map(({ group, items: inGroup }) => (
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

            {isLoading && <div className={styles.loading} role="status" aria-label={t('composer.mention.loading')} />}

            {search.isError && <p className={styles.failed}>{t('composer.mention.failed')}</p>}

            {!isLoading && shown.length === 0 && (
                <div className={styles.empty}>
                    <p>{text ? t('composer.mention.empty', { query: text }) : t('composer.mention.emptyNoQuery')}</p>
                    {text && (
                        <Button variant="secondary" size="sm" onMouseDown={(event) => event.preventDefault()} onClick={keepAsText}>
                            {t('composer.mention.asText', { query: openMenu.query })}
                        </Button>
                    )}
                </div>
            )}
        </div>
    );
}
