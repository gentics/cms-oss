/* eslint-disable @typescript-eslint/no-this-alias */
/* eslint-disable @typescript-eslint/naming-convention */
/* eslint-disable no-underscore-dangle */
import { AlohaEditable, AlohaRangeObject, AlohaSettings } from '@gentics/aloha-models';
import { EditMode, GcmsUiBridge } from '@gentics/cms-integration-api-models';
import { Page, StringTagPartProperty, Tag, TagPropertyType } from '@gentics/cms-models';
import { getExamplePageData } from '@gentics/cms-models/testing/test-data.mock';
import { Subscription } from 'rxjs';
import { SpyEventTarget } from '../../../../../testing/spy-event-target';
import {
    AlohaGlobal,
    CNIFrameDocument,
    CNWindow,
    GCNJSLib,
    GCNJsLibRequestOptions,
    GCNRestRequestArgs,
} from '../../../models/content-frame';
import { CustomScriptHostService } from '../../../providers/custom-script-host/custom-script-host.service';
import { PostLoadScript } from './post-load';
import { PreLoadScript } from './pre-load';

describe('custom scripts', () => {

    let fixture: CustomScriptsTestFixture;
    afterEach(() => fixture && fixture.destroy());

    it('can create a CustomScriptsTestFixture', () => {
        fixture = new CustomScriptsTestFixture();
    });

    it('can run pre-load scripts', () => {
        fixture = new CustomScriptsTestFixture();
        fixture.runPreLoadScript();
    });

    it('can run post-load scripts', () => {
        fixture = new CustomScriptsTestFixture();
        fixture.runPostLoadScript();
    });

    describe('Link handling', () => {

        describe('Link handling with special test preparations', () => {

            // The tests in this describe() require the registration of an additional click event listener
            // to prevent the opening of a nested karma instance in the testing IFrame (which would result in a test loop).
            // We need this because the left clicks done in these tests need to trigger an event handler in post-load.ts,
            // which should prevent navigation without using event.preventDefault().

            let defaultPrevented: boolean;
            const clickEventListener = (event: MouseEvent) => {
                // Store the original defaultPrevented value (which should be false)
                // and then prevent the default action.
                defaultPrevented = event.defaultPrevented;
                event.preventDefault();
            };

            beforeEach(() => {
                defaultPrevented = undefined;
                window.addEventListener('click', clickEventListener);
            });

            afterEach(() => {
                window.removeEventListener('click', clickEventListener);
            });

            it('ignores "#" links', () => {
                fixture = CustomScriptsTestFixture.withPagePreview();
                const link = fixture.addLinkWithHashURL();

                link.leftClick();
                link.middleClick();
                link.ctrlClick();
                link.shiftClick();

                expect(defaultPrevented).toBe(false);
                expect(fixture.window.open).not.toHaveBeenCalled();
                expect(fixture.scriptHost.navigateToPagePreview).not.toHaveBeenCalled();
            });

            it('ignores "#something" links', () => {
                fixture = CustomScriptsTestFixture.withPagePreview();
                const link = fixture.addLinkWithAnchorURL();

                link.leftClick();
                link.middleClick();
                link.ctrlClick();
                link.shiftClick();

                expect(defaultPrevented).toBe(false);
                expect(fixture.window.open).not.toHaveBeenCalled();
                expect(fixture.scriptHost.navigateToPagePreview).not.toHaveBeenCalled();
            });
        });

        it('prevents regular clicks to internal links (edit-mode)', () => {
            fixture = CustomScriptsTestFixture.withPageEditMode();
            let link = fixture.addLinkToDifferentPageInEditMode();
            const event = link.leftClick();
            link.middleClick();
            link.shiftClick();

            expect(fixture.eventPreventedByCustomScript).toBe(true);
            expect(event.defaultPrevented).toBe(true);
            expect(fixture.window.open).not.toHaveBeenCalled();
            expect(fixture.scriptHost.navigateToPagePreview).not.toHaveBeenCalled();

            link = fixture.addInternalLinkWithTargetBlank();
            link.leftClick();
            link.middleClick();
            link.shiftClick();

            expect(fixture.eventPreventedByCustomScript).toBe(true);
            expect(fixture.window.open).not.toHaveBeenCalled();
            expect(fixture.scriptHost.navigateToPagePreview).not.toHaveBeenCalled();
        });

        it('navigates in the UI when ctrl-clicking links to other pages (edit-mode)', () => {
            fixture = CustomScriptsTestFixture.withPageEditMode();
            const link = fixture.addLinkToDifferentPage();
            const event = link.ctrlClick();

            expect(event.defaultPrevented).toBe(true);
            expect(fixture.window.open).not.toHaveBeenCalled();
            expect(fixture.scriptHost.navigateToPagePreview).toHaveBeenCalledWith(
                fixture.linkToOtherPage.nodeId,
                fixture.linkToOtherPage.pageId,
            );
        });

        it('prevents regular clicks to external links (edit-mode)', () => {
            fixture = CustomScriptsTestFixture.withPageEditMode();
            const link = fixture.addExternalLinkWithTargetBlank();
            link.leftClick();
            link.shiftClick();

            expect(fixture.eventPreventedByCustomScript).toBe(true);
            expect(fixture.window.open).not.toHaveBeenCalled();
            expect(fixture.scriptHost.navigateToPagePreview).not.toHaveBeenCalled();
        });

        it('navigates in the UI when clicking links to other pages (preview-mode)', () => {
            fixture = CustomScriptsTestFixture.withPagePreview();
            const link = fixture.addLinkToDifferentPage();
            const event = link.leftClick();

            expect(event.defaultPrevented).toBe(true);
            expect(fixture.window.open).not.toHaveBeenCalled();
            expect(fixture.scriptHost.navigateToPagePreview).toHaveBeenCalledWith(
                fixture.linkToOtherPage.nodeId,
                fixture.linkToOtherPage.pageId,
            );
        });

        it('opens external links in new page when clicked via middle mouse button (edit-mode)', () => {
            fixture = CustomScriptsTestFixture.withPageEditMode();
            const link = fixture.addExternalLinkWithTargetBlank();
            const event = link.middleClick();

            expect(event.defaultPrevented).toBe(true);
            expect(fixture.window.open).toHaveBeenCalledWith(fixture.ALOHAPAGE_URL_OF_EXTERNAL_PAGE, '_blank');
            expect(fixture.scriptHost.navigateToPagePreview).not.toHaveBeenCalled();
        });

        it('opens external links in new page when clicked via ctrl+click (edit-mode)', () => {
            fixture = CustomScriptsTestFixture.withPageEditMode();
            const link = fixture.addExternalLinkWithTargetBlank();
            const event = link.ctrlClick();

            expect(event.defaultPrevented).toBe(true);
            expect(fixture.window.open).toHaveBeenCalledWith(fixture.ALOHAPAGE_URL_OF_EXTERNAL_PAGE, '_blank');
            expect(fixture.scriptHost.navigateToPagePreview).not.toHaveBeenCalled();
        });

        it('opens external links in new page when left-clicked (preview-mode)', () => {
            fixture = CustomScriptsTestFixture.withPagePreview();
            const link = fixture.addExternalLinkWithTargetBlank();
            const event = link.leftClick();

            expect(event.defaultPrevented).toBe(true);
            expect(fixture.window.open).toHaveBeenCalledWith(fixture.ALOHAPAGE_URL_OF_EXTERNAL_PAGE, '_blank');
            expect(fixture.scriptHost.navigateToPagePreview).not.toHaveBeenCalled();
        });

        it('opens external links in new page when clicked via middle mouse button (preview-mode)', () => {
            fixture = CustomScriptsTestFixture.withPagePreview();
            const link = fixture.addExternalLinkWithTargetBlank();
            const event = link.middleClick();

            expect(event.defaultPrevented).toBe(true);
            expect(fixture.window.open).toHaveBeenCalledWith(fixture.ALOHAPAGE_URL_OF_EXTERNAL_PAGE, '_blank');
            expect(fixture.scriptHost.navigateToPagePreview).not.toHaveBeenCalled();
        });

        it('opens external links in new page when clicked via ctrl+click (preview-mode)', () => {
            fixture = CustomScriptsTestFixture.withPagePreview();
            const link = fixture.addExternalLinkWithTargetBlank();
            const event = link.ctrlClick();

            expect(event.defaultPrevented).toBe(true);
            expect(fixture.window.open).toHaveBeenCalledWith(fixture.ALOHAPAGE_URL_OF_EXTERNAL_PAGE, '_blank');
            expect(fixture.scriptHost.navigateToPagePreview).not.toHaveBeenCalled();
        });

        it('changes the URL of links when right-clicked by the user', () => {
            fixture = CustomScriptsTestFixture.withPagePreview();
            const link = fixture.addLinkToDifferentPage();
            const event = link.rightClick();

            expect(event.defaultPrevented).toBe(false);
            expect(link.hasDifferentHref).toBe(true);
        });

        it('reverts the URL of links when the user closes their context menu', () => {
            fixture = CustomScriptsTestFixture.withPagePreview();
            const link = fixture.addLinkToDifferentPage();
            link.rightClick();
            link.blur();

            expect(link.hasDifferentHref).toBe(false);
        });

    });

    describe('updating page tags via the GCNJSLib', () => {

        it('enables the save button when tags change', () => {
            fixture = CustomScriptsTestFixture.withPageEditMode();
            fixture.updateTagViaGCNJSLib();
            expect(fixture.reportsContentModified).toBe(true);
        });

        it('does not enable the save button when no tags are changed', () => {
            fixture = CustomScriptsTestFixture.withPageEditMode();
            fixture.updateTagToCurrentValuesViaGCNJSLib();
            expect(fixture.reportsContentModified).toBe(false);
        });

        it('enables the save button when object properties change', () => {
            fixture = CustomScriptsTestFixture.withPageEditMode();
            fixture.updateObjectPropertyViaGCNJSLib();
            expect(fixture.reportsContentModified).toBe(true);
        });

    });

});

