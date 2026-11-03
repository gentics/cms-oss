import { Radio as RadioPrimitive } from '@base-ui/react/radio';
import { RadioGroup as RadioGroupPrimitive } from '@base-ui/react/radio-group';

import { cn } from './utils';

/** A group of `Radio`s, one of which is selected. Give it an `aria-label` or `aria-labelledby`. */
function RadioGroup({ className, ...props }: RadioGroupPrimitive.Props) {
    return <RadioGroupPrimitive data-slot="radio-group" className={cn('flex flex-col gap-2', className)} {...props} />;
}

/**
 * Radio (design.md §12 "Checkbox / Radio": 16 px, 1.5 px `--border-strong`; on: a 4 px ring in
 * `--interactive`). Put it inside a `<Label>` or give it an `aria-label`, so it has an accessible name.
 */
function Radio({ className, ...props }: RadioPrimitive.Root.Props) {
    return (
        <RadioPrimitive.Root
            data-slot="radio"
            className={cn(
                'inline-flex size-16 shrink-0 cursor-pointer rounded-full border-[1.5px] border-line2 bg-surface transition-colors',
                'data-checked:border-4 data-checked:border-azure',
                'data-disabled:cursor-not-allowed data-disabled:opacity-42',
                className,
            )}
            {...props}
        />
    );
}

export { Radio, RadioGroup };
