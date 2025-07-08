import { File as CMSFile, Folder, Group, Image, Node, Page, ScheduleTask, User, Variant } from '@gentics/cms-models';
import type { Suite } from 'mocha';
import {
    ENV_CMS_VARIANT,
    FileImportData,
    FolderImportData,
    FormattedText,
    FORMATTING_NODES,
    GroupImportData,
    ImageImportData,
    IMPORT_ID,
    ImportData,
    NodeImportData,
    PageImportData,
    ScheduleTaskImportData,
    UserImportData,
} from './common';

export function getItem(data: NodeImportData, entities: Record<string, any>): Node | null;
export function getItem(data: FolderImportData, entities: Record<string, any>): Folder | null;
export function getItem(data: PageImportData, entities: Record<string, any>): Page | null;
export function getItem(data: ImageImportData, entities: Record<string, any>): Image | null;
export function getItem(data: FileImportData, entities: Record<string, any>): CMSFile | null;
export function getItem(data: GroupImportData, entities: Record<string, any>): Group | null;
export function getItem(data: UserImportData, entities: Record<string, any>): User | null;
export function getItem(data: ScheduleTaskImportData, entities: Record<string, any>): ScheduleTask | null;
export function getItem(data: ImportData | string, entities: Record<string, any>): any {
    const importId = typeof data === 'string' ? data : data[IMPORT_ID];
    return entities[importId] || null;
}

export function envAll(env: string | string[]): boolean;
export function envAll(...vars: string[]): boolean {
    return vars.every(envName => process.env[envName]);
}

export function envAny(env: string | string[]): boolean;
export function envAny(...vars: string[]): boolean {
    return vars.some(envName => process.env[envName]);
}

export function envNone(env: string | string[]): boolean;
export function envNone(...vars: string[]): boolean {
    return vars.every(envName => !process.env[envName]);
}

export function isVariant(variant: Variant): boolean {
    return process.env[ENV_CMS_VARIANT] === variant;
}

// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function isJQueryElement($subject: any): $subject is JQuery {
    return !(
        $subject == null
        || typeof $subject !== 'object'
        || typeof $subject.length !== 'number'
        || typeof $subject.jquery !== 'string'
    );
}

export function skipableSuite(doExecute: boolean, title: string, fn: (this: Suite) => void): Suite | void {
    return (doExecute ? describe : describe.skip)(title, fn);
}

/**
 * Extracts the formatting of the provided elements content in a nice array to properly text against.
 * @example
 ```html
    <div id="main">
        <p>Normal Text</p>
        <p>more <b>advanced</b><i> Text</i></p>
        foo
        <div>
            <p>even <b><s>nested</s> <i>content</i> works</b></p>
        </div>
    </div>
 ```
 ```js
    [
        { text: "Normal Text", formats: [] },
        { text: "more ", formats: [] },
        { text: "advanced", formats: ["b"] },
        { text: " Text", formats: ["i"] },
        { text: "foo", formats: [] },
        { text: "even ", formats: [] },
        { text: "nested", formats: ["b", "s"] },
        { text: " ", formats: ["b"] },
        { text: "content", formats: ["b", "i"] },
        { text: " works", formats: ["b"] },
    ]
 ```
 */
export function extractFormatting(element: HTMLElement): FormattedText[] {
    const entries: FormattedText[] = [];
    const walker = element.ownerDocument.createTreeWalker(element, NodeFilter.SHOW_TEXT);

    while (walker.nextNode() != null) {
        const activeFormats = new Set<string>();
        let parent = walker.currentNode.parentElement;
        let name: string;

        // Set upper bounds
        while (parent != null && parent !== element && parent !== element.ownerDocument.body) {
            name = parent.nodeName.toLocaleLowerCase();
            if (FORMATTING_NODES.has(name)) {
                activeFormats.add(name);
            // Break at boundaries?
            // } else if (BOUNDARY_NODES.has(name)) {
            //     break;
            }
            parent = parent.parentElement;
        }

        entries.push({
            text: walker.currentNode.textContent,
            formats: Array.from(activeFormats),
        });
    }

    return entries;
}

