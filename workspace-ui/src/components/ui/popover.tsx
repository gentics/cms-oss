import { Popover as PopoverPrimitive } from '@base-ui/react/popover';

import { cn } from './utils';

// design.md §12 "Popover / Menü": `--bg-surface`, `--border-strong`, `--corner-xl`, `--elevation-pop`;
// §11 Popover 350–410 px; motion `pop` (§10). Not modal: the page around it stays usable.

function Popover(props: PopoverPrimitive.Root.Props) {
    return <PopoverPrimitive.Root data-slot="popover" {...props} />;
}

function PopoverTrigger(props: PopoverPrimitive.Trigger.Props) {
    return <PopoverPrimitive.Trigger data-slot="popover-trigger" {...props} />;
}

/**
 * The popover's card, at its trigger, or at `anchor` (an element or a ref) for a popover without a
 * trigger, opened through `Popover`'s `open`.
 */
function PopoverContent({
    className,
    side = 'bottom',
    sideOffset = 6,
    align = 'start',
    anchor,
    ...props
}: PopoverPrimitive.Popup.Props & Pick<PopoverPrimitive.Positioner.Props, 'align' | 'side' | 'sideOffset' | 'anchor'>) {
    return (
        <PopoverPrimitive.Portal>
            <PopoverPrimitive.Positioner side={side} sideOffset={sideOffset} align={align} anchor={anchor} className="z-50 outline-none">
                <PopoverPrimitive.Popup
                    data-slot="popover-content"
                    className={cn(
                        'w-[380px] max-w-[calc(100vw-20px)] origin-(--transform-origin) overflow-hidden rounded-xl border border-line2 bg-surface text-base leading-normal text-ink shadow-pop outline-none',
                        'transition-[opacity,scale,translate] duration-(--duration-popover) data-ending-style:opacity-0 data-starting-style:translate-y-5 data-starting-style:scale-[.985] data-starting-style:opacity-0',
                        className,
                    )}
                    {...props}
                />
            </PopoverPrimitive.Positioner>
        </PopoverPrimitive.Portal>
    );
}

function PopoverTitle({ className, ...props }: PopoverPrimitive.Title.Props) {
    return <PopoverPrimitive.Title data-slot="popover-title" className={cn('m-0 text-base leading-normal font-medium text-ink', className)} {...props} />;
}

function PopoverClose(props: PopoverPrimitive.Close.Props) {
    return <PopoverPrimitive.Close data-slot="popover-close" {...props} />;
}

export { Popover, PopoverClose, PopoverContent, PopoverTitle, PopoverTrigger };
