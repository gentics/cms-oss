import { Drawer as DrawerPrimitive } from '@base-ui/react/drawer';
import { XIcon } from 'lucide-react';
import type { ComponentProps } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from './button';
import { cn } from './utils';

// design.md §12 "Dialog / Drawer", §11 (drawer from the left, 380 px), §10 (24 px from the left).

/** Drawer from the left. Swiping to the left closes it, like Escape and the close button. */
function Drawer(props: Omit<DrawerPrimitive.Root.Props, 'swipeDirection'>) {
    return <DrawerPrimitive.Root data-slot="drawer" swipeDirection="left" {...props} />;
}

function DrawerTrigger(props: DrawerPrimitive.Trigger.Props) {
    return <DrawerPrimitive.Trigger data-slot="drawer-trigger" {...props} />;
}

function DrawerClose(props: DrawerPrimitive.Close.Props) {
    return <DrawerPrimitive.Close data-slot="drawer-close" {...props} />;
}

function DrawerContent({
    className,
    children,
    showCloseButton = true,
    ...props
}: DrawerPrimitive.Popup.Props & { showCloseButton?: boolean }) {
    const { t } = useTranslation();

    return (
        <DrawerPrimitive.Portal>
            <DrawerPrimitive.Backdrop
                data-slot="drawer-overlay"
                className="fixed inset-0 z-50 bg-backdrop transition-opacity duration-(--dur-card) data-ending-style:opacity-0 data-starting-style:opacity-0"
            />
            <DrawerPrimitive.Viewport data-slot="drawer-viewport" className="fixed inset-0 z-50 pointer-events-none">
                <DrawerPrimitive.Popup
                    data-slot="drawer-content"
                    className={cn(
                        'pointer-events-auto fixed inset-y-0 left-0 flex w-[min(380px,calc(100vw-32px))] flex-col border-r border-line2 bg-surface text-base leading-normal text-ink shadow-drawer',
                        '[transform:translateX(var(--drawer-swipe-movement-x,0px))] transition-[opacity,translate] duration-(--dur-card) data-swiping:duration-0',
                        'data-ending-style:-translate-x-24 data-ending-style:opacity-0 data-starting-style:-translate-x-24 data-starting-style:opacity-0',
                        className,
                    )}
                    {...props}
                >
                    <DrawerPrimitive.Content data-slot="drawer-body" className="flex min-h-0 flex-1 flex-col">
                        {children}
                    </DrawerPrimitive.Content>
                    {showCloseButton && (
                        <DrawerPrimitive.Close
                            data-slot="drawer-close"
                            aria-label={t('ui.close')}
                            render={<Button variant="ghost" size="icon" className="absolute top-12 right-12 text-slate" />}
                        >
                            <XIcon size={18} />
                        </DrawerPrimitive.Close>
                    )}
                </DrawerPrimitive.Popup>
            </DrawerPrimitive.Viewport>
        </DrawerPrimitive.Portal>
    );
}

function DrawerHeader({ className, ...props }: ComponentProps<'div'>) {
    return (
        <div
            data-slot="drawer-header"
            className={cn('flex min-h-44 flex-col justify-center gap-4 border-b border-line px-16 py-12 pr-52', className)}
            {...props}
        />
    );
}

function DrawerBody({ className, ...props }: ComponentProps<'div'>) {
    return <div data-slot="drawer-main" className={cn('min-h-0 flex-1 overflow-y-auto p-16', className)} {...props} />;
}

function DrawerFooter({ className, ...props }: ComponentProps<'div'>) {
    return (
        <div
            data-slot="drawer-footer"
            className={cn('flex flex-wrap justify-end gap-8 border-t border-line bg-tint px-16 py-12', className)}
            {...props}
        />
    );
}

function DrawerTitle({ className, ...props }: DrawerPrimitive.Title.Props) {
    return <DrawerPrimitive.Title data-slot="drawer-title" className={cn('text-md leading-normal font-medium text-ink', className)} {...props} />;
}

function DrawerDescription({ className, ...props }: DrawerPrimitive.Description.Props) {
    return <DrawerPrimitive.Description data-slot="drawer-description" className={cn('text-sm leading-normal text-slate', className)} {...props} />;
}

export { Drawer, DrawerBody, DrawerClose, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle, DrawerTrigger };
