import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Radio, RadioGroup } from '@/components/ui/radio-group';
import type { ContextReference } from '@/services/apiService/genaix/types';

import { RefToken } from './RefToken';

import styles from './MessageParts.module.css';

export interface SelectableItem {
    id: string;
    label: string;
    description?: string;
    /** Why the item is offered (a `choice` option's `reason`, rc.5 addition). */
    reason?: string;
    /** A badge after the label, such as "Recommended". */
    badge?: string;
    ref?: ContextReference;
}

/** A last row for the user's own text, with its field (a `choice` with `other_allowed`, rc.5 addition). */
export interface SelectableOther {
    label: string;
    /** The accessible name of the field. */
    fieldLabel: string;
    placeholder?: string;
    isSelected: boolean;
    text: string;
    onSelect: (isSelected: boolean) => void;
    onTextChange: (text: string) => void;
    disabled?: boolean;
}

interface SelectableListProps {
    items: SelectableItem[];
    /** Several items (checkboxes) or exactly one (radios). */
    multi: boolean;
    value: string[];
    onChange: (value: string[]) => void;
    /** The accessible name of the list. */
    label: string;
    other?: SelectableOther;
}

// The radio value of the `other` row in a single choice; item ids come from GenAIx and never start with NUL.
const OTHER_VALUE = '\u0000other';

/**
 * Rows to pick from (draft `.selrow`): a `selectable_list` part and the options of a `choice`
 * interaction. Controlled: `value` holds the ids of the selected items. With `other`, a last row takes
 * the user's own text; typing into its field selects it, and in a single choice it is one of the radios.
 */
export function SelectableList({ items, multi, value, onChange, label, other }: SelectableListProps) {
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
                <span className={styles.rowLabel}>
                    {item.label}
                    {item.badge && <span className={styles.rowBadge}>{item.badge}</span>}
                </span>
                {item.description && <span className={styles.rowDescription}>{item.description}</span>}
                {item.reason && <span className={styles.rowDescription}>{item.reason}</span>}
            </span>
            {item.ref && <RefToken value={item.ref} />}
        </label>
    ));

    const otherRow = other && (
        <div className={styles.otherRow}>
            <label className={styles.row}>
                {multi
                    ? <Checkbox checked={other.isSelected} onCheckedChange={other.onSelect} disabled={other.disabled} />
                    : <Radio value={OTHER_VALUE} disabled={other.disabled} />}
                <span className={styles.rowText}>
                    <span className={styles.rowLabel}>{other.label}</span>
                </span>
            </label>
            <div className={styles.otherField}>
                <Input
                    aria-label={other.fieldLabel}
                    placeholder={other.placeholder}
                    value={other.text}
                    maxLength={4000}
                    disabled={other.disabled}
                    onChange={(event) => {
                        other.onTextChange(event.target.value);

                        if (!other.isSelected) {
                            other.onSelect(true);
                        }
                    }}
                />
            </div>
        </div>
    );

    if (multi) {
        return <div role="group" aria-label={label} className={styles.rows}>{rows}{otherRow}</div>;
    }

    function select(id: unknown) {
        if (id === OTHER_VALUE) {
            other?.onSelect(true);

            return;
        }

        other?.onSelect(false);
        onChange([String(id)]);
    }

    return (
        <RadioGroup aria-label={label} value={other?.isSelected ? OTHER_VALUE : value[0] ?? null} onValueChange={select}>
            <div className={styles.rows}>{rows}{otherRow}</div>
        </RadioGroup>
    );
}