class CustomScriptsTestFixture {

    public readonly ALOHAPAGE_URL_OF_OTHER_PAGE = '/alohapage?nodeid=1&language=2&sid=123&real=newview&realid=47';
    public readonly ALOHAPAGE_URL_OF_EXTERNAL_PAGE = '/some/other/page';
    readonly linkToOtherPage = { nodeId: 1, pageId: 47 };
    readonly urlOfInternalLink = 'internal link (node 1 page 47)';

    window: FakeWindow;
    document: FakeDocument;
    scriptHost: FakeScriptHost;
    eventPreventedByCustomScript = false;

    private subscription = new Subscription();

    static withPagePreview(): CustomScriptsTestFixture {
        const fixture = new CustomScriptsTestFixture();
        fixture.pretendToBePreviewingAPage();
        fixture.attachToBodyAndRunPreAndPostLoadScripts();
        return fixture;
    }

    static withPageEditMode(): CustomScriptsTestFixture {
        const fixture = new CustomScriptsTestFixture();
        fixture.pretendToBeEditingAPage();
        fixture.attachToBodyAndRunPreAndPostLoadScripts();
        return fixture;
    }

    constructor() {
        this.createFakeWindow();
        this.createFakeScriptHost();
    }

    destroy(): void {
        this.subscription.unsubscribe();
    }

