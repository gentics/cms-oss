import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Radio, RadioGroup } from '@/components/ui/radio-group';
import { useCmsImages, useCmsNodes } from '@/hooks/useCmsQueries';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { cmsImageUrl, type CmsItem } from '@/services/cmsApiService/cmsApiService';

import styles from './ImagePicker.module.css';

// How long the search text has to stay the same before the CMS is searched, as in `ReferenceMenu`.
const SEARCH_DELAY_MS = 250;

export interface ImagePickerProps {
    /** The CMS node whose images are offered. */
    nodeId: number;
    /** The picked image, if any. */
    value: CmsItem | null;
    onValueChange: (image: CmsItem) => void;
}

/**
 * Picks one image of a CMS node: a search field over a grid of the node's images (design.md §12
 * "Bildraster"), each shown as the CMS serves it (`cmsImageUrl`). Without a search text the grid
 * lists the node's images; typing searches their names (`useCmsImages`). Loading, no match and a
 * failed search each say so in one line.
 */
export function ImagePicker({ nodeId, value, onValueChange }: ImagePickerProps) {
    const { t } = useTranslation();
    const searchId = useId();
    const [search, setSearch] = useState('');
    const query = useDebouncedValue(search.trim(), SEARCH_DELAY_MS);
    const nodes = useCmsNodes();
    const node = nodes.data?.find(({ id }) => id === nodeId);
    const images = useCmsImages(node, query);
    const items = images.data ?? [];
    const isFailed = nodes.isError || images.isError || (nodes.isSuccess && !node);
    const status = isFailed
        ? t('preview.image.failed')
        : (images.isPending ? t('preview.image.loading') : (items.length === 0 ? t('preview.image.empty') : ''));

    return (
        <div className={styles.picker}>
            <Label htmlFor={searchId}>{t('preview.image.search')}</Label>
            <Input id={searchId} type="search" value={search} onChange={(event) => setSearch(event.target.value)} />
            {status
                ? <p className={styles.status} role="status">{status}</p>
                : (
                    <RadioGroup
                        aria-label={t('preview.image.search')}
                        value={value ? String(value.id) : null}
                        onValueChange={(id) => {
                            const image = items.find((item) => String(item.id) === String(id));

                            if (image) {
                                onValueChange(image);
                            }
                        }}
                    >
                        <div className={styles.grid}>
                            {items.map((item) => (
                                <label key={item.id} className={styles.tile} data-selected={value?.id === item.id || undefined}>
                                    <img src={cmsImageUrl(nodeId, item.id)} alt={item.name} loading="lazy" />
                                    {/* Named by the tile's label, that is the image's file name. */}
                                    <span className={styles.tileControl}>
                                        <Radio value={String(item.id)} />
                                    </span>
                                </label>
                            ))}
                        </div>
                    </RadioGroup>
                )}
        </div>
    );
}
