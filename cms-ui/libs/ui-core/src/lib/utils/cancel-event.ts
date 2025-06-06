export function cancelEvent(event: Event): void {
    if (!event) {
        return;
    }
    event.preventDefault?.();
    event.stopPropagation?.();
    event.stopImmediatePropagation?.();
}
