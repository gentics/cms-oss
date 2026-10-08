import { Checkbox } from '@/components/ui/checkbox';
import { Radio, RadioGroup } from '@/components/ui/radio-group';
import type { ContextReference } from '@/services/apiService/genaix/types';

import { RefToken } from './RefToken';

import styles from './MessageParts.module.css';

export interface SelectableItem {
    id: string;
    label: string;
    description?: string;
    ref?: ContextReference;
}

interface SelectableListProps {
    items: SelectableItem[];
    /** Several items (checkboxes) or exactly one (radios). */
    multi: boolean;
    value: string[];
    onChange: (value: string[]) => void;
    /** The accessible name of the list. */
    label: string;
}

/**
 * Rows to pick from (draft `.selrow`): a `selectable_list` part and the options of a `choice`
 * interaction. Controlled: `value` holds the ids of the selected items.
 */
export function SelectableList({ items, multi, value, onChange, label }: SelectableListProps) {
    const rows = items.map((item) => (
        <label key={item.id} className={styles.row}>
            {multi
                ? (
                    <Checkbox
                        checked={value.includes(item.id)}
                        onCheckedChange={(checked) => onChange(checked ? [...value, item.id] : value.filter((id) => id !== item.id))}
                    />
                )
                : <Radio value={item.id} />}
            <span className={styles.rowText}>
                <span className={styles.rowLabel}>{item.label}</span>
                {item.description && <span className={styles.rowDescription}>{item.description}</span>}
            </span>
            {item.ref && <RefToken value={item.ref} />}
        </label>
    ));

    if (multi) {
        return <div role="group" aria-label={label} className={styles.rows}>{rows}</div>;
    }

    return (
        <RadioGroup aria-label={label} value={value[0] ?? null} onValueChange={(id) => onChange([String(id)])}>
            <div className={styles.rows}>{rows}</div>
        </RadioGroup>
    );
}
