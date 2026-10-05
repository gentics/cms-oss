import { Select as SelectPrimitive } from '@base-ui/react/select';
import { CheckIcon } from 'lucide-react';

import { cn } from './utils';

// design.md §12 "Eingabefelder": surface, `--line-field` border, `--r-md`, 32 px high, own chevron
// (two 5 px triangles). Focus like a text field (§8): `--azure` border + halo. Invalid: §3.3.

/** Select. Pass `items` (value → label) so the trigger shows the label of the chosen value. */
function Select<Value, Multiple extends boolean | undefined = false>(props: SelectPrimitive.Root.Props<Value, Multiple>) {
    return <SelectPrimitive.Root {...props} />;
}

function SelectTrigger({ className, children, ...props }: SelectPrimitive.Trigger.Props) {
    return (
        <SelectPrimitive.Trigger
            data-slot="select-trigger"
            className={cn(
                'inline-flex h-32 min-w-[160px] cursor-pointer items-center justify-between gap-8 rounded-md border border-line-field bg-surface px-8 py-6 text-base leading-normal text-ink transition-colors',
                'focus-visible:border-azure focus-visible:shadow-[0_0_0_3px_rgba(var(--az-rgb),.16)] data-popup-open:border-azure',
                'aria-invalid:border-err/55 aria-invalid:bg-errbg data-invalid:border-err/55 data-invalid:bg-errbg',
                'data-disabled:cursor-not-allowed data-disabled:opacity-42',
                className,
            )}
            {...props}
        >
            {children}
            <SelectPrimitive.Icon className="text-slate">
                <svg width="10" height="14" viewBox="0 0 10 14" fill="currentColor" aria-hidden="true">
                    <path d="M5 1 9.5 5.5h-9z" />
                    <path d="M5 13 .5 8.5h9z" />
                </svg>
            </SelectPrimitive.Icon>
        </SelectPrimitive.Trigger>
    );
}

function SelectValue({ className, ...props }: SelectPrimitive.Value.Props) {
    return (
        <SelectPrimitive.Value
            data-slot="select-value"
            className={cn('truncate text-left data-placeholder:text-muted', className)}
            {...props}
        />
    );
}

function SelectContent({ className, children, ...props }: SelectPrimitive.Popup.Props) {
    return (
        <SelectPrimitive.Portal>
            <SelectPrimitive.Positioner sideOffset={6} alignItemWithTrigger={false} className="z-50 outline-none">
                <SelectPrimitive.Popup
                    data-slot="select-content"
                    className={cn(
                        'max-h-(--available-height) min-w-(--anchor-width) origin-(--transform-origin) overflow-y-auto rounded-xl border border-line2 bg-surface p-4 text-base leading-normal text-ink shadow-pop outline-none',
                        'transition-[opacity,scale,translate] duration-(--dur-pop) data-ending-style:opacity-0 data-starting-style:translate-y-5 data-starting-style:scale-[.985] data-starting-style:opacity-0',
                        className,
                    )}
                    {...props}
                >
                    <SelectPrimitive.List>{children}</SelectPrimitive.List>
                </SelectPrimitive.Popup>
            </SelectPrimitive.Positioner>
        </SelectPrimitive.Portal>
    );
}

function SelectItem({ className, children, ...props }: SelectPrimitive.Item.Props) {
    return (
        <SelectPrimitive.Item
            data-slot="select-item"
            className={cn(
                'relative flex cursor-default items-center gap-8 rounded-md py-6 pr-32 pl-8 outline-none select-none',
                'data-highlighted:bg-tint focus-visible:bg-azure/15 focus-visible:shadow-[inset_0_0_0_1px_rgba(var(--az-rgb),.55)]',
                'data-selected:font-medium data-disabled:cursor-not-allowed data-disabled:opacity-42',
                className,
            )}
            {...props}
        >
            <SelectPrimitive.ItemText>{children}</SelectPrimitive.ItemText>
            <SelectPrimitive.ItemIndicator className="absolute right-8 flex items-center text-azure">
                <CheckIcon size={16} />
            </SelectPrimitive.ItemIndicator>
        </SelectPrimitive.Item>
    );
}

export { Select, SelectContent, SelectItem, SelectTrigger, SelectValue };