    runPreLoadScript(): void {
        const script = new PreLoadScript(
            this.window as any as CNWindow,
            this.document as any as CNIFrameDocument,
        );
        script.run();
    }

    runPostLoadScript(): void {
        const script = new PostLoadScript(
            this.window as any as CNWindow,
            this.document as any as CNIFrameDocument,
            this.scriptHost as any as CustomScriptHostService,
            null,
        );
        script.run();
    }

    addLink(attributes: { href?: string; target?: string; role?: string }): LinkTestFixture {
        const linkFixture = LinkTestFixture.withAttributes(attributes);
        this.document.body.appendChild(linkFixture.nativeElement);
        this.subscription.add(() => this.document.body.removeChild(linkFixture.nativeElement));
        return linkFixture;
    }

    addLinkWithHashURL(): LinkTestFixture {
        return this.addLink({ href: '#' });
    }

    addLinkWithAnchorURL(): LinkTestFixture {
        return this.addLink({ href: '#newtab' });
    }

    addLinkToDifferentPage(): LinkTestFixture {
        const href = this.ALOHAPAGE_URL_OF_OTHER_PAGE;
        return this.addLink({ href });
    }

    addLinkToDifferentPageInEditMode(): LinkTestFixture {
        const href = this.ALOHAPAGE_URL_OF_OTHER_PAGE;
        return this.addLink({ href }).inEditMode();
    }

