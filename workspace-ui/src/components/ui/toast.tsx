import { Toast as ToastPrimitive } from '@base-ui/react/toast';
import { CircleAlertIcon, CircleCheckIcon, InfoIcon, XIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

import { cn } from './utils';

// design.md §12 "Toast": `--solid`, white text, `--r-lg`, `--sh-toast`, icon `#7FC7EA`, optional
// action `rgba(255,255,255,.14)` / hover `.26`. Error toast: `--errbg`, edge and text in `--err`.
// §11: top right, max. 520 px. §10: `drop`.

/** Toast types. Status is never shown by colour alone (§3.3): each type has its own icon. */
type ToastType = 'info' | 'success' | 'error';

const icons: Record<ToastType, ReactNode> = {
    info: <InfoIcon size={18} />,
    success: <CircleCheckIcon size={18} />,
    error: <CircleAlertIcon size={18} />,
};

function isToastType(type: string | undefined): type is ToastType {
    return type !== undefined && Object.hasOwn(icons, type);
}

function ToastList() {
    const { t } = useTranslation();
    const { toasts } = ToastPrimitive.useToastManager();

    return toasts.map((toast) => {
        const isError = toast.type === 'error';

        return (
            <ToastPrimitive.Root
                key={toast.id}
                toast={toast}
                data-slot="toast"
                className={cn(
                    'flex w-full items-start gap-10 rounded-lg py-8 pr-8 pl-12 text-base leading-normal shadow-toast',
                    'animate-drop transition-opacity duration-(--dur-pop) data-ending-style:opacity-0',
                    // Over the provider's limit Base UI makes the oldest toasts inert; they come back
                    // once a newer one closes.
                    'data-limited:hidden',
                    isError ? 'border border-err/30 bg-errbg' : 'bg-solid text-white',
                )}
            >
                <span className={cn('pt-6', isError ? 'text-err' : 'text-toast-icon')}>{icons[isToastType(toast.type) ? toast.type : 'info']}</span>
                <ToastPrimitive.Content className="flex min-w-0 flex-1 flex-col gap-2 py-4">
                    {/* Renders an `h2`: the text colour beats the global `h2` colour from src/index.css. */}
                    <ToastPrimitive.Title
                        data-slot="toast-title"
                        className={cn('text-base leading-normal font-medium', isError ? 'text-err' : 'text-white')}
                    />
                    <ToastPrimitive.Description
                        data-slot="toast-description"
                        className={cn('text-sm leading-normal [overflow-wrap:anywhere]', isError ? 'text-slate' : 'text-white')}
                    />
                </ToastPrimitive.Content>
                <ToastPrimitive.Action
                    data-slot="toast-action"
                    className={cn(
                        'shrink-0 cursor-pointer self-center rounded-md px-10 py-4 text-sm font-medium transition-colors',
                        isError ? 'border border-err/30 bg-surface text-err hover:border-err' : 'bg-white/14 text-white hover:bg-white/26',
                    )}
                />
                <ToastPrimitive.Close
                    data-slot="toast-close"
                    aria-label={t('ui.dismiss')}
                    className={cn(
                        'inline-flex size-28 shrink-0 cursor-pointer items-center justify-center rounded-md transition-colors',
                        isError ? 'text-err hover:bg-err/10' : 'text-white hover:bg-white/14',
                    )}
                >
                    <XIcon size={16} />
                </ToastPrimitive.Close>
            </ToastPrimitive.Root>
        );
    });
}

/**
 * Provides toasts to `children` and renders them. Show one with `useToast().add({ title, type })`
 * from `./use-toast`.
 */
function Toaster({ children }: { children: ReactNode }) {
    const { t } = useTranslation();

    return (
        <ToastPrimitive.Provider>
            {children}
            <ToastPrimitive.Portal>
                <ToastPrimitive.Viewport
                    data-slot="toast-viewport"
                    aria-label={t('ui.notifications')}
                    className="fixed top-16 right-16 z-50 flex w-[min(520px,calc(100%-32px))] flex-col gap-8 outline-none"
                >
                    <ToastList />
                </ToastPrimitive.Viewport>
            </ToastPrimitive.Portal>
        </ToastPrimitive.Provider>
    );
}

export { Toaster };
export type { ToastType };
