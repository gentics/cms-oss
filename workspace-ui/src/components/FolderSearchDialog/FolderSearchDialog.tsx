import { CheckIcon, ChevronRightIcon, FolderIcon } from 'lucide-react';
import { type KeyboardEvent, type ReactNode, useEffect, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { FOLDER_LEVEL_PAGE_SIZE, useCmsFolderLevel, useCmsFolderSearch, useCmsNodes } from '@/hooks/useCmsQueries';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import type { CmsFolder, CmsNode } from '@/services/cmsApiService/cmsApiService';
import { useRecentFoldersStore } from '@/store/useRecentFoldersStore';

import styles from './FolderSearchDialog.module.css';

// How long the text has to stay the same before the CMS is searched, as in the @-menu.
const SEARCH_DELAY_MS = 250;

/** A folder the user opened: its id, unless it is only known by name from a found folder's path. */
interface TrailStep {
    id: number | null;
    name: string;
}

/** One row of the list: the folder and, where the list mixes levels, where it lies. */
interface Row {
    key: string;
    folder: CmsFolder;
    detail?: string;
}

interface Section {
    key: string;
    head: string;
    rows: Row[];
}

// The names from below the node's root folder to `folder` itself, from its `path`
// (`/Website/Products/Cloud/` → `Products`, `Cloud`).
function folderNames(folder: CmsFolder): string[] {
    return folder.path ? folder.path.split('/').filter(Boolean).slice(1) : [folder.name];
}

// Where `folder` lies, for a list that mixes levels: the names above it, shortened in the middle
// past three (draft `fdCrumbTxt`), or the node's name for a folder on its root level.
function parentPath(folder: CmsFolder, node: CmsNode): string {
    const names = folderNames(folder).slice(0, -1);

    if (names.length === 0) {
        return node.name;
    }

    return (names.length > 3 ? [names[0], '…', ...names.slice(-2)] : names).join(' › ');
}

// Found folders, best first (draft `fdSearch`): the name is the text, starts with it, contains it,
// or only the description or id matched; then the shallower one. The CMS sorts them by name.
function rank(folders: CmsFolder[], text: string): CmsFolder[] {
    const needle = text.toLowerCase();
    const score = (folder: CmsFolder) => {
        const name = folder.name.toLowerCase();
        const at = name.indexOf(needle);
        const match = name === needle ? 0 : at === 0 ? 1 : at > 0 ? 2 : 3;

        return match * 100 + folderNames(folder).length;
    };

    return [...folders].sort((first, second) => score(first) - score(second));
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

interface FolderSearchDialogProps {
    /** The text searched for at first, e.g. what followed the `@`. */
    initialQuery?: string;
    /** A folder was chosen in `node`, the node or channel it was chosen in. */
    onPick: (node: CmsNode, folder: CmsFolder) => void;
    /** Closed without a folder: Cancel, the close button, Esc or a click beside it. */
    onCancel: () => void;
}

/**
 * The advanced folder search (draft `#fdDlg`, "Choose a folder"), for nodes with many folders and
 * channels. A node or channel is chosen at the top. Text searches all its folders and preselects
 * the best one (top `FOLDER_SEARCH_MAX_ITEMS`, with how many there are). Without text, the folders
 * chosen last come first, then the levels one at a time: a double click, the chevron or → opens a
 * folder, the path or ← goes back up, `FOLDER_LEVEL_PAGE_SIZE` at a time. A click or ↑ ↓ pick, only
 * Enter or "Use folder" takes the picked folder, Esc cancels. Mounted while open.
 */
export function FolderSearchDialog({ initialQuery = '', onPick, onCancel }: FolderSearchDialogProps) {
    const { t, i18n } = useTranslation();
    const nodeSelectId = useId();
    const listId = useId();
    const inputRef = useRef<HTMLInputElement>(null);
    const listRef = useRef<HTMLDivElement>(null);
    // Whether the picked folder changed by keyboard, so that the list scrolls it into view.
    const followsKeyboardRef = useRef(false);
    const nodes = useCmsNodes();
    const [nodeId, setNodeId] = useState<number | null>(null);
    const node = nodes.data?.find((candidate) => candidate.id === nodeId) ?? nodes.data?.[0];
    const [trail, setTrail] = useState<TrailStep[]>([]);
    const [query, setQuery] = useState(initialQuery);
    const [picked, setPicked] = useState<CmsFolder | null>(null);
    const text = query.trim();
    const searchText = useDebouncedValue(text, SEARCH_DELAY_MS);
    const isSearching = text !== '';
    const levelId = trail.at(-1)?.id ?? node?.folderId;
    const level = useCmsFolderLevel(isSearching ? undefined : node?.id, levelId);
    const search = useCmsFolderSearch(node, isSearching ? searchText : '');
    const recent = useRecentFoldersStore((state) => state.recent);
    const remember = useRecentFoldersStore((state) => state.remember);
    const format = (count: number) => count.toLocaleString(i18n.language);

    const sections: Section[] = [];

    if (node && isSearching && search.data) {
        const { folders, total } = search.data;

        sections.push({
            key: 'found',
            head: total > folders.length ? t('folderSearch.topMatches', { shown: format(folders.length), total: format(total) }) : t('folderSearch.matches', { count: total }),
            rows: rank(folders, searchText).map((folder) => ({ key: `found-${folder.id}`, folder, detail: parentPath(folder, node) })),
        });
    }

    if (node && !isSearching) {
        const recentHere = trail.length === 0 ? recent.filter((entry) => entry.nodeId === node.id) : [];

        if (recentHere.length > 0) {
            sections.push({
                key: 'recent',
                head: t('folderSearch.recent'),
                rows: recentHere.map(({ folder }) => ({ key: `recent-${folder.id}`, folder, detail: parentPath(folder, node) })),
            });
        }

        if (level.data) {
            const total = format(level.data.pages[0]?.total ?? 0);

            sections.push({
                key: 'level',
                head: trail.length === 0 ? t('folderSearch.allFolders', { total }) : t('folderSearch.inFolder', { name: trail.at(-1)!.name, total }),
                rows: level.data.pages.flatMap((page) => page.folders).map((folder) => ({ key: `level-${folder.id}`, folder })),
            });
        }
    }

    const rows = sections.flatMap((section) => section.rows);
    // The folders found for other text than typed, shown until the search for it answers.
    const isSearchStale = text !== searchText || search.isPlaceholderData;
    // While searching, the best folder is picked until another one is (draft: Enter takes it); not
    // one found for other text, which Enter would take before the answer.
    const best = isSearchStale ? null : rows[0]?.folder ?? null;
    const current = isSearching && !rows.some((row) => row.folder.id === picked?.id) ? best : picked;
    const activeKey = rows.find((row) => row.folder.id === current?.id)?.key;
    const optionId = (key: string) => `${listId}-${key}`;
    const loadedOnLevel = level.data?.pages.reduce((sum, page) => sum + page.folders.length, 0) ?? 0;
    const levelTotal = level.data?.pages[0]?.total ?? 0;
    const isLoading = nodes.isLoading || (isSearching ? isSearchStale || search.isLoading : level.isLoading);
    const isError = nodes.isError || (isSearching ? search.isError : level.isError);
    const isLocal = (folder: CmsFolder) => node?.type === 'channel' && !folder.inherited;

    function choose(folder: CmsFolder | null) {
        if (!folder || !node) {
            return;
        }

        remember(node.id, folder);
        onPick(node, folder);
    }

    // Moves the pick by `step` through the folders shown, each once.
    function move(step: number) {
        const folders = rows.map((row) => row.folder).filter((folder, index, all) => all.findIndex((other) => other.id === folder.id) === index);

        if (folders.length === 0) {
            return;
        }

        const index = folders.findIndex((folder) => folder.id === current?.id);
        const next = index < 0 ? (step > 0 ? 0 : folders.length - 1) : Math.max(0, Math.min(folders.length - 1, index + step));

        followsKeyboardRef.current = true;
        setPicked(folders[next]);
    }

    // The keys work in the search field only: the folder search keeps the focus there.
    function focusSearch() {
        inputRef.current?.focus();
    }

    // Shows the folders in `folder` and picks it, so that it can be taken as well. A found folder
    // brings the names above it along; those are not opened again from the path.
    function open(folder: CmsFolder) {
        const above = isSearching ? folderNames(folder).slice(0, -1).map((name): TrailStep => ({ id: null, name })) : trail;

        setTrail([...above, { id: folder.id, name: folder.name }]);
        setQuery('');
        setPicked(folder);
        listRef.current?.scrollTo?.({ top: 0 });
        focusSearch();
    }

    // Back to the level of the trail's step `index`, -1 for the node's root level.
    function goTo(index: number) {
        setTrail(trail.slice(0, index + 1));
        listRef.current?.scrollTo?.({ top: 0 });
        focusSearch();
    }

    // One level up, to the nearest step that can be opened; the folder left stays picked.
    function up() {
        const index = trail.slice(0, -1).findLastIndex((step) => step.id !== null);

        goTo(index);
    }

    function chooseNode(value: unknown) {
        setNodeId(Number(value));
        setTrail([]);
        setPicked(null);
        focusSearch();
    }

    function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
        switch (event.key) {
            case 'ArrowDown':
                event.preventDefault();
                move(1);
                break;
            case 'ArrowUp':
                event.preventDefault();
                move(-1);
                break;
            case 'Enter':
                event.preventDefault();
                choose(current);
                break;
            case 'ArrowRight':
                if (!isSearching && current?.hasSubfolders) {
                    event.preventDefault();
                    open(current);
                }

                break;
            case 'ArrowLeft':
                if (!isSearching && trail.length > 0) {
                    event.preventDefault();
                    up();
                }

                break;
            default:
                break;
        }
    }

    // Keeps the folder the keyboard picked in view.
    useEffect(() => {
        if (!followsKeyboardRef.current) {
            return;
        }

        followsKeyboardRef.current = false;

        if (activeKey) {
            document.getElementById(optionId(activeKey))?.scrollIntoView({ block: 'nearest' });
        }
    });

    const nodeItems = Object.fromEntries((nodes.data ?? []).map((candidate) => [
        String(candidate.id),
        candidate.type === 'channel' && candidate.inheritedFromName ? t('folderSearch.channelOf', { name: candidate.name, master: candidate.inheritedFromName }) : candidate.name,
    ]));
    const pickedNames = current ? folderNames(current) : [];

    return (
        <Dialog open onOpenChange={(isOpen) => !isOpen && onCancel()}>
            <DialogContent initialFocus={inputRef} finalFocus={false}>
                <DialogHeader>
                    <div className={styles.head}>
                        <DialogTitle>{t('folderSearch.title')}</DialogTitle>
                        <div className={styles.node}>
                            <Label htmlFor={nodeSelectId}>{t('folderSearch.node')}</Label>
                            <Select items={nodeItems} value={node ? String(node.id) : null} onValueChange={chooseNode} disabled={!nodes.data?.length}>
                                <SelectTrigger id={nodeSelectId}>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {Object.entries(nodeItems).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}
                                </SelectContent>
                            </Select>
                        </div>
                    </div>
                </DialogHeader>

                <div className={styles.search}>
                    <Input
                        ref={inputRef}
                        type="search"
                        autoComplete="off"
                        role="combobox"
                        aria-expanded="true"
                        aria-autocomplete="list"
                        aria-controls={listId}
                        aria-activedescendant={activeKey ? optionId(activeKey) : undefined}
                        aria-label={t('folderSearch.search')}
                        placeholder={node ? t('folderSearch.placeholder', { node: node.name }) : t('folderSearch.search')}
                        value={query}
                        onChange={(event) => setQuery(event.target.value)}
                        onKeyDown={handleKeyDown}
                    />
                </div>

                {node && !isSearching && (
                    <nav className={styles.crumbs} aria-label={t('folderSearch.path')}>
                        {trail.length === 0
                            ? <b aria-current="location">{node.name}</b>
                            : <Button variant="ghost" size="sm" onClick={() => goTo(-1)}>{node.name}</Button>}
                        {trail.map((step, index) => (
                            <span key={`${index}-${step.name}`} className={styles.crumb}>
                                <ChevronRightIcon size={12} aria-hidden />
                                {index === trail.length - 1
                                    ? <b aria-current="location">{step.name}</b>
                                    : step.id === null
                                        ? <span>{step.name}</span>
                                        : <Button variant="ghost" size="sm" onClick={() => goTo(index)}>{step.name}</Button>}
                            </span>
                        ))}
                    </nav>
                )}

                <div ref={listRef} className={styles.body}>
                    <div id={listId} role="listbox" aria-label={t('folderSearch.list')}>
                        {sections.map((section) => (
                            <div key={section.key} role="group" aria-labelledby={`${listId}-${section.key}`}>
                                <div id={`${listId}-${section.key}`} className={styles.groupHead}>{section.head}</div>
                                {section.rows.map((row) => (
                                    <div
                                        key={row.key}
                                        id={optionId(row.key)}
                                        role="option"
                                        aria-selected={row.key === activeKey}
                                        className={styles.row}
                                        // Keeps the focus in the search field, where Enter takes the folder.
                                        onMouseDown={(event) => event.preventDefault()}
                                        onClick={() => setPicked(row.folder)}
                                        onDoubleClick={() => row.folder.hasSubfolders && open(row.folder)}
                                    >
                                        <span className={styles.icon}><FolderIcon size={16} aria-hidden /></span>
                                        <span className={styles.text}>
                                            <span className={styles.name}>{highlight(row.folder.name, isSearching ? searchText : '')}</span>
                                            {row.detail && <span className={styles.detail}>{row.detail}</span>}
                                        </span>
                                        {isLocal(row.folder) && <span className={styles.badge} title={t('folderSearch.localHint')}>{t('folderSearch.local')}</span>}
                                        {row.folder.hasSubfolders && (
                                            <Button
                                                variant="ghost"
                                                size="icon"
                                                tabIndex={-1}
                                                aria-label={t('folderSearch.open', { name: row.folder.name })}
                                                title={t('folderSearch.open', { name: row.folder.name })}
                                                onClick={(event) => {
                                                    event.stopPropagation();
                                                    open(row.folder);
                                                }}
                                                onDoubleClick={(event) => event.stopPropagation()}
                                            >
                                                <ChevronRightIcon size={16} aria-hidden />
                                            </Button>
                                        )}
                                    </div>
                                ))}
                            </div>
                        ))}
                    </div>

                    {!isSearching && level.hasNextPage && (
                        <div className={styles.more}>
                            <Button
                                variant="ghost"
                                size="sm"
                                disabled={level.isFetchingNextPage}
                                onClick={() => {
                                    void level.fetchNextPage();
                                    focusSearch();
                                }}
                            >
                                {t('folderSearch.showMore', {
                                    count: Math.min(FOLDER_LEVEL_PAGE_SIZE, levelTotal - loadedOnLevel),
                                    remaining: format(levelTotal - loadedOnLevel),
                                })}
                            </Button>
                        </div>
                    )}

                    {(isLoading || level.isFetchingNextPage) && <p className={styles.note} role="status">{t('folderSearch.loading')}</p>}
                    {isError && <p className={styles.note}>{t('folderSearch.failed')}</p>}
                    {node && !isLoading && !isError && rows.length === 0 && (
                        <p className={styles.note}>
                            {isSearching ? t('folderSearch.empty', { query: searchText, node: node.name }) : t('folderSearch.emptyLevel')}
                        </p>
                    )}
                </div>

                <DialogFooter>
                    <span className={styles.picked}>
                        {current && node
                            ? <>{node.name} › <b>{pickedNames.join(' › ')}</b></>
                            : t('folderSearch.pick')}
                    </span>
                    <Button variant="secondary" size="sm" onClick={onCancel}>{t('folderSearch.cancel')}</Button>
                    <Button variant="primary" size="sm" disabled={!current} onClick={() => choose(current)}>
                        <CheckIcon size={14} aria-hidden />
                        {t('folderSearch.use')}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