    addInternalLinkWithTargetBlank(): LinkTestFixture {
        const href = this.ALOHAPAGE_URL_OF_OTHER_PAGE;
        const target = '_blank';
        return this.addLink({ href, target });
    }

    addExternalLinkWithTargetBlank(): LinkTestFixture {
        const href = this.ALOHAPAGE_URL_OF_EXTERNAL_PAGE;
        const target = '_blank';
        return this.addLink({ href, target });
    }

    updateTagViaGCNJSLib(): void {
        this.window.Aloha.GCN.page.tag('tag1', (tag) => {
            tag.part('text', 'changed text');
        });
    }

    updateTagToCurrentValuesViaGCNJSLib(): void {
        // eslint-disable-next-line no-underscore-dangle
        const currentText = (this.window.Aloha.GCN.page._data.tags.tag1.properties.text as StringTagPartProperty).stringValue;
        this.window.Aloha.GCN.page.tag('tag1', (tag) => {
            tag.part('text', currentText);
        });
    }

    updateObjectPropertyViaGCNJSLib(): void {
        this.window.Aloha.GCN.page.tag('object.prop1', (tag) => {
            tag.part('value', 'changed value');
        });
    }

    get reportsContentModified(): boolean {
        return this.scriptHost.mostRecentModifiedValue;
    }

    private createFakeWindow(): void {
        this.window = new FakeWindow() as any;
        this.document = this.window.document;
        this.subscription.add(() => this.window.destroy());
    }

    private createFakeScriptHost(): void {
        this.scriptHost = new FakeScriptHost();
    }

    private attachToBodyAndRunPreAndPostLoadScripts(): void {
        this.attachTestDocumentToRealBody();
        this.runPreLoadScript();
        this.runPostLoadScript();
        this.preventLinkClicksFromNavigating();
    }

    private attachTestDocumentToRealBody(): void {
        const element = this.document.documentElement;
        document.body.appendChild(element);
        this.subscription.add(() => document.body.removeChild(element));
    }

    private pretendToBePreviewingAPage(): void {
        this.scriptHost.editMode = EditMode.PREVIEW;
    }

    private pretendToBeEditingAPage(): void {
        this.scriptHost.editMode = EditMode.EDIT;
    }

    private appendHtmlToBody(html: string): void {
        const container = document.createElement('div');
        container.innerHTML = html;
        const fragment = document.createDocumentFragment();
        while (container.firstChild) {
            fragment.appendChild(container.firstChild);
        }
        this.document.body.appendChild(fragment);
    }

    private preventLinkClicksFromNavigating(): void {
        this.document.body.addEventListener('click', (event) => {
            const link = event.target as HTMLLinkElement;
            const href = link.getAttribute('href');

            if (link.tagName === 'A' && /^\//.test(href)) {
                if (event.defaultPrevented) {
                    this.eventPreventedByCustomScript = true;
                } else {
                    event.preventDefault();
                }
            }
        });
    }
}

class FakeWindow extends SpyEventTarget {
    // tslint:disable variable-name
    Aloha = new FakeAlohaGlobal();
    document = new FakeDocument();
    jQuery: FakeJQuery;
    location = {
        href: '',
    };

    name: string;
    frameElement = {
        id: '',
        dataset: {} as DOMStringMap,
    };

    JSI3_objprop_new_0 = ['JSI3_objprop_new_0'];
    JSI3_objprop_new_1 = ['JSI3_objprop_new_1'];
    GCMSUI = new FakeGCMSUI();

