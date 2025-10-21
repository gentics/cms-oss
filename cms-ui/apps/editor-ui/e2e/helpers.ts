/* eslint-disable import/no-nodejs-modules */
/// <reference lib="dom"/>
import { readFileSync } from 'fs';
import { Page as CmsPage } from '@gentics/cms-models';
import { dismissNotifications, ITEM_TYPE_PAGE, openContext } from '@gentics/e2e-utils';
import { expect, Frame, Locator, Page } from '@playwright/test';
import { HelperWindow, RENDERABLE_ALOHA_COMPONENTS } from './common';

export function findList(page: Page, type: string): Locator {
    return page.locator(`item-list .content-list[data-item-type="${type}"]`);
}

export function findItem(list: Locator, id: string | number): Locator {
    return list.locator(`gtx-contents-list-item[data-id="${id}"], masonry-item[data-id="${id}"]`);
}

export async function selectNode(element: Page | Locator, nodeId: number | string): Promise<void> {
    const context = await openContext(element.locator('node-selector > gtx-dropdown-list'));
    await context.locator(`.node-selector-list [data-id="${nodeId}"], [data-global-id="${nodeId}"]`).click();
}

export async function itemAction(item: Locator, action: string): Promise<void> {
    const dropdown = await openContext(item.locator('[data-action="item-context"]'));
    await dropdown.locator(`[data-action="${action}"]`).click();
}

export async function listAction(list: Locator, action: string): Promise<void> {
    const dropdown = await openContext(list.locator('[data-action="open-list-context"]'));
    await dropdown.locator(`[data-action="${action}"]`).click();
}

export async function findImage(list: Locator, id: string | number): Promise<Locator> {
    await list.locator('.list-body').waitFor();
    const listItems = await list.locator('gtx-contents-list-item').count();
    if (listItems < 1) {
        await list.locator('.list-header .header-controls gtx-dropdown-trigger gtx-button').click();
        await list.page().locator('gtx-dropdown-content gtx-dropdown-item[data-action="toggle-display-type"]').waitFor();
        await list.page().locator('gtx-dropdown-content gtx-dropdown-item[data-action="toggle-display-type"]').click();
    }
    return list.locator(`gtx-contents-list-item[data-id="${id}"]`);
}

export async function uploadFiles(page: Page, type: 'file' | 'image', files: string[], options?: UploadOptions): Promise<Record<string, any>> {
    // Note: This is a simplified version. You'll need to implement file upload handling
    if (options?.dragAndDrop) {
        const data = files.map(f => {
            const buffer = readFileSync(`./fixtures/${f}`).toString('base64');
            return {
                bufferData: `data:application/octet-stream;base64,${buffer}`,
                name: f,
                type: type === 'image' ? 'image/jpeg' : 'text/plain',
            };
        });
        const dataTransfer = await page.evaluateHandle(async (data) => {
            const transfer = new DataTransfer();
            // Put the binaries/Files into the transfer
            for (const file of Object.values(data)) {
                const blobData = await fetch(file.bufferData).then((res) => res.blob());
                transfer.items.add(new File([blobData], file.name, { type: file.type }))
            }
            return transfer;
        }, data);
        await page.dispatchEvent('folder-contents > [data-action="file-drop"]', 'drop', { dataTransfer }, { strict: true });
        await page.waitForRequest(request =>
            request.url().includes('/rest/file/create') ||
            request.url().includes('/rest/image/create'));
    } else {
        const fileChooserPromise = page.waitForEvent('filechooser');
        const uploadButton = page.locator(`item-list.${type} .list-header .header-controls [data-action="upload-item"] gtx-button button`);
        await uploadButton.waitFor({ state: 'visible' });
        await uploadButton.click();
        const fileChooser = await fileChooserPromise;
        await fileChooser.setFiles(files.map(f => `./fixtures/${f}`));
    }

    // Wait for upload to complete and return response
    const response = await page.waitForResponse(response =>
        response.url().includes('/rest/file/create') ||
        response.url().includes('/rest/image/create'),
    );
    const responseData = await response.json();

    const output: Record<string, any> = {};
    files.forEach(file => {
        output[file] = responseData.file || responseData.image;
    });
    return output;
}

export async function openPropertiesTab(page: Page): Promise<void> {
    await page.waitForSelector('content-frame .content-frame-container');
    const previewActivated = await page.locator('content-frame .content-frame-container .properties-tabs .tab-link[data-id="preview"].is-active').count();
    if (previewActivated > 0) {
        await page.click('content-frame .content-frame-container .properties-tabs .tab-link[data-id="properties"] a');
    }
    await page.click('content-frame .content-frame-container .properties-tabs .tab-link[data-id="item-properties"]');
}

