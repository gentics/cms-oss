import { Checkbox as CheckboxPrimitive } from '@base-ui/react/checkbox';
import { CheckIcon, MinusIcon } from 'lucide-react';

import { cn } from './utils';

/**
 * Checkbox (design.md §12 "Checkbox / Radio"). Put it inside a `<Label>` or give it an
 * `aria-label`, so it has an accessible name.
 */
function Checkbox({ className, indeterminate, ...props }: CheckboxPrimitive.Root.Props) {
    return (
        <CheckboxPrimitive.Root
            data-slot="checkbox"
            indeterminate={indeterminate}
            className={cn(
                'inline-flex size-16 shrink-0 cursor-pointer items-center justify-center rounded-xs border-[1.5px] border-line2 bg-surface text-white transition-colors',
                'data-checked:border-akzent data-checked:bg-akzent data-indeterminate:border-akzent data-indeterminate:bg-akzent',
                'aria-invalid:border-err/55 aria-invalid:bg-errbg data-invalid:border-err/55 data-invalid:bg-errbg',
                'data-disabled:cursor-not-allowed data-disabled:opacity-42',
                className,
            )}
            {...props}
        >
            <CheckboxPrimitive.Indicator data-slot="checkbox-indicator" className="flex items-center justify-center">
                {indeterminate ? <MinusIcon size={12} /> : <CheckIcon size={12} />}
            </CheckboxPrimitive.Indicator>
        </CheckboxPrimitive.Root>
    );
}

export { Checkbox };
