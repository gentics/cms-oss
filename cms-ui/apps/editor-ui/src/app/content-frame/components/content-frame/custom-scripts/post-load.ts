/* eslint-disable no-underscore-dangle */
import { AlohaIntegrationService } from '@editor-ui/app/content-frame/providers';
import { typeIdsToName } from '@gentics/cms-components';
import { EditMode } from '@gentics/cms-integration-api-models';
import { Page } from '@gentics/cms-models';
import { ALOHAPAGE_URL, API_BASE_URL } from '../../../../common/utils/base-urls';
import { CNIFrameDocument, CNWindow, GCNJsLibRequestOptions } from '../../../models/content-frame';
import { CustomScriptHostService } from '../../../providers/custom-script-host/custom-script-host.service';

const ATTR_OBJECT_ID = 'data-gentics-aloha-object-id';
const ATTR_NODE_ID = 'data-gcn-channelid';
const ATTR_REPO = 'data-gentics-aloha-repository';
const REPO_CMS_ITEM = 'com.gentics.aloha.GCN.Page';
const FILE_LINK_PREFIX = `${API_BASE_URL}/file/content/load`;

interface InternalLink {
    nodeId?: number;
    type: 'file' | 'image' | 'page' | 'form';
    itemId: number;
}

/**
 * This will execute in the context of the IFrame, when the code inside it calls the `GCMSUI.runPostLoadScript()` method.
 * This occurs when the document's `load` event fires (i.e. full document and all resources are loaded).
 *
 * Scripts that need to manipulate DOM which may itself be generated by other scripts on the page (such as the tagfill
 * stuff) needs to go in here.
 *
 * For scripts that do not rely on generated DOM, use the pre-load function.
 *
 * We pass in the instance of the ScriptHost so that we can refer back to that scope and interact directly it.
 */
export class PostLoadScript {

    private editablesChanged = false;
    private pageIsSavingFromSaveButton = false;

    constructor(
        private iFrameWindow: CNWindow,
        private iFrameDocument: CNIFrameDocument,
        private scriptHost: CustomScriptHostService,
        private aloha: AlohaIntegrationService,
    ) { }

    run(): void {
        this.setupAlohaHooks(this.iFrameWindow);
        this.handleClickEventsOnLinks();
        this.notifyWhenContentsChange();
        this.scriptHost.runChangeDetection();
    }

    private setupAlohaHooks(iFrameWindow: CNWindow): void {
        if (!this.aloha) {
            return;
        }

        this.aloha.setWindow(iFrameWindow);

        // In case of an error, the Aloha property may not be present.
        if (iFrameWindow.Aloha != null) {
            this.aloha.reference$.next(iFrameWindow.Aloha);
            this.aloha.settings$.next(iFrameWindow.Aloha.settings);
            iFrameWindow.Aloha.ready(() => {
                this.aloha.ready$.next(true);
                // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                this.aloha.windowLoaded$.next(true);
            });
        } else {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            this.aloha.windowLoaded$.next(true);
        }

        iFrameWindow.addEventListener('unload', () => {
            this.aloha.settings$.next(null);
            this.aloha.reference$.next(null);
        });
    }

    /**
     * Intercept any clicks on anchor links within the iframe. External links should open in a new window to prevent
     * the UI state getting messed up, and internal links to other pages should cause a regular navigation within the UI app.
     *
     * Clicks to links should only be valid when clicking with `CTRL` or middle-mouse clicks on external links.
     */
    handleClickEventsOnLinks(): void {
        const handleClickEvent = (e: MouseEvent, middleClick: boolean) => {
            // Ignore already handled events
            if (e.defaultPrevented) {
                return;
            }

            const link = e.target;
            if (!isAnchorElement(link)) {
                return;
            }
            const url = link.getAttribute('href');
            const internalLink = parseInternalLink(link);

            if (internalLink) {
                // All internal links are handled here, so prevent default handling
                e.preventDefault();

                // Ignore clicks which aren't the CTRL key, and ignore all middle-clicks
                if ((this.scriptHost.editMode === EditMode.EDIT && !e.ctrlKey) || middleClick) {
                    return;
                }

                // Edge case handling until SUP-18130 is addressed
                if (internalLink.nodeId == null) {
                    return;
                }

                // Attempt to internally navigate
                if (internalLink.type === 'page') {
                    this.scriptHost.navigateToPagePreview(internalLink.nodeId, internalLink.itemId);
                } else if (internalLink.type === 'file' || internalLink.type === 'image') {
                    this.scriptHost.navigateToFileOrImagePreview(internalLink.nodeId, internalLink.type, internalLink.itemId);
                }

                return;
            }

            // Anchors/Hash links are fine, as they don't cause a navigation
            if (url.startsWith('#')) {
                return;
            }

            // While editing, we want to ignore regular clicks to links, as it would interfere with editing
            // links in general and cause other issues as well.
            // The default behavior of a middle-click would be, to open a link in a new tab.
            // This doesn't work in contenteditables however, unless the link has "_blank" for some reason.
            // Therefore we always handle it manually here.
            if (this.scriptHost.editMode === EditMode.EDIT && !e.ctrlKey && !middleClick) {
                e.preventDefault();
                return;
            }

            // Open external links always in a new tab, since we don't want to close our app
            e.preventDefault();
            this.iFrameWindow.open(url, '_blank');
            return;
        };

        // Simple left click handler
        this.iFrameDocument.body.addEventListener('click', (e: MouseEvent) => {
            handleClickEvent(e, false);
        }, true);

        // Middle click handler
        this.iFrameDocument.body.addEventListener('auxclick', (e: PointerEvent) => {
            if (e.button === 1) {
                handleClickEvent(e, true);
            }
        }, true);

        /**
         * Intercept contextmenu to handle the case where internal links are opened in a new tab / window. In this case,
         * we want to cause the whole UI app to be loaded into that new window with the correct URL pointing to
         * the page which was clicked.
         *
         * This is done by temporarily changing the href of the link when the context menu is opened, and then changing
         * it back once the link loses focus.
         */
        this.iFrameDocument.body.addEventListener('contextmenu', (e: MouseEvent) => {
            if (e.defaultPrevented) {
                return;
            }

            const target = e.target;
            if (!isAnchorElement(target)) {
                return;
            }

            const internalLink = parseInternalLink(target);

            if (!internalLink || internalLink.type !== 'page') {
                return;
            }

            // Edge case handling until SUP-18130 is addressed
            if (internalLink.nodeId == null) {
                return;
            }

            const originalHref = target.getAttribute('href');
            target.href = this.scriptHost.getInternalLinkUrlToPagePreview(internalLink.nodeId, internalLink.type, internalLink.itemId);
            const resetHref = (): void => {
                target.setAttribute('href', originalHref);
                target.removeEventListener('blur', resetHref);
            };
            target.addEventListener('blur', resetHref);
        });
    }

    notifyWhenContentsChange(): void {
        this.listenToAlohaEvents();
        this.periodicallyPollAlohaModified();
        this.notifyWhenPageIsModifiedViaJsLib();
    }

    /**
     * TODO: The "correct" way to do this would be to use the `Aloha.isModified()` method to check.
     * However, currently this is completely unreliable - see https://jira.gentics.com/browse/SUP-3985
     *
     * For the time being we rely on the `aloha-smart-content-changed` event, which unfortunately
     * fires immediately after loading the page in some cases. Therefore, we ignore the first event.
     */
    listenToAlohaEvents(): void {
        if (!this.iFrameDocument || !this.iFrameDocument.body || !this.iFrameWindow.Aloha || typeof this.iFrameWindow.Aloha.bind !== 'function') {
            return;
        }

        const initialTime = new Date().getTime();
        let isFirstChangeEvent = true;

        this.iFrameWindow.Aloha.bind('aloha-smart-content-changed', (event: Event) => {
            const delay = event.timeStamp - initialTime;

            // Ignore the `smart-content-changed` fired when opening the page
            if ((!isFirstChangeEvent || delay > 3000) && this.iFrameWindow.Aloha.isModified()) {
                this.editablesChanged = true;
                this.scriptHost.setContentModified(true);
            }
            isFirstChangeEvent = false;
        });
    }

    /**
     * Not all AlohaEditor plugins trigger the `aloha-smart-content-changed` event when something is changed.
     * As a hack, we poll for the Aloha.isModified() state, until the content is marked modified.
     */
    periodicallyPollAlohaModified(): void {
        this.iFrameWindow.setInterval(() => {
            if (!this.scriptHost.contentFrame.contentModified
                && this.iFrameWindow.Aloha
                && this.iFrameWindow.Aloha.isModified
                && this.iFrameWindow.Aloha.isModified()
            ) {
                this.scriptHost.setContentModified(true);
            }
        }, 1000);
    }

    notifyWhenPageIsModifiedViaJsLib(): void {
        // eslint-disable-next-line prefer-const
        let checkIfPageWasModifiedWhenPageIsUpdated: () => void;

        const pollForGCNObject = () => {
            if (this.iFrameWindow.Aloha && this.iFrameWindow.Aloha.GCN && this.iFrameWindow.Aloha.GCN.page) {
                checkIfPageWasModifiedWhenPageIsUpdated();
                this.observeAllGCNJsLibAjaxRequests();
            } else {
                setTimeout(pollForGCNObject, 50);
            }
        };

        checkIfPageWasModifiedWhenPageIsUpdated = () => {
            const GCN = this.iFrameWindow.Aloha.GCN;

            let originalPageContents = GCN.page._data;
            let originalPageJSON = this.safelyStringifyAlohaPageObject(originalPageContents);

            // eslint-disable-next-line @typescript-eslint/unbound-method
            const originalUpdate = GCN.page._update;
            GCN.page._update = (path, value, error, force) => {
                const returnValue = originalUpdate.call(GCN.page, path, value, error, force);

                if (!this.pageIsSavingFromSaveButton) {
                    let modified = this.editablesChanged;
                    if (!modified) {
                        const pageContents = GCN.page._data;
                        const pageContentsJSON = this.safelyStringifyAlohaPageObject(pageContents);
                        modified = (pageContents !== originalPageContents) || (pageContentsJSON !== originalPageJSON);
                    }

                    this.scriptHost.setContentModified(modified);
                }

                return returnValue;
            };

            this.scriptHost.onPageStartSaving(() => {
                this.pageIsSavingFromSaveButton = true;
            });

            this.scriptHost.onPageSaved(() => {
                this.pageIsSavingFromSaveButton = false;
                this.editablesChanged = false;
                originalPageContents = GCN.page._data;
                originalPageJSON = this.safelyStringifyAlohaPageObject(originalPageContents);
            });
        };

        pollForGCNObject();
    }

    observeAllGCNJsLibAjaxRequests(): void {
        // eslint-disable-next-line @typescript-eslint/unbound-method
        const originalAjax = this.iFrameWindow.Aloha.GCN.page._ajax;
        // eslint-disable-next-line @typescript-eslint/no-this-alias
        const self = this;

        this.iFrameWindow.Aloha.GCN.page._ajax = function wrappedJslibAjax(requestOptions: GCNJsLibRequestOptions): void {
            self.beforeJsLibAjaxRequest(requestOptions);

            // eslint-disable-next-line @typescript-eslint/unbound-method
            const originalSuccess = requestOptions.success;
            requestOptions.success = data => {
                self.afterJsLibAjaxRequest(data, requestOptions);
                return originalSuccess(data);
            };

            return originalAjax.call(this, requestOptions);
        };
    }