export async function openObjectPropertyEditor(page: Page, categoryId: string | number, name: string): Promise<void> {
    await openPropertiesTab(page);
    const group = page.locator(`content-frame combined-properties-editor .properties-tabs .tab-group[data-id="${categoryId}"]`);
    if (!(await group.evaluate(el => el.classList.contains('expanded')))) {
        await group.locator('.collapsible-header').click();
    }
    const tab = group.locator(`.tab-link[data-id="object.${name}"]`);
    await tab.click();
}

export async function closeObjectPropertyEditor(page: Page, force: boolean = true): Promise<void> {
    await page.locator('content-frame gtx-editor-toolbar gtx-button.close-button').click();
    const unsavedChanges = await page.locator('confirm-navigation-modal gtx-button[type="alert"] button').count();
    if (unsavedChanges > 0 && force) {
        await page.click('confirm-navigation-modal gtx-button[type="alert"] button');
    }
}

export async function editorAction(page: Page, action: string): Promise<void> {
    // Some fantastic notifications are blocking the buttons, causing everything to lock up
    await dismissNotifications(page);
    await page.click(`content-frame gtx-editor-toolbar [data-action="${action}"] button[data-action="primary"]`);
}

export async function selectOption(element: Locator, value: number | string | (string | number)[]): Promise<void> {
    const dropdown = element.locator('> gtx-dropdown-list');
    const context = await openContext(dropdown);

    if (!Array.isArray(value)) {
        await context.locator(`[data-id="${value}"]`).click();
        return;
    }

    for (const singleValue of value) {
        const option = context.locator(`[data-id="${singleValue}"]`);
        if (await option.count() !== 1) {
            throw new Error(`Could not find any select option for value "${singleValue}"!`);
        }
        await option.click();
    }
}

export function findAlohaComponent(page: Page, options?: { slot?: string, action?: string, type?: string }, subject?: Locator): Locator {
    const root = subject || page.locator('project-editor content-frame gtx-page-editor-controls');
    const slotSelector = options?.slot ? `[slot="${options.slot}"]` : '';
    const actionSelector = `[data-action="${options.action ? options.action : 'primary'}"]`;
    const childSelector = (options?.type ? RENDERABLE_ALOHA_COMPONENTS[options.type] : '*') || '*';

    const aloha = root.locator(`gtx-aloha-component-renderer${slotSelector} > ${childSelector} button${actionSelector}`);
    return aloha;
}

export function selectRangeIn(element: Locator, start: number, end?: number): Promise<boolean> {
    return element.evaluate((el, context) => {
        window.getSelection().removeAllRanges();
        const applied = (window as unknown as HelperWindow).selectRange(
            el as HTMLElement,
            context.start,
            context.end,
        );

        if (applied) {
            (window as unknown as HelperWindow).updateAlohaRange(window, window.getSelection().getRangeAt(0));
        }

        return applied;
    }, { start, end })
}

export function selectTextIn(element: Locator, textToSelect: string): Promise<boolean> {
    return element.evaluate((el, context) => {
        window.getSelection().removeAllRanges();
        const win = (window as any as HelperWindow);
        const applied = win.selectText(el as HTMLElement, context.textToSelect);

        if (applied) {
            win.updateAlohaRange(win, win.getSelection().getRangeAt(0));
        }

        return applied;
    }, { textToSelect });
}

export function selectEditorTab(page: Page, id: string): Promise<void> {
    return page.locator(`gtx-page-editor-tabs button[data-id="${id}"]`).click();
}

async function createLink(page: Page, handler: (form: Locator) => Promise<void>): Promise<void> {
    await selectEditorTab(page, 'formatting');

    const linkButton = findAlohaComponent(page, { slot: 'insertLink', type: 'toggle-split-button' });
    await linkButton.click();

    // Fill link form
    const modal = page.locator('gtx-dynamic-form-modal');
    const form = modal.locator('.form-wrapper');

    await handler(form);

    await modal.locator('.modal-footer [data-action="confirm"] button[data-action="primary"]').click();
}

export async function createExternalLink(
    page: Page,
    formHandler: (form: Locator) => Promise<void>,
): Promise<void> {
    return createLink(page, async form => {
        // Fill out rest of the form
        await formHandler(form);
    });
}

