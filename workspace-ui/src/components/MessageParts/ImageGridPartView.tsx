import { ImagesIcon } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Checkbox } from '@/components/ui/checkbox';
import { Radio, RadioGroup } from '@/components/ui/radio-group';
import type { ImageGridPart } from '@/services/apiService/genaix/types';

import { refText } from './contextReference';
import { PartCard } from './PartCard';
import type { PartViewProps } from './partProps';

import styles from './MessageParts.module.css';

/**
 * `image_grid` (design.md §12 "Bildraster"): three columns of 16:10 tiles; a selected tile has the
 * 2 px `--interactive` border. One image or several (`multi`); the selection starts from the items'
 * `selected` and stays in this card.
 */
export function ImageGridPartView({ part }: PartViewProps<ImageGridPart>) {
    const { t } = useTranslation();
    const items = part.items ?? [];
    const multi = part.multi ?? false;
    const [value, setValue] = useState<string[]>(() => items.filter((item) => item.selected).map((item) => item.id));
    const title = part.label || t('parts.images.title');

    const tiles = items.map((item, index) => {
        const name = item.alt || (item.ref && refText(item.ref)) || t('parts.images.image', { number: index + 1 });
        const isSelected = value.includes(item.id);

        return (
            <label key={item.id} className={styles.tile} data-selected={isSelected || undefined}>
                <img src={item.url} alt={name} loading="lazy" />
                {/* Named by the tile's label, that is the image's `alt`. */}
                <span className={styles.tileControl}>
                    {multi
                        ? (
                            <Checkbox
                                checked={isSelected}
                                onCheckedChange={(checked) => setValue((current) => (checked ? [...current, item.id] : current.filter((id) => id !== item.id)))}
                            />
                        )
                        : <Radio value={item.id} />}
                </span>
            </label>
        );
    });

    return (
        <PartCard icon={ImagesIcon} title={title} footer={t('parts.selectedCount', { count: value.length })}>
            {multi
                ? <div role="group" aria-label={title} className={styles.grid}>{tiles}</div>
                : (
                    <RadioGroup aria-label={title} value={value[0] ?? null} onValueChange={(id) => setValue([String(id)])}>
                        <div className={styles.grid}>{tiles}</div>
                    </RadioGroup>
                )}
        </PartCard>
    );
}
