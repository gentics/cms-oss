/** The element id of an interaction's card (`InteractionCard`), for `focusInteractionCard`. */
export function interactionCardId(interactionId: string): string {
    return `interaction-card-${interactionId}`;
}

/**
 * Scrolls the card of a pending interaction into view and focuses it, for the plan's "Answer"
 * (`PlanCard`). `false` when the card is not shown.
 */
export function focusInteractionCard(interactionId: string): boolean {
    const card = document.getElementById(interactionCardId(interactionId));

    if (!card) {
        return false;
    }

    card.scrollIntoView({ block: 'center' });
    card.focus({ preventScroll: true });

    return true;
}
