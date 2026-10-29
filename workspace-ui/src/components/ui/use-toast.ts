import { Toast as ToastPrimitive } from '@base-ui/react/toast';

/**
 * Adds, updates and closes toasts inside a `<Toaster>`. `type` is `'info'` (default),
 * `'success'` or `'error'`; `actionProps` adds one action button, e.g. "Undo".
 */
export const useToast = ToastPrimitive.useToastManager;
