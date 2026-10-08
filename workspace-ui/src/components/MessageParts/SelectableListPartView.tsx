import { ListChecksIcon } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import type { SelectableListPart } from '@/services/apiService/genaix/types';

import { PartCard } from './PartCard';
import type { PartViewProps } from './partProps';
import { SelectableList } from './SelectableList';

/**
 * `selectable_list`: items to pick, several when `multi`. The selection starts from `selected` and
 * stays in this card; when the list accompanies a `choice` interaction, the interaction card answers it.
 */
export function SelectableListPartView({ part }: PartViewProps<SelectableListPart>) {
    const { t } = useTranslation();
    const [value, setValue] = useState<string[]>(() => part.selected ?? []);
    const title = part.label || t('parts.list.title');

    return (
        <PartCard icon={ListChecksIcon} title={title} footer={t('parts.selectedCount', { count: value.length })}>
            <SelectableList items={part.items ?? []} multi={part.multi ?? false} value={value} onChange={setValue} label={title} />
        </PartCard>
    );
}
