import { Tooltip as TooltipPrimitive } from '@base-ui/react/tooltip';

import { cn } from './utils';

// design.md has no tooltip entry; per §1.2 it uses the nearest tokens: a dark surface like the
// toast (`--bg-solid`, white text, §4: 9.13:1), `--corner-md`, `--elevation-pop`, `pop` motion (§10).

function TooltipProvider({ delay = 400, ...props }: TooltipPrimitive.Provider.Props) {
    return <TooltipPrimitive.Provider delay={delay} {...props} />;
}

function Tooltip(props: TooltipPrimitive.Root.Props) {
    return <TooltipPrimitive.Root data-slot="tooltip" {...props} />;
}

function TooltipTrigger(props: TooltipPrimitive.Trigger.Props) {
    return <TooltipPrimitive.Trigger data-slot="tooltip-trigger" {...props} />;
}

function TooltipContent({
    className,
    side = 'top',
    sideOffset = 6,
    align = 'center',
    ...props
}: TooltipPrimitive.Popup.Props & Pick<TooltipPrimitive.Positioner.Props, 'align' | 'side' | 'sideOffset'>) {
    return (
        <TooltipPrimitive.Portal>
            <TooltipPrimitive.Positioner side={side} sideOffset={sideOffset} align={align} className="z-50">
                <TooltipPrimitive.Popup
                    data-slot="tooltip-content"
                    className={cn(
                        'max-w-[280px] origin-(--transform-origin) rounded-md bg-solid px-8 py-4 text-sm leading-normal text-white shadow-pop',
                        'transition-[opacity,scale] duration-(--duration-popover) data-ending-style:opacity-0 data-starting-style:scale-[.985] data-starting-style:opacity-0',
                        className,
                    )}
                    {...props}
                />
            </TooltipPrimitive.Positioner>
        </TooltipPrimitive.Portal>
    );
}

export { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger };
