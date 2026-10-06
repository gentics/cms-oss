import { Dialog as DialogPrimitive } from '@base-ui/react/dialog';
import { XIcon } from 'lucide-react';
import type { ComponentProps } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from './button';
import { cn } from './utils';

// design.md §12 "Dialog / Drawer", §11 overlays (max. 720 × 600 px), §6.3 backdrop.

function Dialog(props: DialogPrimitive.Root.Props) {
    return <DialogPrimitive.Root data-slot="dialog" {...props} />;
}

function DialogTrigger(props: DialogPrimitive.Trigger.Props) {
    return <DialogPrimitive.Trigger data-slot="dialog-trigger" {...props} />;
}

function DialogClose(props: DialogPrimitive.Close.Props) {
    return <DialogPrimitive.Close data-slot="dialog-close" {...props} />;
}

function DialogContent({
    className,
    children,
    showCloseButton = true,
    ...props
}: DialogPrimitive.Popup.Props & { showCloseButton?: boolean }) {
    const { t } = useTranslation();

    return (
        <DialogPrimitive.Portal>
            <DialogPrimitive.Backdrop
                data-slot="dialog-overlay"
                className="fixed inset-0 z-50 bg-backdrop transition-opacity duration-(--duration-popover) data-ending-style:opacity-0 data-starting-style:opacity-0"
            />
            <DialogPrimitive.Popup
                data-slot="dialog-content"
                className={cn(
                    'fixed top-1/2 left-1/2 z-50 flex max-h-[min(600px,calc(100dvh-32px))] w-[min(720px,calc(100vw-32px))] -translate-x-1/2 -translate-y-1/2 flex-col overflow-hidden rounded-3xl border border-line2 bg-surface text-base leading-normal text-ink shadow-modal',
                    'transition-[opacity,scale] duration-(--duration-popover) data-ending-style:opacity-0 data-starting-style:scale-[.985] data-starting-style:opacity-0',
                    className,
                )}
                {...props}
            >
                {children}
                {showCloseButton && (
                    <DialogPrimitive.Close
                        data-slot="dialog-close"
                        aria-label={t('ui.close')}
                        render={<Button variant="ghost" size="icon" className="absolute top-12 right-12 text-slate" />}
                    >
                        <XIcon size={18} />
                    </DialogPrimitive.Close>
                )}
            </DialogPrimitive.Popup>
        </DialogPrimitive.Portal>
    );
}

function DialogHeader({ className, ...props }: ComponentProps<'div'>) {
    return <div data-slot="dialog-header" className={cn('flex flex-col gap-4 px-16 pt-16 pr-52 pb-12', className)} {...props} />;
}

function DialogBody({ className, ...props }: ComponentProps<'div'>) {
    return <div data-slot="dialog-body" className={cn('min-h-0 flex-1 overflow-y-auto px-16 pb-16', className)} {...props} />;
}

function DialogFooter({ className, ...props }: ComponentProps<'div'>) {
    return (
        <div
            data-slot="dialog-footer"
            className={cn('flex flex-wrap justify-end gap-8 border-t border-line bg-tint px-16 py-12', className)}
            {...props}
        />
    );
}

function DialogTitle({ className, ...props }: DialogPrimitive.Title.Props) {
    return <DialogPrimitive.Title data-slot="dialog-title" className={cn('text-md leading-normal font-medium text-ink', className)} {...props} />;
}

function DialogDescription({ className, ...props }: DialogPrimitive.Description.Props) {
    return <DialogPrimitive.Description data-slot="dialog-description" className={cn('text-sm leading-normal text-slate', className)} {...props} />;
}

export { Dialog, DialogBody, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger };
