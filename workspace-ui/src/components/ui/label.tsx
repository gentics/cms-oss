import type { ComponentProps } from 'react';

import { cn } from './utils';

/** Visible field label (design.md §4.2, §5.1: 12 px, 500, `--fg-primary`, above the field). */
function Label({ className, ...props }: ComponentProps<'label'>) {
    return (
        <label
            data-slot="label"
            className={cn('inline-flex items-center gap-8 text-sm leading-normal font-medium text-ink select-none', className)}
            {...props}
        />
    );
}

export { Label };