/**
 * Checks if two arrays are equals in sense of content, ignoreing element order.
 * @param arr1 First array
 * @param arr2 Second array
 * @param eqFn Optional compare/equality function
 * @returns If both arrays have the same content, not neccessarily in the same order
 */
export function arrayEqual<T>(arr1: T[], arr2: T[], eqFn?: (a: T, b: T) => boolean): boolean {
    if (arr1.length !== arr2.length) {
        return false;
    }

    if (typeof eqFn !== 'function') {
        eqFn = (a, b) => a === b;
    }

    for (const val1 of arr1) {
        let found = false;
        for (const val2 of arr2) {
            // eslint-disable-next-line no-cond-assign
            if (found = eqFn(val1, val2)) {
                break;
            }
        }
        if (!found) {
            return false;
        }
    }

    return true;
}

/**
 * Attempts to create a range from the provided infos, and will then add it to the
 * windows selection.
 * If you wish to replace the selection, then call the `window.getSelection().removeAllRanges()`,
 * to clear the previous selection before calling this function.
 *
 * @param element The element in which the selection should be applied
 * @param start The starting index/offset from where the selection should start
 * @param end The ending index/offset until where the selection should be set.
 * If left empty/`null`, then it'll apply the selection util the end of the container `element`.
 * You may also provide a negative value, to count from the end instead.
 * @returns The range if it was possible to create, otherwise `null`.
 */
export function createRange(element: HTMLElement, start: number, end: number | null = null): Range | null {
    const size = element.textContent.length;

    // When a positive end is provided, it has to be greater than the start position.
    if (end != null && end > -1 && end < start) {
        return null;
    // When a negative value is provided, we have to check the bounds from the other side.
    } else if (end != null && end < 0 && (size + end) < start) {
        return null;
    }

    const doc = element.ownerDocument;
    const win = doc.defaultView;
    let offset = 0;

    const walker = doc.createTreeWalker(element, NodeFilter.SHOW_TEXT);
    const range = new win.Range();
    const nodesAfterStart: globalThis.Node[] = [];

    while (walker.nextNode() != null) {
        // Start has been set already, simply accumulate the nodes now
        if (range.startContainer !== doc) {
            nodesAfterStart.push(walker.currentNode);
            continue;
        }

        const len = walker.currentNode.textContent.length;
        // The range has to start with this element
        if ((len + offset) < start) {
            offset += len;
            continue;
        }
        range.setStart(walker.currentNode, start - offset);

        // If the end position is in this same node
        nodesAfterStart.push(walker.currentNode);
    }

    if (!end) {
        const last = nodesAfterStart.pop();
        range.setEnd(last, last.textContent.length);
    } else if (end === start) {
        // If it's the same position, it's actually just a cursor position/collapsed range.
        range.collapse(true);
    } else if (end < 0) {
        // Since we start backwards from the "beginning", we have to reset the offset
        offset = 0;
        // Convert it back to a positive number, to make calculations with the offset easier
        const posEnd = end * -1;
        for (const node of nodesAfterStart.reverse()) {
            const len = node.textContent.length;
            if ((offset + len) < posEnd) {
                offset += len;
                continue;
            }

            range.setEnd(node, len + end);
            break;
        }
    } else {
        for (const node of nodesAfterStart) {
            const len = node.textContent.length;

            if ((len + offset) >= end) {
                range.setEnd(node, end - offset);
                break;
            }

            offset += len;
        }
    }

    // If the end container has not been set, then we don't want to apply the range at all
    if (range.endContainer === doc) {
        return null;
    }

    return range;
}

/**
 * @see {@link createRange}
 * @returns If the range could be created and applied to the window's selection.
 */
export function selectRange(element: HTMLElement, start: number, end: number | null = null): boolean {
    const range = createRange(element, start, end);

    if (!range) {
        return false;
    }

    element.ownerDocument.defaultView.getSelection().addRange(range);
    return true;
}

