import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    OnChanges,
    OnDestroy,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { RepositoryBrowserClient } from '@editor-ui/app/shared/providers';
import { AlohaLinkPlugin, AlohaPubSub } from '@gentics/aloha-models';
import { nameToTypeId, typeIdsToName } from '@gentics/cms-components';
import { AllowedItemSelectionType, AllowedSelectionType, File, Image, ItemRequestOptions, Page } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { InputComponent } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import {
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
} from '../../../common/models/aloha-integration';
import { BaseControlsComponent } from '../base-controls/base-controls.component';

interface LinkCheckerSettings {
    enabled?: boolean;
    valid?: boolean;
    report?: LinkCheckerReport;
}

interface LinkCheckerReport {
    date: Date;
    url: string;
    text: string;
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
export class LinkControlsComponent extends BaseControlsComponent implements OnChanges, OnDestroy {

    @Output()
    public activeChange = new EventEmitter<boolean>();

    /** The input where the user can enter the URL. */
    @ViewChild('targetInput')
    public inputTargetElement?: InputComponent;

    /** If currently a link is focused adn therefore the options for it should be displayed. */
    public active = false;

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
        enabled: true,
        valid: true,
        report: {
            date: new Date(),
            url: 'https://example.copm',
            text: 'sample text',
        },
    };

    /** The currently selected Link Element in the iFrame. */
    protected currentElement: HTMLAnchorElement | null;

    /** Instance of the link-plugin from the iFrame. */
    protected linkPlugin: AlohaLinkPlugin;
    /** Instance of the PubSub from the iFrame. */
    protected pubSub: AlohaPubSub;

    /** Subscription which loads the picked item from the CMS. */
    protected linkLoader: Subscription | null;

    constructor(
        changeDetector: ChangeDetectorRef,
        protected api: GcmsApi,
        protected i18n: I18nService,
        protected repositoryBrowser: RepositoryBrowserClient,
    ) {
        super(changeDetector);
    }

    public ngOnChanges(changes: SimpleChanges): void {
        if (changes.aloha) {
            if (this.aloha) {
                try {
                    this.pubSub = this.aloha.require('PubSub');
                } catch (err) {
                    console.warn('Error while loading pub-sub!', err);
                    this.pubSub = null;
                }
                try {
                    this.linkPlugin = this.aloha.require('link/link-plugin');
                } catch (err) {
                    console.warn('Error while loading link-plugin!', err);
                    this.linkPlugin = null;
                }
            } else {
                this.linkPlugin = null;
            }
        }

        if (changes.range || changes.settings) {
            this.updateStateFromAloha();
        }
    }

    ngOnDestroy(): void {
        this.clearLinkLoader();
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

        const res = this.linkPlugin.insertLink(false);
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
        this.linkPlugin.removeLink();
        this.updateActive(false);
    }

    public updateStateFromAloha(): void {
        this.currentElement = null;

        if (!this.linkPlugin || !this.range || !this.range.markupEffectiveAtStart) {
            this.updateActive(false);
            this.changeDetector.markForCheck();
            return;
        }

        for (const elem of this.range.markupEffectiveAtStart) {
            // Only enable the link handling when the plugin is available
            if (elem.nodeName !== LINK_NODE_NAME || (
                !elem.classList.contains(INLINE_LINK_CLASS)
                && elem.getAttribute(INLINE_LINK_URL_ATTRIBUTE) == null
            )) {
                continue;
            }

            this.updateActive(true);
            this.clearLinkLoader();
            this.currentElement = elem as HTMLAnchorElement;

            // Set the common/easy properties
            this.anchor = elem.getAttribute(INLINE_LINK_ANCHOR_ATTRIBUTE) || '';
            this.language = elem.getAttribute(INLINE_LINK_LANGUAGE_ATTRIBUTE) || '';
            this.title = elem.getAttribute(INLINE_LINK_TITLE_ATTRIBUTE) || '';
            this.newTab = elem.getAttribute(INLINE_LINK_TARGET_ATTRIBUTE) === LINK_TARGET_NEW_TAB;

            const linkObjectId = elem.getAttribute(INLINE_LINK_OBJECT_ID_ATTRIBUTE);
            const linkObjectNodeId = elem.getAttribute(INLINE_LINK_OBJECT_NODE_ATTRIBUTE);

            // If it's a referenced item, then we need to load it and properly save it.
            if (linkObjectId) {
                this.loadLinkObject(linkObjectId, linkObjectNodeId);
            } else {
                this.loadingObject = false;
                this.isTargetObject = false;
                this.targetObject = null;
                this.targetUrl = elem.getAttribute(INLINE_LINK_URL_ATTRIBUTE) || '';
            }

            break;
        }

        if (this.currentElement == null) {
            this.updateActive(false);
        }
    }

    public toggleLinkCheckValidity(): void {
        this.linkChecker.valid = !this.linkChecker.valid;
    }

    public updateLinkTarget(value: string): void {
        this.targetUrl = value;
        this.isTargetObject = false;
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
            allowedSelection: (this.linkPlugin.objectTypeFilter || ['file', 'image', 'page'])
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
        this.linkPlugin.hrefValue = hrefValue + anchorValue;
        this.linkPlugin.hrefField.setValue(hrefValue);

        // Anchor field is especially funny, because it has setters defined, but they don't do anything.
        // Instead, we have to set the value directly to the HTML Element.
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        const anchorHtmlElem = this.linkPlugin.anchorField.element?.[0] as HTMLInputElement;
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
        this.linkPlugin.hrefField.setItem(itemForPlugin);
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
                element: this.currentElement,
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
                this.linkLoader = this.api.pages.getPage(itemId, options).subscribe(res => {
                    this.targetObject = res.page;
                    this.loadingObject = false;
                    this.changeDetector.markForCheck();
                });
                break;

            case 'file':
                this.linkLoader = this.api.files.getFile(itemId, options).subscribe(res => {
                    this.targetObject = res.file;
                    this.loadingObject = false;
                    this.changeDetector.markForCheck();
                });
                break;

            case 'image':
                this.linkLoader = this.api.images.getImage(itemId, options).subscribe(res => {
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
        if (this.linkLoader != null) {
            this.linkLoader.unsubscribe();
            this.linkLoader = null;
        }
    }
}
