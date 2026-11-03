import { type ClassValue, clsx } from 'clsx';
import { extendTailwindMerge } from 'tailwind-merge';

// tailwind-merge has to know the custom theme keys from `ui.css`, otherwise it treats e.g.
// `text-sm` (size) and `text-ink` (colour) as the same group and drops one of them.
const twMerge = extendTailwindMerge({
    extend: {
        theme: {
            text: ['overline', 'xs', 'sm', 'base', 'md', 'lg'],
            radius: ['xs', 'sm', 'md', 'lg', 'xl', '2xl', '3xl', 'full'],
            shadow: ['1', 'float', 'pop', 'modal', 'drawer', 'toast'],
        },
    },
});

/** Joins class names and resolves conflicting Tailwind classes (the shadcn/ui `cn` helper). */
export function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs));
}
