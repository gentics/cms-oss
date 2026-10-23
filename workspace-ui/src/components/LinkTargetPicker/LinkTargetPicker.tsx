import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Radio, RadioGroup } from '@/components/ui/radio-group';
import { useCmsFolderPages, useCmsNodes, useCmsPage, useCmsSearch } from '@/hooks/useCmsQueries';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';

import styles from './LinkTargetPicker.module.css';

// How long the search text has to stay the same before the CMS is searched, as in `ReferenceMenu`.
const SEARCH_DELAY_MS = 250;

// The object type the page search asks for; one array, so the search's queries keep their keys.
const PAGE_TYPES = ['page'] as const;

/** A CMS page as a link target: its id, and its name once known. */
export interface LinkTargetPage {
    id: number;
    name?: string;
}

/** Where a link should lead: a URL as typed, or a CMS page (`null` while none is picked). */
export type LinkTargetValue = { type: 'url'; url: string } | { type: 'page'; page: LinkTargetPage | null };

export interface LinkTargetPickerProps {
    /** The CMS node whose pages are offered; without it only a URL can be given. */
    nodeId: number | undefined;
    /** The page the link is on; without a CMS page as the link's target, its folder's pages are listed first. */
    pageId: number;
    value: LinkTargetValue;
    onValueChange: (value: LinkTargetValue) => void;
}

// A page picked before whose name is not known yet, e.g. the target the link has now: its name, read from the CMS (`useCmsPage`).
function PageName({ page, nodeId }: { page: LinkTargetPage; nodeId: number }) {
    const { t } = useTranslation();
    const loaded = useCmsPage(page.id, nodeId);

    return <>{loaded.data?.name || t('preview.link.currentPage', { id: page.id })}</>;
}

/**
 * Picks the target of a link (the preview's link card): a URL typed into a field, or a page of the
 * node `nodeId`, chosen from a list with a search field above it. Without a search text the list holds
 * the pages of one folder (`useCmsFolderPages`): that of the CMS page the link pointed to when the
 * picker opened, else that of the page `pageId` the link is on (both read with `useCmsPage`), and the
 * node's root folder when that page cannot be read. A search text searches the whole node
 * (`useCmsSearch`). The picked page stays on top of the list while it is not among the results, by
 * its name, read from the CMS when only its id is known. Switching between URL and page keeps what was
 * given for each. Loading, no match and a failed search each say so in one line.
 */
export function LinkTargetPicker({ nodeId, pageId, value, onValueChange }: LinkTargetPickerProps) {
    const { t } = useTranslation();
    const urlId = useId();
    const searchId = useId();
    // What was given for the kind not shown, to bring back when switching to it again.
    const [lastUrl, setLastUrl] = useState(value.type === 'url' ? value.url : '');
    const [lastPage, setLastPage] = useState(value.type === 'page' ? value.page : null);
    const [search, setSearch] = useState('');
    const query = useDebouncedValue(search.trim(), SEARCH_DELAY_MS);
    const nodes = useCmsNodes();
    const node = nodeId === undefined ? undefined : nodes.data?.find(({ id }) => id === nodeId);
    // The page whose folder is listed without a search text: the link's target as the picker opened, else the page the link is on.
    const [folderPageId] = useState(value.type === 'page' && value.page ? value.page.id : pageId);
    const folderPage = useCmsPage(folderPageId, nodeId);
    const folderId = folderPage.data?.folderId ?? (folderPage.isError ? node?.folderId : undefined);
    const folderPages = useCmsFolderPages(node, query === '' ? folderId : undefined);
    const searchedPages = useCmsSearch(node && query !== '' ? [node] : [], query, PAGE_TYPES);
    const items = (query === '' ? folderPages.data : searchedPages.results[0]?.items) ?? [];
    const isLoading = query === '' ? folderPages.isPending : searchedPages.isLoading;
    const isFailed = nodes.isError || (query === '' ? folderPages.isError : searchedPages.isError) || (nodes.isSuccess && nodeId !== undefined && !node);
    const status = isFailed
        ? t('preview.link.failed')
        : (isLoading || !node ? t('preview.link.loading') : (items.length === 0 ? t('preview.link.empty') : ''));
    const picked = value.type === 'page' ? value.page : null;
    const isPickedListed = picked !== null && items.some((item) => item.id === picked.id);

    function switchTo(type: unknown) {
        if (type === 'url' && value.type === 'page') {
            setLastPage(value.page);
            onValueChange({ type: 'url', url: lastUrl });
        } else if (type === 'page' && value.type === 'url') {
            setLastUrl(value.url);
            onValueChange({ type: 'page', page: lastPage });
        }
    }

    return (
        <div className={styles.picker}>
            <RadioGroup aria-label={t('preview.link.target')} value={value.type} onValueChange={switchTo}>
                <div className={styles.kinds}>
                    <label className={styles.kind}>
                        <Radio value="url" />
                        {t('preview.link.url')}
                    </label>
                    {nodeId !== undefined && (
                        <label className={styles.kind}>
                            <Radio value="page" />
                            {t('preview.link.page')}
                        </label>
                    )}
                </div>
            </RadioGroup>
            {value.type === 'url' && (
                <>
                    <Label htmlFor={urlId}>{t('preview.link.url')}</Label>
                    <Input id={urlId} type="url" value={value.url} onChange={(event) => onValueChange({ type: 'url', url: event.target.value })} />
                </>
            )}
            {value.type === 'page' && nodeId !== undefined && (
                <>
                    <Label htmlFor={searchId}>{t('preview.link.search')}</Label>
                    <Input id={searchId} type="search" placeholder={t('preview.link.searchPlaceholder')} value={search} onChange={(event) => setSearch(event.target.value)} />
                    <RadioGroup
                        aria-label={t('preview.link.page')}
                        value={picked ? String(picked.id) : null}
                        onValueChange={(id) => {
                            const item = items.find((found) => String(found.id) === String(id));

                            if (item) {
                                onValueChange({ type: 'page', page: { id: item.id, name: item.name } });
                            }
                        }}
                    >
                        <div className={styles.pages}>
                            {picked && !isPickedListed && (
                                <label className={styles.page}>
                                    <Radio value={String(picked.id)} />
                                    <span className={styles.pageName}>{picked.name ?? <PageName page={picked} nodeId={nodeId} />}</span>
                                </label>
                            )}
                            {!status && items.map((item) => (
                                <label key={item.id} className={styles.page}>
                                    <Radio value={String(item.id)} />
                                    <span className={styles.pageName}>{item.name}</span>
                                    {item.path && <span className={styles.pagePath}>{item.path}</span>}
                                </label>
                            ))}
                        </div>
                    </RadioGroup>
                    {status && <p className={styles.status} role="status">{status}</p>}
                </>
            )}
        </div>
    );
}
