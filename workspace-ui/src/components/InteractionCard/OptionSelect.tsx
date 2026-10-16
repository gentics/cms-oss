import { useTranslation } from 'react-i18next';

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import type { ContextReference } from '@/services/apiService/genaix/types';

import { withPath } from './withPath';

import styles from './InteractionCard.module.css';

export interface Option {
    value: string;
    label: string;
    /** The CMS object the option names (`settings_review`), its `path` telling same-named folders apart. */
    ref?: ContextReference;
    /** Why GenAIx offers the option (`settings_review`). */
    reason?: string;
}

/**
 * A layer `Select` over `options`, labelled by `id`'s `<label>`. Each option shows its path when it has
 * one, and in the list also why it is offered. `placeholder` while nothing is chosen, "Choose…" by default.
 * As wide as the card's text fields. With `defaultOpen` its list is open from the start; `onClose` when it
 * closes, chosen or not.
 */
export function OptionSelect({ id, options, value, onChange, disabled, placeholder, defaultOpen = false, onClose }: {
    id: string;
    options: Option[];
    value: string;
    onChange: (value: string) => void;
    disabled: boolean;
    placeholder?: string;
    defaultOpen?: boolean;
    onClose?: () => void;
}) {
    const { t } = useTranslation();
    const items = Object.fromEntries(options.map((option) => [option.value, withPath(option.label, option.ref)]));

    return (
        <Select
            items={items}
            value={value || null}
            onValueChange={(next) => onChange(next === null ? '' : String(next))}
            disabled={disabled}
            defaultOpen={defaultOpen}
            onOpenChange={(open) => {
                if (!open) {
                    onClose?.();
                }
            }}
        >
            <SelectTrigger id={id} fullWidth>
                <SelectValue placeholder={placeholder ?? t('chat.interaction.choose')} />
            </SelectTrigger>
            <SelectContent>
                {options.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                        {withPath(option.label, option.ref)}
                        {option.reason && <span className={styles.optionReason}>{option.reason}</span>}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    );
}
