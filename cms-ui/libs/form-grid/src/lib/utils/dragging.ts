import { FormElement } from '@gentics/cms-models';
import { ElementMoveData, UNIQUE_ID_GLUE } from '../models';

/**
 * Helper function which checks if a move is allowed for a certain container.
 * @param moveData The data of the element that is currently being moved
 * @param wl The whitelist of the container element it's supposed to move to
 * @param containerContextId The context-id of the container the element it's supposed to move to
 * @param rootId The root container-id
 * @param elMap Map of all current elements
 * @param eqInsert Boolean value to check against if the move data is inserting. Leave empty if it shouldn't check.
 * @returns If the drag operation should be blocked for the container.
 */
export function blockDragForElement(
    moveData: ElementMoveData,
    wl: string[],
    containerContextId: string,
    rootId: string,
    elMap: Record<string, FormElement>,
    eqInsert?: boolean,
): boolean {
    // Block if we have no data, to prevent potentially wrong drops
    if (!moveData) {
        return true;
    }

    // Checking the whitelist
    if (Array.isArray(wl)) {
        const elType = moveData.elementType;
        if (!elType && !wl.includes(elType)) {
            return false;
        }
    }

    if (typeof eqInsert === 'boolean') {
        // If we drag a new item from the palette, we have nothing else to check anymore
        if (moveData.inserting === eqInsert) {
            return false;
        }
    }

    // If we move within the same context, then it's safe
    if (moveData.contextId === containerContextId) {
        return false;
    }

    const uniqueElId = containerContextId === rootId
        ? moveData.elementId
        : `${containerContextId}${UNIQUE_ID_GLUE}${moveData.elementId}`;

    // If our context already has such an element, block it
    if (elMap[uniqueElId] != null) {
        return true;
    }

    const uniqueMoveElId = moveData.contextId === rootId
        ? moveData.elementId
        : `${moveData.contextId}${UNIQUE_ID_GLUE}${moveData.elementId}`;

    const moveEl = elMap[uniqueMoveElId];
    if (moveEl == null) {
        return true;
    }

    // Allow if we have no children to check
    if (
        !Array.isArray(moveEl.elements)
        || moveEl.elements.length === 0
    ) {
        return false;
    }

    // Since we can also drag container elements, we need to check for all
    // children if they are valid.
    for (const child of moveEl.elements) {
        const childMoveData: ElementMoveData = {
            elementId: child.id,
            contextId: moveEl.type === 'aggregate' ? moveEl.id : moveData.contextId,
            elementType: child.formGridOptions?.type,
            inserting: moveData.inserting,
        };
        if (blockDragForElement(childMoveData, wl, containerContextId, rootId, elMap)) {
            return true;
        }
    }

    return false;
}