    private subscriptions = new Subscription();

    constructor() {
        super('window');
        spyOn(this, 'hopedit' as any);
        spyOn(this, 'open' as any);
        spyOn(this, 'displayContextMenu' as any);
        this.subscriptions.add(() => this.document.destroy());
    }

    destroy(): void {
        this.subscriptions.unsubscribe();
    }

    hopedit(url: string, w: number, h: number): void { }

    setTimeout(handler: Function, timeout: number): number {
        const handle = setTimeout(handler, timeout);
        this.subscriptions.add(() => clearTimeout(handle));
        return handle;
    }

    clearTimeout(handle: number): void {
        return clearTimeout(handle);
    }

    setInterval(handler: Function, timeout: number): number {
        const handle = setInterval(handler, timeout);
        this.subscriptions.add(() => clearInterval(handle));
        return handle;
    }

    clearInterval(handle: number): void {
        return clearInterval(handle);
    }

    open(url?: string, target?: string): void { }

    displayContextMenu(layerName: string, menuItems: any[][], event: MouseEvent, depth?: number): void {}
}

class FakeDocument {
    documentElement: HTMLElement;
    body: HTMLElement;
    tagfill: HTMLFormElement;
    private subscriptions = new Subscription();

    constructor() {
        this.documentElement = document.createElement('test-fixture-html');
        this.body = document.createElement('test-fixture-body');
        this.documentElement.appendChild(this.body);
    }

    destroy(): void {
        this.subscriptions.unsubscribe();
    }

    addEventListener(eventName: string, handler: () => any, useCapture: boolean = false): void {
        this.documentElement.addEventListener(eventName, handler, useCapture);
        this.subscriptions.add(() => this.documentElement.removeEventListener(eventName, handler, useCapture));
    }

    createElement<K extends keyof HTMLElementTagNameMap, R extends HTMLElementTagNameMap[K]>(tagName: K): R {
        return document.createElement(tagName) as R;
    }

    querySelector(selector: string): HTMLElement {
        return this.documentElement.querySelector(selector);
    }

    querySelectorAll(selector: string): NodeListOf<HTMLElement> {
        return this.documentElement.querySelectorAll(selector);
    }
}

class FakeAlohaGlobal implements AlohaGlobal {
    settings: AlohaSettings;
    trigger(eventName: string, data: any): void { }
    activeEditable?: AlohaEditable;
    getEditableById(id: string | number): AlohaEditable { return null; }
    getEditableHost($element: JQuery): AlohaEditable | null {
        return null;
    }

    jQuery: JQueryStatic;
    scrollToSelection(): void {}

    GCN = new FakeGCNJSLib();

    Selection = {
        getRangeObject(): AlohaRangeObject { return null; },
        updateSelection(): void { },
        SelectionRange: null,
    };

    bind(): void { }
    unbind(): void { }
    ready(fn: () => void): void {
        fn?.();
    }

    require(dependency: string): any;
    require(dependencies: string[], callback: (...dependencies: any[]) => any): void;
    require(dependencies: string | string[], callback?: (...dependencies: any[]) => any): void { }

    isModified(): boolean {
        return false;
    }
}

class FakeGCNJSLib implements GCNJSLib {
    page: GCNJSLib['page'];
    pendingJsLibAjaxRequests: GCNJsLibRequestOptions[] = [];

    constructor() {
        this.page = this.buildPageObject();
    }

    private buildPageObject(): this['page'] {
        const self = this;
        const pageObject = {
            _data: this.buildExamplePageWithTags(),
            _shadow: {},
            _ajax(options: GCNJsLibRequestOptions): void {
                self.pendingJsLibAjaxRequests.push(options);
            },
            _update(path: string, value: any, error?: any, force?: boolean): void {
                const [tag, property] = path.split('/');
                self.page._data.tags[tag].properties[property] = {
                    ...self.page._data.tags[tag].properties[property],
                    stringValue: value,
                } as StringTagPartProperty;
            },
            tag(tagName: string, callback: (tag: FakeGCNJSLibTag) => void): void {
                const tag = new FakeGCNJSLibTag(self, tagName);
                callback(tag);
            },
        };
        return pageObject;
    }

