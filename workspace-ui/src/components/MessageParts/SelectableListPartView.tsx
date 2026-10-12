import { ListChecksIcon } from 'lucide-react';
import { useContext, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { EditTheseButton } from '@/components/EditTheseButton/EditTheseButton';
import { HandOffContext } from '@/components/EditTheseButton/handOffContext';
import type { SelectableListPart } from '@/services/apiService/genaix/types';

import { PartCard } from './PartCard';
import type { PartViewProps } from './partProps';
import { SelectableList } from './SelectableList';

import styles from './MessageParts.module.css';

/**
 * `selectable_list`: items to pick, several when `multi`. The selection starts from `selected` and
 * stays in this card; when the list accompanies a `choice` interaction, the interaction card answers it.
 * Otherwise, in a read-only session (`HandOffContext`), the objects of the selected items can be
 * handed off to a new session ("Edit these").
 */
export function SelectableListPartView({ part, sessionId }: PartViewProps<SelectableListPart>) {
    const { t } = useTranslation();
    const [value, setValue] = useState<string[]>(() => part.selected ?? []);
    const title = part.label || t('parts.list.title');
    const items = part.items ?? [];
    const canHandOff = useContext(HandOffContext) !== null && !part.interaction_id && items.some((item) => item.ref);
    const count = t('parts.selectedCount', { count: value.length });
    const footer = canHandOff
        ? (
            <>
                <span className={styles.caption}>{count}</span>
                <EditTheseButton sessionId={sessionId} references={items.flatMap((item) => (item.ref && value.includes(item.id) ? [item.ref] : []))} />
            </>
        )
        : count;

    return (
        <PartCard icon={ListChecksIcon} title={title} footer={footer}>
            <SelectableList items={items} multi={part.multi ?? false} value={value} onChange={setValue} label={title} />
        </PartCard>
    );
}
