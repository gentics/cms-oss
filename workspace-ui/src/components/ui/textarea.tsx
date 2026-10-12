import type { ComponentProps } from 'react';

import { cn } from './utils';

/**
 * Multi-line text field in the look of `Input` (design.md §12 "Eingabefelder": `--bg-surface`,
 * `--border-field`, `--corner-md`, focus as a text field; "Textarea vertikal skalierbar"). Needs a
 * visible `Label` above it (§4.2).
 */
function Textarea({ className, ...props }: ComponentProps<'textarea'>) {
    return (
        <textarea
            data-slot="textarea"
            className={cn(
                'block min-h-72 w-full min-w-0 resize-y rounded-md border border-line-field bg-surface px-8 py-6 text-base leading-normal text-ink transition-colors outline-none placeholder:text-muted',
                'focus-visible:border-azure focus-visible:shadow-[0_0_0_3px_rgba(var(--interactive-rgb),.16)]',
                'aria-invalid:border-err/55 aria-invalid:bg-errbg',
                'disabled:cursor-not-allowed disabled:opacity-42',
                className,
            )}
            {...props}
        />
    );
}

export { Textarea };
