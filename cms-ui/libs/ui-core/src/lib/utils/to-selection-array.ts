import { CheckboxState, TableSelection } from '../common';

export function toSelectionArray(selection: string[] | TableSelection, state: CheckboxState = true): string[] {
    if (selection == null || typeof selection !== 'object') {
        return [];
    }

    if (Array.isArray(selection)) {
        return selection;
    }

    const out: string[] = [];
    for (const entry of Object.entries(selection)) {
        if (entry[1] === state) {
            out.push(entry[0]);
        }
    }

    return out;
}
