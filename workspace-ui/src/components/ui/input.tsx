import { Input as InputPrimitive } from '@base-ui/react/input';

import { cn } from './utils';

/**
 * Text field (design.md §12 "Eingabefelder": `--bg-surface`, `--border-field`, `--corner-md`, 32 px;
 * §7 6 × 8 px; §8 focus as a text field, invalid, disabled). Needs a visible `Label` above it (§4.2).
 */
function Input({ className, ...props }: InputPrimitive.Props) {
    return (
        <InputPrimitive
            data-slot="input"
            className={cn(
                'h-32 w-full min-w-0 rounded-md border border-line-field bg-surface px-8 py-6 text-base leading-normal text-ink transition-colors outline-none placeholder:text-muted',
                'focus-visible:border-azure focus-visible:shadow-[0_0_0_3px_rgba(var(--interactive-rgb),.16)]',
                'aria-invalid:border-err/55 aria-invalid:bg-errbg data-invalid:border-err/55 data-invalid:bg-errbg',
                'data-disabled:cursor-not-allowed data-disabled:opacity-42 disabled:cursor-not-allowed disabled:opacity-42',
                className,
            )}
            {...props}
        />
    );
}

export { Input };