    private buildExamplePageWithTags(): Page {
        const tag1: Tag = {
            active: true,
            constructId: 1,
            id: 1,
            name: 'tag1',
            rootTag: false,
            inherited: false,
            properties: {
                text: {
                    id: 2,
                    partId: 2,
                    type: TagPropertyType.STRING,
                    stringValue: 'text before',
                },
            },
            type: 'CONTENTTAG',
        };

        const objectProperty1: Tag = {
            active: true,
            constructId: 3,
            id: 3,
            name: 'object.prop1',
            rootTag: false,
            inherited: false,
            properties: {
                value: {
                    id: 4,
                    partId: 4,
                    stringValue: 'value before',
                    type: TagPropertyType.STRING,
                },
            },
            type: 'OBJECTTAG',
        };

        return {
            ...getExamplePageData({ id: 123 }),
            tags: {
                tag1,
                'object.prop1': objectProperty1,
            },
        };
    }

    performRESTRequest(config: GCNRestRequestArgs): void {
        // TODO
    }

    savePage(options: {
        createVersion?: boolean;
        unlock?: boolean;
        onsuccess(returnValue: Page): void;
        onfailure(error: Error): void;
    }): void {
        // TODO: Call update for every property
    }
}

class FakeGCNJSLibTag {

    constructor(
        private _parent: GCNJSLib,
        private _tagName: string) { }

    edit(): void { }
    parent(): GCNJSLib['page'] {
        return this._parent.page;
    }

    part(name: string, value: any): void {
        // TODO: find correct value for path
        return this._parent.page._update(this._tagName + '/' + name, value);
    }

    parts(name: string): void { }
    prop(name: string, value: any): void { }
    remove(): void { }
    render(): void { }
    save(): void {
        // TODO
    }
}

class FakeJQuery {

}

class FakeScriptHost {
    mostRecentModifiedValue: boolean;
    mostRecentPropertyModifiedValue: boolean;
    editMode: EditMode = EditMode.PREVIEW;
    onSaveObjectPropertyHandler: () => void;
    private pageSavedHandler = (): void => {};
    private pageStartsSavingHandler = (): void => {};

    constructor() {
        spyOn(this, 'navigateToPagePreview' as any);
        spyOn(this, 'runChangeDetection' as any);
        spyOn(this, 'setContentModified' as any).and.callThrough();
        spyOn(this, 'setObjectPropertyModified' as any).and.callThrough();
        spyOn(this, 'getInternalLinkUrlToPagePreview' as any).and.callThrough();
    }

    getInternalLinkUrlToPagePreview(nodeId: number, itemType: string, pageId: number): string {
        return `internal link (node ${nodeId} ${itemType} ${pageId})`;
    }

    getTranslation(key: string): string {
        return `translated(${key})`;
    }

    navigateToPagePreview(nodeId: number, pageId: number): void { }

    onSaveObjectProperty(handler: () => void): void {
        this.onSaveObjectPropertyHandler = handler;
    }

    runChangeDetection(): void { }

    setContentModified(modified: boolean): void {
        this.mostRecentModifiedValue = modified;
    }

    setObjectPropertyModified(modified: boolean): void {
        this.mostRecentPropertyModifiedValue = modified;
    }

    onPageSaved(handler: () => void): void {
        this.pageSavedHandler = handler;
    }

    pageWasSaved(): void {
        this.pageSavedHandler();
    }

    onPageStartSaving(handler: () => void): void {
        this.pageStartsSavingHandler = handler;
    }

    pageStartsSaving(): void {
        this.pageStartsSavingHandler();
    }

}

class FakeGCMSUI implements Partial<GcmsUiBridge> {
    gcmsUiStylesUrl = 'gcmsUiStyles';
}

class ClickableElementFixture<T extends Element> {

    static from<T extends Element>(nativeElement: T): ClickableElementFixture<T> {
        return new ClickableElementFixture(nativeElement);
    }