export async function createInternalLink(
    page: Page,
    repoHandler: (repoBrowser: Locator) => Promise<void>,
    formHandler: (form: Locator) => Promise<void>,
): Promise<void> {
    return createLink(page, async form => {
        // Select internal page
        await form.locator('[data-slot="url"] .target-wrapper .internal-target-picker').click();
        const repoBrowser = page.locator('repository-browser');
        await repoHandler(repoBrowser);
        await repoBrowser.locator('.modal-footer [data-action="confirm"] button').click();

        // Fill out rest of the form
        await formHandler(form);
    });
}

export function findDynamicDropdown(page: Page, ref: string): Locator {
    return page.locator(`gtx-dynamic-dropdown .gtx-context-menu[data-ref="${ref}"]`);
}

export async function findDynamicFormModal(page: Page, ref?: string): Promise<Locator> {
    const refSelector = ref ? `[data-ref="${ref}"]` : '';
    const modal = page.locator(`gtx-dynamic-modal gtx-dynamic-form-modal${refSelector}`);
    await modal.waitFor();
    return modal;
}

export async function getAlohaIFrame(page: Page): Promise<Frame> {
    const iframeSelector = '[name="master-frame"][loaded="true"]';
    await page.locator('iframe' + iframeSelector).waitFor({ timeout: 60_000 });
    return page.frame({ name: 'master-frame' });
}

export async function openPageForEditing(page: Page, pageToEdit: CmsPage): Promise<{
    row: Locator,
    iframe: Frame,
    editable: Locator,
}> {
    // Setup page for editing
    const list = findList(page, ITEM_TYPE_PAGE);
    const row = findItem(list, pageToEdit.id);
    await itemAction(row, 'edit');

    // Wait for editor to be ready
    const iframe = await getAlohaIFrame(page);
    const editable = iframe.locator('main .container [contenteditable="true"]');
    await editable.waitFor({ timeout: 60_000 });

    return {
        row,
        iframe,
        editable,
    }
}

export async function setupHelperWindowFunctions(page: Page): Promise<void> {
    // annoying copy paste from e2e-utils, as context is mounted into the runtime
    // from playwright, where only serializable objects can be passed.
    // since functions are not serializable, we have to define them here.
    await page.addInitScript(() => {
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
        function createRange(element: HTMLElement, start: number, end: number | null = null): Range | null {
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
        function selectRange(element: HTMLElement, start: number, end: number | null = null): boolean {
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
        function selectText(element: HTMLElement, text: string): boolean {
            const idx = element.textContent.indexOf(text);
            if (idx === -1) {
                return false;
            }

            return selectRange(element, idx, idx + text.length);
        }

        /**
         * Updates the selection handler in Aloha to properly select the range.
         *
         * @param win Window object where the Aloha object is available.
         * @param range The new range that should be applied/updated.
         */
        function updateAlohaRange(win: Window, range: Range): void {
            const aloha = (win as any).Aloha;
            // eslint-disable-next-line no-underscore-dangle, @typescript-eslint/no-unsafe-call
            aloha.Selection._updateSelection(null, new aloha.Selection.SelectionRange(range));
        }

        (window as any as HelperWindow).createRange = createRange;
        (window as any as HelperWindow).selectRange = selectRange;
        (window as any as HelperWindow).selectText = selectText;
        (window as any as HelperWindow).updateAlohaRange = updateAlohaRange;
    });
}

export async function expectItemOffline(item: Locator): Promise<void> {
    // TODO it would be better not to test on the icon
    await expect(item.locator('icon.main-icon')).toHaveText('cloud_off');
}

export async function expectItemPublished(item: Locator): Promise<void> {
    // TODO it would be better not to test on the icon
    await expect(item.locator('icon.main-icon')).toHaveText('cloud_upload');
}

export async function openToolOrAction(page: Page, id: string): Promise<void> {
    const context = await openContext(page.locator('gtx-top-bar gtx-actions-selector gtx-dropdown-list'));
    const btn = context.locator(`.action-button[data-tool-id="${id}"], .action-button[data-action-id="${id}"]`);
    await btn.click();
}

export function overrideAlohaConfig(page: Page, configFilename: string): Promise<void> {
    return page.route('/internal/minimal/files/js/aloha-config.js', route => {
        return route.fulfill({
            headers: {
                location: `/internal/minimal/files/js/${configFilename}`,
            },
            status: 301,
        })
    });
}
