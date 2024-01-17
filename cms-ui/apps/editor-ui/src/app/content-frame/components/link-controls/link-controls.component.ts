import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { RepositoryBrowserClient } from '@editor-ui/app/shared/providers';
import { ApplicationStateService } from '@editor-ui/app/state';
import { AlohaBlockManager, AlohaDOM, AlohaLinkPlugin } from '@gentics/aloha-models';
import { nameToTypeId, typeIdsToName } from '@gentics/cms-components';
import {
    AllowedItemSelectionType,
    AllowedSelectionType,
    EditMode,
    ExternalLink,
    File,
    Image,
    ItemRequestOptions,
    LinkCheckerCheckResponse,
    NodeFeature,
    Page,
    ResponseCode,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { InputComponent, cancelEvent, waitTake } from '@gentics/ui-core';
import { BehaviorSubject, Observable, Subscription, combineLatest, of } from 'rxjs';
import { distinctUntilChanged, filter, map, mergeMap, switchMap, tap } from 'rxjs/operators';
import {
    COMMAND_LINK,
    INLINE_LINK_ANCHOR_ATTRIBUTE,
    INLINE_LINK_CLASS,
    INLINE_LINK_HREF_ATTRIBUTE,
    INLINE_LINK_LANGUAGE_ATTRIBUTE,
    INLINE_LINK_OBJECT_ID_ATTRIBUTE,
    INLINE_LINK_OBJECT_NODE_ATTRIBUTE,
    INLINE_LINK_TARGET_ATTRIBUTE,
    INLINE_LINK_TITLE_ATTRIBUTE,
    INLINE_LINK_URL_ATTRIBUTE,
    LINK_NODE_NAME,
    LINK_TARGET_NEW_TAB,
    NODE_NAME_TO_COMMAND,
} from '../../../common/models/aloha-integration';
import { BaseControlsComponent } from '../base-controls/base-controls.component';

enum LinkCheckerStatus {
    VALID = 'valid',
    INVALID = 'invalid',
    LOADING = 'loading',
}

interface LinkCheckerSettings {
    enabled: boolean;
    status: LinkCheckerStatus;
    links?: ExternalLink[];
}

const ALLOWED_SELECTION_TYPES: AllowedSelectionType[] = [
    'contenttag',
    'file',
    'folder',
    'form',
    'image',
    'page',
    'template',
    'templatetag',
];

/** Hardcoded repository ID to let the (gcn)link-plugin understand the item correctly. */
const ALOHA_REPOSITORY_ID = 'com.gentics.aloha.GCN.Page';

@Component({
    selector: 'gtx-link-controls',
    templateUrl: './link-controls.component.html',
    styleUrls: ['./link-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LinkControlsComponent extends BaseControlsComponent implements OnInit, OnChanges, OnDestroy {

    public readonly LinkCheckerStatus = LinkCheckerStatus;

    @Output()
    public activeChange = new EventEmitter<boolean>();

    /** The input where the user can enter the URL. */
    @ViewChild('targetInput')
    public inputTargetElement?: InputComponent;

    /** If currently a link is focused adn therefore the options for it should be displayed. */
    public active = false;
    /** If it's allowed to use links in the current context. */
    public allowed = false;

    /** The anchor of the target. */
    public anchor = '';
    /** If it should open the link in a new tab. */
    public newTab = false;
    /** The title of the link. */
    public title = '';
    /** The language of the link. */
    public language = '';

    /** Flag to determine if it is a user defined URL or an Element picked from the CMS. */
    public isTargetObject = false;
    /** The URL of the link. Only set when manually entered. */
    public targetUrl = '';
    /** The element picked from the CMS. */
    public targetObject: Image | Page | File | null = null;
    /** If it is currently fetching the item from the CMS to display it. */
    public loadingObject = false;

    /** Infos of the link-checker */
    public linkChecker: LinkCheckerSettings = {
        enabled: false,
        status: LinkCheckerStatus.LOADING,
    };

    /** The currently selected Link Element in the iFrame. */
    protected currentElement: HTMLAnchorElement | null;
    /** Subject for the links to check. */
    protected linkToCheck = new BehaviorSubject<string>(null);

    /** Instance of the link-plugin from the iFrame. */
    protected plugin: AlohaLinkPlugin;
    protected alohaDom: AlohaDOM;
    protected blockManager: AlohaBlockManager;

    /** Subscription which loads the picked item from the CMS. */
    protected internalRefLoader: Subscription | null;
    /** Subscription for the link-checker loading. */
    protected checkLoader: Subscription | null;

    constructor(
        changeDetector: ChangeDetectorRef,
        protected client: GCMSRestClientService,
        protected i18n: I18nService,
        protected repositoryBrowser: RepositoryBrowserClient,
        protected appState: ApplicationStateService,
    ) {
        super(changeDetector);
    }

    public ngOnInit(): void {
        const checkerEnabled$: Observable<boolean> = this.appState.select(state => state.editor.nodeId).pipe(
            mergeMap(nodeId => this.appState.select(state => state.features.nodeFeatures[nodeId])),
            map(([nodeFeatures]) => (nodeFeatures || []).includes(NodeFeature.LINK_CHECKER)),
        );

        this.subscriptions.push(checkerEnabled$.subscribe(enabled => {
            this.linkChecker.enabled = enabled;
            this.changeDetector.markForCheck();
        }))

        this.subscriptions.push(checkerEnabled$.pipe(
            filter(enabled => enabled),
            mergeMap(() => this.appState.select(state => state.editor).pipe(
                filter(editor => editor.editMode === EditMode.EDIT && editor.itemType === 'page'),
                switchMap(editor => this.client.linkChecker.pageLinks(editor.itemId)),
            )),
        ).subscribe(status => {
            this.linkChecker.links = status.items
                .filter(link => link.lastStatus === 'invalid');
            this.changeDetector.markForCheck();
        }));

        this.checkLoader = combineLatest([
            checkerEnabled$,
            this.linkToCheck.asObservable().pipe(
                distinctUntilChanged(),
            ),
        ]).pipe(
            filter(([enabled, link]) => enabled && link != null),
            tap(() => {
                this.linkChecker.status = LinkCheckerStatus.LOADING;
                this.changeDetector.markForCheck();
            }),
            waitTake(500),
            switchMap(([, link]) => {
                if (link.trim().length === 0) {
                    return of<LinkCheckerCheckResponse>({
                        messages: [],
                        reason: '',
                        responseInfo: {
                            responseCode: ResponseCode.OK,
                        },
                        valid: true,
                    });
                }
                return this.client.linkChecker.checkLink(link);
            }),
        ).subscribe(status => {
            this.linkChecker.status = status.valid ? LinkCheckerStatus.VALID : LinkCheckerStatus.INVALID;
            this.changeDetector.markForCheck();
        }, () => {
            this.linkChecker.status = LinkCheckerStatus.INVALID;
            this.changeDetector.markForCheck();
        });
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.aloha) {
            this.plugin = this.safeRequire('link/link-plugin');
            this.alohaDom = this.safeRequire('util/dom');
            this.blockManager = this.safeRequire('block/block-manager');
        }
    }

    public override ngOnDestroy(): void {
        this.clearLinkLoader();
        if (this.checkLoader) {
            this.checkLoader.unsubscribe();
            this.checkLoader = null;
        }
        super.ngOnDestroy();
    }

    public toggleActive(): void {
        // Make the current selection a link. We do this with the Link-Plugin.
        if (!this.active) {
            this.insertLinkAtCurrentLocation();
        } else {
            this.removeLinkAtCurrentLocation();
        }
    }

    public insertLinkAtCurrentLocation(): void {
        if (!this.range || !this.range.markupEffectiveAtStart) {
            return;
        }

        const res = this.plugin.insertLink(false);
        // Only newer versions have the boolean return type
        if (typeof res === 'boolean') {
            // If it's false, then it means the link couldn't be created
            if (!res) {
                return;
            }
        }

        this.range = this.aloha.Selection.getRangeObject();
        this.forwardStateToIFrame();
        this.updateActive(true);

        // Has to be ugly delayed, because it's only available after we flip it to active.
        // After a new link has been created, focus the input element, as the button is still active,
        // which isn't what we want.
        setTimeout(() => {
            if (this.inputTargetElement) {
                this.inputTargetElement.inputElement?.nativeElement?.focus();
            }
        });
    }

    public removeLinkAtCurrentLocation(): void {
        this.plugin.removeLink();
        this.updateActive(false);
    }

    public selectionOrEditableChanged(): void {
        if (!this.plugin || !this.range || !this.range.markupEffectiveAtStart || !this.aloha.activeEditable?.obj) {
            this.currentElement = null;
            this.allowed = false;
            this.updateActive(false);
            this.changeDetector.markForCheck();
            return;
        }

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.allowed = this.aloha?.activeEditable?.obj != null && (this.plugin.getEditableConfig(this.aloha.activeEditable.obj) || [])
            .filter(nodeName => this.contentRules.isAllowed(this.aloha.activeEditable?.obj, nodeName))
            .map((nodeName: string) => NODE_NAME_TO_COMMAND[nodeName.toUpperCase()])
            .filter(val => val != null)
            .includes(COMMAND_LINK);

        let foundElement: HTMLAnchorElement | null = null;

        for (const elem of this.range.markupEffectiveAtStart) {
            // Only enable the link handling when the plugin is available
            if (elem.nodeName !== LINK_NODE_NAME || (
                !elem.classList.contains(INLINE_LINK_CLASS)
                && elem.getAttribute(INLINE_LINK_URL_ATTRIBUTE) == null
            )) {
                continue;
            }

            foundElement = elem as HTMLAnchorElement;
            break;
        }

        if (!foundElement) {
            this.currentElement = null;
        } else if (foundElement !== this.currentElement) {
            this.currentElement = foundElement;
            this.updateActive(true);
            this.loadDataFromElement();
        }

        if (this.currentElement == null) {
            this.updateActive(false);
        }
    }

    public focusLink(link: ExternalLink, event?: MouseEvent): void {
        cancelEvent(event);

        if (this.disabled || !this.aloha || !this.alohaDom) {
            return;
        }

        const $contentTag = this.aloha.jQuery(`[data-gcn-tagid="${link.contenttagId}"]`);
        if (!$contentTag.length) {
            return;
        }

        const $host = this.aloha.jQuery(this.alohaDom.getEditingHostOf($contentTag.get(0)));
        const editable = this.aloha.getEditableById($host.attr('id'));
        const blockId = $contentTag.eq(0).attr('id');
        if (editable) {
            editable.activate();
            this.alohaDom.setCursorInto($contentTag.get(0));
            this.aloha.scrollToSelection();
            this.alohaDom.selectDomNode($contentTag.get(0));
            this.aloha.Selection.updateSelection();
        } else {
            if (this.blockManager && blockId) {
                const block = this.blockManager.getBlock(blockId);
                if (block) {
                    block.activate(block.$element);
                }
            }
            $(window.document).scrollTop($contentTag.offset().top);
        }
    }

    protected loadDataFromElement(): void {
        this.clearLinkLoader();

        // Set the common/easy properties
        this.anchor = this.currentElement.getAttribute(INLINE_LINK_ANCHOR_ATTRIBUTE) || '';
        this.language = this.currentElement.getAttribute(INLINE_LINK_LANGUAGE_ATTRIBUTE) || '';
        this.title = this.currentElement.getAttribute(INLINE_LINK_TITLE_ATTRIBUTE) || '';
        this.newTab = this.currentElement.getAttribute(INLINE_LINK_TARGET_ATTRIBUTE) === LINK_TARGET_NEW_TAB;

        const linkObjectId = this.currentElement.getAttribute(INLINE_LINK_OBJECT_ID_ATTRIBUTE);
        const linkObjectNodeId = this.currentElement.getAttribute(INLINE_LINK_OBJECT_NODE_ATTRIBUTE);

        // If it's a referenced item, then we need to load it and properly save it.
        if (linkObjectId) {
            // internal links are always valid
            this.linkChecker.status = LinkCheckerStatus.VALID;
            this.loadLinkObject(linkObjectId, linkObjectNodeId);
        } else {
            this.loadingObject = false;
            this.isTargetObject = false;
            this.targetObject = null;
            this.targetUrl = this.currentElement.getAttribute(INLINE_LINK_URL_ATTRIBUTE) || '';
            this.linkToCheck.next(this.targetUrl);
        }
    }

    public updateLinkTarget(value: string): void {
        this.targetUrl = value;
        this.isTargetObject = false;
        this.linkToCheck.next(this.targetUrl);
        this.forwardStateToIFrame();
    }

    public setLinkNewTab(newTab: boolean): void {
        this.newTab = newTab;
        this.forwardStateToIFrame();
    }

    public updateAnchorValue(value: string): void {
        this.anchor = value;
        this.forwardStateToIFrame();
    }

    public updateTitleValue(value: string): void {
        this.title = value;
        this.forwardStateToIFrame();
    }

    public updateLanguageValue(value: string): void {
        this.language = value;
        this.forwardStateToIFrame();
    }

    public selectLinkTarget(): void {
        this.repositoryBrowser.openRepositoryBrowser({
            allowedSelection: (this.plugin.objectTypeFilter || ['file', 'image', 'page'])
                .map(value => (value === 'website' ? 'page' : null) as AllowedItemSelectionType)
                .filter(value => value != null && ALLOWED_SELECTION_TYPES.includes(value)),
            selectMultiple: false,
            title: this.i18n.translate('editor.format_link_pick_cms_target_modal_title'),
        }).then(res => {
            if (res == null) {
                this.targetObject = null;
            } else {
                this.targetObject = res as any;
            }

            this.isTargetObject = true;
            this.loadingObject = false;
            this.forwardStateToIFrame();
            this.changeDetector.markForCheck();
        }).catch(err => {
            // Shouldn't really happen, but we log it just in case
            console.error('error while picking target for link from repository browser!', err);
        });
    }

    public clearLinkTargetObject(): void {
        this.isTargetObject = false;
        this.targetObject = null;
        this.targetUrl = '';
        this.forwardStateToIFrame();
        this.changeDetector.markForCheck();
    }

    protected forwardStateToIFrame(): void {
        this.updateLinkPluginData();
        this.updateCurrentElementAttributes();
        this.triggerAlohaChangeEvents();
    }

    protected updateLinkPluginData(): void {
        // We also have to update the value inside of the link-plugin, because the plugin will not read
        // the data of the HTML element, but from the internal state for that element instead.
        // This would therefore reset the href and anchor all the time, which isn't what we want.
        const hrefValue = this.isTargetObject ? '' : this.targetUrl;
        const anchorValue = this.anchor ? `#${this.anchor}` : '';
        this.plugin.hrefValue = hrefValue + anchorValue;
        this.plugin.hrefField.setValue(hrefValue);

        // Anchor field is especially funny, because it has setters defined, but they don't do anything.
        // Instead, we have to set the value directly to the HTML Element.
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        const anchorHtmlElem = this.plugin.anchorField.element?.[0] as HTMLInputElement;
        if (anchorHtmlElem) {
            anchorHtmlElem.value = this.anchor;
        }

        // The Item needs some additional properties, otherwise the Plugin will not know what to do
        const itemForPlugin: any = this.targetObject ? {
            ...this.targetObject,
            id: `${nameToTypeId(this.targetObject.type)}.${this.targetObject.id}`,
            repositoryId: ALOHA_REPOSITORY_ID,
            baseType: 'document',
            renditions: [],
            loaded: false,
        } : null;
        this.plugin.hrefField.setItem(itemForPlugin);
    }

    /** Update the HTML Elements attribute with the current state/data */
    protected updateCurrentElementAttributes(): void {
        if (!this.currentElement) {
            return;
        }

        this.currentElement.setAttribute(INLINE_LINK_ANCHOR_ATTRIBUTE, this.anchor);
        this.currentElement.setAttribute(INLINE_LINK_LANGUAGE_ATTRIBUTE, this.language);
        this.currentElement.setAttribute(INLINE_LINK_TITLE_ATTRIBUTE, this.title);
        this.currentElement.setAttribute(INLINE_LINK_TARGET_ATTRIBUTE, this.newTab ? '_blank' : '_self');

        if (this.isTargetObject) {
            this.currentElement.setAttribute(INLINE_LINK_HREF_ATTRIBUTE, '#')
            this.currentElement.setAttribute(INLINE_LINK_URL_ATTRIBUTE, '');
            this.currentElement.setAttribute(INLINE_LINK_OBJECT_ID_ATTRIBUTE, `${nameToTypeId(this.targetObject.type)}.${this.targetObject.id}`);
            if ((this.targetObject as any).nodeId) {
                // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                this.currentElement.setAttribute(INLINE_LINK_OBJECT_NODE_ATTRIBUTE, `${(this.targetObject as any).nodeId}`);
            } else {
                this.currentElement.removeAttribute(INLINE_LINK_OBJECT_NODE_ATTRIBUTE);
            }
        } else {
            this.currentElement.setAttribute(INLINE_LINK_URL_ATTRIBUTE, this.targetUrl);
            this.currentElement.setAttribute(INLINE_LINK_HREF_ATTRIBUTE, this.targetUrl + (this.anchor ? `#${this.anchor}` : ''))
            this.currentElement.removeAttribute(INLINE_LINK_OBJECT_ID_ATTRIBUTE);
            this.currentElement.removeAttribute(INLINE_LINK_OBJECT_NODE_ATTRIBUTE);
        }
    }

    protected triggerAlohaChangeEvents(): void {
        let href = this.isTargetObject ? '' : this.targetUrl;
        if (this.anchor) {
            href += `#${this.anchor}`;
        }

        if (this.aloha) {
            this.aloha.trigger('aloha-link-href-change', {
                href: href,
                obj: this.currentElement,
                item: this.isTargetObject ? this.targetObject : null,
            });
        }

        if (this.pubSub) {
            this.pubSub.pub('aloha.link.changed', {
                href: href,
                element: this.aloha.jQuery(this.currentElement),
                input: null,
            });
        }
    }

    protected loadLinkObject(elementId: string, rawNodeId?: string): void {
        const [itemTypeId, itemId] = elementId.split('.');
        const itemTypeName = typeIdsToName(Number(itemTypeId));
        const options: ItemRequestOptions = {};

        // Reset link data
        this.targetUrl = '';
        this.loadingObject = true;
        this.isTargetObject = true;

        if (rawNodeId) {
            options.nodeId = Number(rawNodeId);
        }

        switch (itemTypeName) {
            case 'page':
                this.internalRefLoader = this.client.page.get(itemId, options).subscribe(res => {
                    this.targetObject = res.page;
                    this.loadingObject = false;
                    this.changeDetector.markForCheck();
                });
                break;

            case 'file':
                this.internalRefLoader = this.client.file.get(itemId, options).subscribe(res => {
                    this.targetObject = res.file;
                    this.loadingObject = false;
                    this.changeDetector.markForCheck();
                });
                break;

            case 'image':
                this.internalRefLoader = this.client.image.get(itemId, options).subscribe(res => {
                    this.targetObject = res.image;
                    this.loadingObject = false;
                    this.changeDetector.markForCheck();
                });
                break;

            default:
                this.isTargetObject = false;
                this.loadingObject = false;
                this.changeDetector.markForCheck();
        }
    }

    protected updateActive(active: boolean, silent: boolean = false): void {
        this.active = active;
        if (!silent) {
            this.activeChange.emit(active);
        }
    }

    protected clearLinkLoader(): void {
        if (this.internalRefLoader != null) {
            this.internalRefLoader.unsubscribe();
            this.internalRefLoader = null;
        }
    }
}
