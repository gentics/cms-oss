import { Drawer as DrawerPrimitive } from '@base-ui/react/drawer';
import { XIcon } from 'lucide-react';
import type { ComponentProps } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from './button';
import { cn } from './utils';

// design.md §12 "Dialog / Drawer", §11 (drawer from the left, 380 px; the tasks from the right, 480 px), §10 (24 px in from its side).

/**
 * Drawer from the left, or with `side="right"` from the right. Swiping towards its side closes it, like
 * Escape and the close button.
 */
function Drawer({ side = 'left', ...props }: Omit<DrawerPrimitive.Root.Props, 'swipeDirection'> & { side?: 'left' | 'right' }) {
    return <DrawerPrimitive.Root data-slot="drawer" swipeDirection={side} {...props} />;
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
    size = 'default',
    ...props
}: DrawerPrimitive.Popup.Props & { showCloseButton?: boolean; size?: 'default' | 'wide' }) {
    const { t } = useTranslation();

    return (
        <DrawerPrimitive.Portal>
            <DrawerPrimitive.Backdrop
                data-slot="drawer-overlay"
                className="fixed inset-0 z-50 bg-backdrop transition-opacity duration-(--duration-card) data-ending-style:opacity-0 data-starting-style:opacity-0"
            />
            <DrawerPrimitive.Viewport data-slot="drawer-viewport" className="fixed inset-0 z-50 pointer-events-none">
                <DrawerPrimitive.Popup
                    data-slot="drawer-content"
                    className={cn(
                        'pointer-events-auto fixed inset-y-0 left-0 flex flex-col border-r border-line2 bg-surface text-base leading-normal text-ink shadow-drawer',
                        size === 'wide' ? 'w-[min(480px,calc(100vw-32px))]' : 'w-[min(380px,calc(100vw-32px))]',
                        '[transform:translateX(var(--drawer-swipe-movement-x,0px))] transition-[opacity,translate] duration-(--duration-card) data-swiping:duration-0',
                        'data-ending-style:-translate-x-24 data-ending-style:opacity-0 data-starting-style:-translate-x-24 data-starting-style:opacity-0',
                        // From the right (`Drawer side`, which Base UI marks on the popup).
                        'data-[swipe-direction=right]:right-0 data-[swipe-direction=right]:left-auto data-[swipe-direction=right]:border-r-0 data-[swipe-direction=right]:border-l',
                        'data-[swipe-direction=right]:data-ending-style:translate-x-24 data-[swipe-direction=right]:data-starting-style:translate-x-24',
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