/**
 * Wrapper for `selectRange`, where the `start` and `end` position are based on the
 * `indexOf` from the `element`'s `textContent`.
 * If the text can't be found at all (i.E. `indexOf` === -1), then it returns `false`.
 *
 * @param element The element in which the selection should be applied
 * @param text The text to search and select.
 * @returns If the text was successfully selected.
 * @see {@link selectRange}
 * @see {@link createRange}
 */
export function selectText(element: HTMLElement, text: string): boolean {
    const idx = element.textContent.indexOf(text);
    if (idx === -1) {
        return false;
    }

    return selectRange(element, idx, idx + text.length);
}

const REGEX_ALOHA_EMPTY = /(^<p>\n<\/p>|<p>\n<\/p>$)/g;

/**
 * Aloha will sometimes add empty paragraphs at the end of content,
 * to make it possible to click there and create a new paragraph.
 * This function simply removes these empty paragraphs from the start
 * and beginning of the content and returns it.
 * @param content Text/HTML Content
 * @returns A trimmed version of the content
 */
export function trimAlohaEmpty(content: string): string {
    return content.replaceAll(REGEX_ALOHA_EMPTY, '');
}

/**
 * Updates the selection handler in Aloha to properly select the range.
 *
 * @param win Window object where the Aloha object is available.
 * @param range The new range that should be applied/updated.
 */
export function updateAlohaRange(win: Window, range: Range): void {
    const aloha = (win as any).Aloha;
    // eslint-disable-next-line no-underscore-dangle, @typescript-eslint/no-unsafe-call
    aloha.Selection._updateSelection(null, new aloha.Selection.SelectionRange(range));
}

/**
 * A combination matrix where each combination of the elements is created
 * and output in a two dimensional array.
 * @example
```ts
combinationMatrix([1, 2]);
// [ [1], [1, 2], [2] ]
combinationMatrix([1, 2, 3]);
// [ [1], [1, 2], [1, 3], [1, 2, 3], [2], [2, 3], [3]]
combinationMatrix([1, 2, 3, 4])
// [
//      [1],
//      [1, 2], [1, 3], [1, 4],
//      [1, 2, 3], [1, 2, 4],
//      [1, 2, 3, 4],
//      [1, 3, 4],
//      [2],
//      [2, 3], [2, 4],
//      [2, 3, 4],
//      [3],
//      [3, 4],
//      [4]
// ]
```
 * As you can see, the matrix multiplies all variations (ignoring order),
 * and exponentionally increases the multiplications, so be careful how many items you provide.
 *
 * @param elements The elements which should be combined to build the matrix
 * @returns A fully combined matrix
 */
export function combinationMatrix<T>(elements: T[]): T[][] {
    const out: T[][] = [];

    // eslint-disable-next-line @typescript-eslint/prefer-for-of
    for (let i = 0; i < elements.length; i++) {
        out.push([elements[i]]);

        // Start and end positions in the array to create slices to repeat later on
        for (let start = i + 1; start < elements.length; start++) {
            for (let end = start; end < elements.length; end++) {
                // If there's no "buffer" set (start === end), we don't want to repeat values we already have
                // Unless it's the very first round, then we do want to populate them
                if (start === end && start !== i + 1) {
                    continue;
                }

                /** This will be filled with the range from start to end, to be appended before each entry.*/
                const buffer: T[] = [elements[i], ...elements.slice(start, end)];

                for (let k = end; k < elements.length; k++) {
                    out.push([...buffer, elements[k]]);
                }
            }
        }
    }

    return out;
}

export function isUrlPath(url: string, path: string): boolean {
    const urlObj = new URL(url);
    return urlObj.pathname === path;
}

export function hasMatchingParams(url: string, params: Record<string, string>): boolean {
    const urlObj = new URL(url);
    return Object.entries(params).every(([key, value]) => urlObj.searchParams.get(key) === value);
}
