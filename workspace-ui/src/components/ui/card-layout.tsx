import type { ComponentProps } from 'react';

import { cn } from './utils';

// The layout every edit card is built on (the preview's feedback, image and link cards, the inline editor's link
// card), inside a popover's card (`PopoverContent`, which is the container): head, body, foot.
// design.md §12 "Popover / Menü", §6.2 lines in `--border-subtle`, §7 paddings; the foot on `--bg-subtle`.
// Layout only: title and close button stay the popover's (`PopoverTitle`, `PopoverClose`), so it keeps its name.

/** The head: an icon (a direct `svg`, in `--interactive`), the title (`PopoverTitle`, takes the room) and a close button. */
function CardLayoutHeader({ className, ...props }: ComponentProps<'div'>) {
    return (
        <div
            data-slot="card-layout-header"
            className={cn(
                'flex items-center gap-8 border-b border-line py-8 pr-8 pl-12',
                '[&>svg]:flex-none [&>svg]:text-azure [&>[data-slot=popover-title]]:min-w-0 [&>[data-slot=popover-title]]:flex-1',
                className,
            )}
            {...props}
        />
    );
}

/** The body: the fields, one below the other. */
function CardLayoutBody({ className, ...props }: ComponentProps<'div'>) {
    return <div data-slot="card-layout-body" className={cn('flex flex-col items-start gap-8 p-12', className)} {...props} />;
}

/** The foot: the actions, at the end. */
function CardLayoutFooter({ className, ...props }: ComponentProps<'div'>) {
    return <div data-slot="card-layout-footer" className={cn('flex justify-end gap-8 border-t border-line bg-tint px-12 py-8', className)} {...props} />;
}

export { CardLayoutBody, CardLayoutFooter, CardLayoutHeader };