    beforeJsLibAjaxRequest(requestOptions: GCNJsLibRequestOptions): void {
        if (/rest\/.+\/save/.test(requestOptions.url)) {
            // A page / tag / object property is about to be saved
            this.scriptHost.setContentModified(true);
        }
    }

    afterJsLibAjaxRequest(data: any, requestOptions: GCNJsLibRequestOptions): void {
        // TODO: If the whole page is saved in an AJAX request, we could run "setContentModified(false)"

        if (!this.pageIsSavingFromSaveButton && /rest\/.+\/save/.test(requestOptions.url)) {
            // A page / tag / object property was saved
            this.scriptHost.setContentModified(true);
        }

        if (/\/rest\/page\/save/.test(requestOptions.url)) {
            this.pageIsSavingFromSaveButton = false;
        }
    }

    /**
     * Safely stringifies a Page object obtained from the Aloha.GCN plugin.
     * This Page object is aumented by the Aloha.GCN plugin and may contain
     * a reference to itself in the pageVariants and possibly the languageVariants arrays.
     */
    private safelyStringifyAlohaPageObject(page: Page): string {
        const copy = {
            ...page,
        };
        delete copy.pageVariants;
        delete copy.languageVariants;
        return JSON.stringify(copy);
    }
}

function isAnchorElement(element: any): element is HTMLAnchorElement {
    return element && element.tagName === 'A';
}

/** Checks for an internal alohapage link and if found, parses that link to extract the pageId and nodeId */
function parseInternalLink(anchor: HTMLAnchorElement): InternalLink | null {

    let attrValue: InternalLink | null = null;

    // Attempt to load the link-data from the attributes first, if present
    if (anchor.hasAttribute(ATTR_REPO) && anchor.getAttribute(ATTR_REPO) === REPO_CMS_ITEM) {
        const objId = anchor.getAttribute(ATTR_OBJECT_ID);
        let nodeId = parseInt(anchor.getAttribute(ATTR_NODE_ID), 10);
        if (isNaN(nodeId)) {
            nodeId = null;
        }

        if (objId && (nodeId == null || Number.isInteger(nodeId))) {
            const split = objId.split('.');
            const type = typeIdsToName(parseInt(split[0], 10));

            if (type) {
                attrValue = {
                    itemId: parseInt(split[1], 10),
                    type: type as any,
                    nodeId: nodeId,
                };
            }
        }
    }

    const href = anchor.getAttribute('href');
    let parsed: URL;
    try {
        parsed = new URL(href, window.location as any);
    } catch (err) {
        return attrValue;
    }

    if (parsed.pathname.startsWith(ALOHAPAGE_URL)) {
        return parseInternalPageLink(anchor, parsed);
    } else if (parsed.pathname.startsWith(FILE_LINK_PREFIX)) {
        return parseInternalFileLink(anchor, parsed);
    }

    return attrValue;
}

function parseInternalPageLink(anchor: HTMLAnchorElement, parsed: URL): InternalLink | null {
    const nodeId = parsed.searchParams.get('nodeid');
    const pageId = parsed.searchParams.get('realid');

    if (!nodeId || !pageId) {
        return null;
    }

    return {
        nodeId: parseInt(nodeId, 10),
        type: 'page',
        itemId: parseInt(pageId, 10),
    };
}

function parseInternalFileLink(anchor: HTMLAnchorElement, parsed: URL): InternalLink | null {
    const id = parseInt(parsed.pathname.substring(FILE_LINK_PREFIX.length + 1), 10);

    if (!Number.isInteger(id)) {
        return null;
    }

    let type = 'file';
    const objId = anchor.getAttribute(ATTR_OBJECT_ID);
    const nodeId = parseInt(parsed.searchParams.get('nodeId'), 10);

    if (objId) {
        const typeId = parseInt(objId.split('.')[0], 10);
        type = typeIdsToName(typeId) || type;
    }

    return {
        nodeId,
        type: type as any,
        itemId: id,
    };
}
