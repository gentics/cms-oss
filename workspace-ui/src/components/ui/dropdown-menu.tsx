import { Menu as MenuPrimitive } from '@base-ui/react/menu';
import { CheckIcon, ChevronRightIcon } from 'lucide-react';

import { cn } from './utils';

// design.md §12 "Popover / Menü": item hover `rgba(az,.07)`, keyboard selection `rgba(az,.15)` +
// inner ring `rgba(az,.55)`, group heads as overline, motion `pop` (§10).

const popupClassName = cn(
    'max-h-(--available-height) min-w-[180px] origin-(--transform-origin) overflow-y-auto rounded-xl border border-line2 bg-surface p-4 text-base leading-normal text-ink shadow-pop outline-none',
    'transition-[opacity,scale,translate] duration-(--dur-pop) data-ending-style:opacity-0 data-starting-style:translate-y-5 data-starting-style:scale-[.985] data-starting-style:opacity-0',
);

const itemClassName = cn(
    'relative flex cursor-default items-center gap-8 rounded-md px-8 py-6 outline-none select-none',
    'data-highlighted:bg-azure/7 focus-visible:bg-azure/15 focus-visible:shadow-[inset_0_0_0_1px_rgba(var(--az-rgb),.55)]',
    'data-disabled:cursor-not-allowed data-disabled:opacity-42',
    '[&_svg]:text-slate',
);

function DropdownMenu(props: MenuPrimitive.Root.Props) {
    return <MenuPrimitive.Root data-slot="dropdown-menu" {...props} />;
}

function DropdownMenuTrigger(props: MenuPrimitive.Trigger.Props) {
    return <MenuPrimitive.Trigger data-slot="dropdown-menu-trigger" {...props} />;
}

function DropdownMenuContent({
    className,
    side = 'bottom',
    sideOffset = 6,
    align = 'start',
    ...props
}: MenuPrimitive.Popup.Props & Pick<MenuPrimitive.Positioner.Props, 'align' | 'side' | 'sideOffset'>) {
    return (
        <MenuPrimitive.Portal>
            <MenuPrimitive.Positioner side={side} sideOffset={sideOffset} align={align} className="z-50 outline-none">
                <MenuPrimitive.Popup data-slot="dropdown-menu-content" className={cn(popupClassName, className)} {...props} />
            </MenuPrimitive.Positioner>
        </MenuPrimitive.Portal>
    );
}

function DropdownMenuGroup(props: MenuPrimitive.Group.Props) {
    return <MenuPrimitive.Group data-slot="dropdown-menu-group" {...props} />;
}

function DropdownMenuLabel({ className, ...props }: MenuPrimitive.GroupLabel.Props) {
    return (
        <MenuPrimitive.GroupLabel
            data-slot="dropdown-menu-label"
            className={cn('px-8 pt-6 pb-4 text-overline leading-normal font-medium tracking-overline text-muted uppercase', className)}
            {...props}
        />
    );
}

function DropdownMenuItem({
    className,
    variant = 'default',
    ...props
}: MenuPrimitive.Item.Props & { variant?: 'default' | 'danger' }) {
    return (
        <MenuPrimitive.Item
            data-slot="dropdown-menu-item"
            data-variant={variant}
            className={cn(
                itemClassName,
                variant === 'danger' && 'text-err data-highlighted:bg-errbg focus-visible:bg-errbg focus-visible:shadow-[inset_0_0_0_1px_var(--err)] [&_svg]:text-err',
                className,
            )}
            {...props}
        />
    );
}

function DropdownMenuCheckboxItem({ className, children, ...props }: MenuPrimitive.CheckboxItem.Props) {
    return (
        <MenuPrimitive.CheckboxItem data-slot="dropdown-menu-checkbox-item" className={cn(itemClassName, 'pr-32', className)} {...props}>
            {children}
            <MenuPrimitive.CheckboxItemIndicator className="absolute right-8 flex items-center">
                <CheckIcon size={16} className="text-azure" />
            </MenuPrimitive.CheckboxItemIndicator>
        </MenuPrimitive.CheckboxItem>
    );
}

function DropdownMenuRadioGroup(props: MenuPrimitive.RadioGroup.Props) {
    return <MenuPrimitive.RadioGroup data-slot="dropdown-menu-radio-group" {...props} />;
}

function DropdownMenuRadioItem({ className, children, ...props }: MenuPrimitive.RadioItem.Props) {
    return (
        <MenuPrimitive.RadioItem data-slot="dropdown-menu-radio-item" className={cn(itemClassName, 'pr-32', className)} {...props}>
            {children}
            <MenuPrimitive.RadioItemIndicator className="absolute right-8 flex items-center">
                <CheckIcon size={16} className="text-azure" />
            </MenuPrimitive.RadioItemIndicator>
        </MenuPrimitive.RadioItem>
    );
}

function DropdownMenuSub(props: MenuPrimitive.SubmenuRoot.Props) {
    return <MenuPrimitive.SubmenuRoot data-slot="dropdown-menu-sub" {...props} />;
}

function DropdownMenuSubTrigger({ className, children, ...props }: MenuPrimitive.SubmenuTrigger.Props) {
    return (
        <MenuPrimitive.SubmenuTrigger
            data-slot="dropdown-menu-sub-trigger"
            className={cn(itemClassName, 'data-popup-open:bg-azure/7', className)}
            {...props}
        >
            {children}
            <ChevronRightIcon size={16} className="ml-auto" />
        </MenuPrimitive.SubmenuTrigger>
    );
}

function DropdownMenuSubContent({ className, ...props }: MenuPrimitive.Popup.Props) {
    return (
        <MenuPrimitive.Portal>
            <MenuPrimitive.Positioner side="right" align="start" sideOffset={2} alignOffset={-5} className="z-50 outline-none">
                <MenuPrimitive.Popup data-slot="dropdown-menu-sub-content" className={cn(popupClassName, className)} {...props} />
            </MenuPrimitive.Positioner>
        </MenuPrimitive.Portal>
    );
}

function DropdownMenuSeparator({ className, ...props }: MenuPrimitive.Separator.Props) {
    return <MenuPrimitive.Separator data-slot="dropdown-menu-separator" className={cn('-mx-4 my-4 h-1 bg-line', className)} {...props} />;
}

export {
    DropdownMenu,
    DropdownMenuCheckboxItem,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuSeparator,
    DropdownMenuSub,
    DropdownMenuSubContent,
    DropdownMenuSubTrigger,
    DropdownMenuTrigger,
};