    static create<K extends keyof HTMLElementTagNameMap>(
        tagName: K, attributes: { [name: string]: string },
    ): ClickableElementFixture<HTMLElementTagNameMap[K]> {
        const element = document.createElement(tagName);
        for (const name of Object.keys(attributes)) {
            element.setAttribute(name, attributes[name]);
        }
        const fixture = new ClickableElementFixture<HTMLElementTagNameMap[K]>(element);
        return fixture;
    }

    protected constructor(public nativeElement: T) { }

    get isVisibleToUser(): boolean {
        const element = this.nativeElement as any as HTMLElement;
        const style = element && element.style;
        if (!element) {
            return false;
        } else if (style && ((style.display === 'none') || (style.visibility === 'hidden'))) {
            return false;
        } else if (getComputedStyle(element).display === 'none') {
            return false;
        } else if (!element.parentElement) {
            return false;
        } else {
            return (element.offsetWidth * element.offsetHeight) > 0;
        }
    }

    leftClick(): MouseEvent {
        return this.triggerMouseEvent('click', { button: 0, detail: 1 });
    }

    middleClick(): MouseEvent {
        return this.triggerMouseEvent('auxclick', { button: 1, detail: 2 });
    }

    rightClick(): MouseEvent {
        return this.triggerMouseEvent('contextmenu', { });
    }

    blur(): Event {
        return this.triggerEvent('blur');
    }

    ctrlClick(): MouseEvent {
        return this.triggerMouseEvent('click', { button: 0, detail: 1, ctrlKey: true });
    }

    shiftClick(): MouseEvent {
        return this.triggerMouseEvent('click', { button: 0, detail: 1, shiftKey: true });
    }

    private createMouseEvent(typeArg: string, { detail, button, ctrlKey, shiftKey, altKey }: Partial<MouseEventInit>): MouseEvent {
        const canBubbleArg = true;
        const cancelableArg = true;
        const viewArg = this.nativeElement.ownerDocument.defaultView;
        const screenXArg = 0;
        const screenYArg = 0;
        const clientXArg = 0;
        const clientYArg = 0;
        const metaKeyArg = false;
        const relatedTargetArg: EventTarget = undefined;

        return new MouseEvent(typeArg, {
            bubbles: canBubbleArg,
            cancelable: cancelableArg,
            view: viewArg,
            detail: detail,
            screenX: screenXArg,
            screenY: screenYArg,
            clientX: clientXArg,
            clientY: clientYArg,
            ctrlKey: ctrlKey || false,
            altKey: altKey || false,
            shiftKey: shiftKey || false,
            metaKey: metaKeyArg,
            button: button,
            relatedTarget: relatedTargetArg,
        });
    }

    private triggerMouseEvent(type: string, { detail, button, ctrlKey, shiftKey, altKey }: Partial<MouseEventInit>): MouseEvent {
        const event = this.createMouseEvent(type, { detail, button, ctrlKey, shiftKey, altKey });
        this.nativeElement.dispatchEvent(event);
        return event;
    }

    private triggerEvent(type: string): Event {
        const event = document.createEvent('event');
        event.initEvent(type, true, true);
        this.nativeElement.dispatchEvent(event);
        return event;
    }
}

class LinkTestFixture extends ClickableElementFixture<HTMLAnchorElement> {

    private originalHref: string;

    static withAttributes(attributes: { href?: string; target?: string; role?: string }): LinkTestFixture {
        const element = document.createElement('a');
        for (const name of Object.keys(attributes)) {
            element.setAttribute(name, attributes[name as 'href' | 'target' | 'role']);
        }
        return new LinkTestFixture(element);
    }

    constructor(nativeElement: HTMLAnchorElement) {
        super(nativeElement);
        this.originalHref = this.href;
    }

    get href(): string {
        return this.nativeElement.getAttribute('href');
    }

    get hasDifferentHref(): boolean {
        return this.href !== this.originalHref;
    }

    inEditMode(): this {
        this.nativeElement.dataset.gcnI18nConstructname = 'construct name';

        return this;
    }

}
