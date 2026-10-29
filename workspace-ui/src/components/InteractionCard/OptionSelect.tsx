import { SparklesIcon } from 'lucide-react';
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

/** The option GenAIx recommends (`settings_review` `missing[].recommended`) and why. */
export interface Recommendation {
    value: string;
    reason?: string;
}

// "Recommended", its reason the tooltip (design.md §12 "Badge", sparkles an icon with meaning, §9 "KI").
function RecommendedBadge({ reason }: { reason?: string }) {
    const { t } = useTranslation();

    return (
        <span className={styles.recommended} title={reason}>
            <SparklesIcon size={11} aria-hidden />
            {t('chat.interaction.recommended')}
        </span>
    );
}

/**
 * A layer `Select` over `options`, labelled by `id`'s `<label>`. Each option shows its path when it has
 * one, and in the list also why it is offered. `placeholder` while nothing is chosen, "Choose…" by default.
 * As wide as the card's text fields. The `recommended` option has "Recommended" beside it in the list, and
 * in the field while it is the one chosen.
 */
export function OptionSelect({ id, options, value, onChange, disabled, placeholder, recommended }: {
    id: string;
    options: Option[];
    value: string;
    onChange: (value: string) => void;
    disabled: boolean;
    placeholder?: string;
    recommended?: Recommendation;
}) {
    const { t } = useTranslation();
    const items = Object.fromEntries(options.map((option) => [option.value, withPath(option.label, option.ref)]));
    const placeholderText = placeholder ?? t('chat.interaction.choose');

    return (
        <Select items={items} value={value || null} onValueChange={(next) => onChange(next === null ? '' : String(next))} disabled={disabled}>
            <SelectTrigger id={id} fullWidth>
                {recommended
                    ? (
                        // A render function replaces the placeholder (Base UI `Select.Value`), so it shows that too.
                        <SelectValue>
                            {(selected: unknown) => {
                                if (selected === null || selected === undefined || selected === '') {
                                    return placeholderText;
                                }

                                return (
                                    <span className={styles.selectedValue}>
                                        <span className={styles.selectedLabel}>{items[String(selected)] ?? String(selected)}</span>
                                        {String(selected) === recommended.value && <RecommendedBadge reason={recommended.reason} />}
                                    </span>
                                );
                            }}
                        </SelectValue>
                    )
                    : <SelectValue placeholder={placeholderText} />}
            </SelectTrigger>
            <SelectContent>
                {options.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                        {withPath(option.label, option.ref)}
                        {option.value === recommended?.value && <RecommendedBadge reason={recommended.reason} />}
                        {option.reason && <span className={styles.optionReason}>{option.reason}</span>}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    );
}
