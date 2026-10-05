import { LucideProvider } from 'lucide-react';
import type { ReactNode } from 'react';

import { Toaster } from './toast';
import { TooltipProvider } from './tooltip';

import './ui.css';

/**
 * Wraps the app once: loads the component layer styles, sets the Lucide defaults (design.md §9:
 * stroke 1.7, 18 px, `currentColor`) and provides tooltips and toasts.
 */
function UiProvider({ children }: { children: ReactNode }) {
    return (
        <LucideProvider size={18} strokeWidth={1.7}>
            <TooltipProvider>
                <Toaster>{children}</Toaster>
            </TooltipProvider>
        </LucideProvider>
    );
}

export { UiProvider };
