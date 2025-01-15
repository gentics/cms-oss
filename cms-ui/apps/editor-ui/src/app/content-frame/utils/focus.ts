export function focusFirst(sourceElement: Element): HTMLElement | null {
    if (sourceElement == null) {
        return;
    }

    // Find all focusable elements
    const foundElements = sourceElement.querySelectorAll([
        'input:not([disabled])',
        'textarea:not([disabled])',
        'button:not([disabled])',
        '[tabindex]:not([disabled]):not([tabindex="-1"])',
    ].join(',')) as any as HTMLElement[]; // Casting here to prevent casting down the line

    for (const el of foundElements) {
        if (
            // Ignore all non-html elements
            typeof el.focus !== 'function'
            // Ignore invisible elements
            || el.offsetHeight <= 0
            || el.offsetWidth <= 0
        ) {
            continue;
        }

        el.focus();
        return el;
    }

    return null;
}
