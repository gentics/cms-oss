import { HttpClient } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    inject,
    input,
    model,
    OnChanges,
    OnDestroy,
    ViewChild,
} from '@angular/core';
import { ALOHAPAGE_URL, I18nService } from '@gentics/cms-components';
import { Page, PageResponse } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { cancelEvent, ChangesOf } from '@gentics/ui-core';
import { forkJoin, Subscription } from 'rxjs';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { ApplicationStateService } from '../../../state';

const CLASS_EDITABLE = 'gcmsui-editable';
const CLASS_INHERITED = 'inherited';
const ATTR_INHERITED_LABEL = 'data-gcmsui-inherited';
const ATTR_LOCALIZED_LABEL = 'data-gcmsui-localized';

const STYLES = `
@font-face {
  font-family: 'Roboto';
  font-style: normal;
  font-weight: 100 900;
  font-stretch: 100%;
  src: url('./assets/fonts/Roboto-VariableFont.ttf') format('truetype');
}

.${CLASS_EDITABLE} {
  position: relative;
  outline: 2px solid #0096dc;
  background-color: #0096dc2b;
  transition: 200ms;
  user-select: none;
  min-height: 24px;

  &::before {
    content: attr(${ATTR_LOCALIZED_LABEL});
    background: #0096dc;
    color: #f5f5f5;
    font-size: 14px;
    padding: 2px 5px 4px 7px;
    border-bottom-left-radius: 5px;
    position: absolute;
    right: 0;
    top: 0;
    line-height: 1.1;
    font-family: 'Roboto';
    font-weight: normal;
    font-style: normal;
    transition: 200ms;
  }

  &.${CLASS_INHERITED} {
    outline-color: #d6d6d6;
    background: #d6d6d669;

    &::before {
      content: attr(${ATTR_INHERITED_LABEL});
      background: #d6d6d6;
      color: #222;
    }
  }
}`;

interface AlohaRenderResult {
    content: string;
}

@Component({
    selector: 'gtx-inheritance-editor',
    templateUrl: './inheritance-editor.component.html',
    styleUrls: ['./inheritance-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class InheritanceEditorComponent implements OnChanges, OnDestroy {

    private http = inject(HttpClient);
    private client = inject(GCMSRestClientService);
    private changeDetector = inject(ChangeDetectorRef);
    private appState = inject(ApplicationStateService);
    private i18n = inject(I18nService);
    private errorHandler = inject(ErrorHandler);

    public pageId = input<number>();
    public nodeId = input<number>();
    public modifiedTags = model<Record<string, boolean>>();

    @ViewChild('iframe', { static: true })
    public iframe: ElementRef<HTMLIFrameElement>;

    public loading = false;

    protected loadedPage: Page;
    protected changedTags = new Set<string>();

    private subscription: Subscription | null = null;

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.modifiedTags) {
            this.changedTags = new Set(Object.keys(this.modifiedTags() || {}));
        }

        if (changes.pageId || changes.nodeId) {
            this.loadPageContent(this.pageId(), this.nodeId());
        }
    }

    ngOnDestroy(): void {
        if (this.subscription != null) {
            this.subscription.unsubscribe();
            this.subscription = null;
        }
    }

    public cleanupHooks(): void {
        if (!this.iframe?.nativeElement) {
            return;
        }
        // Prevent the default aloha check if it's allowed
        this.iframe.nativeElement.contentWindow.onbeforeunload = (event) => {
            cancelEvent(event);
        };
    }

    private loadPageContent(pageId: number, nodeId: number): void {
        if (this.subscription != null) {
            this.subscription.unsubscribe();
        }

        this.loading = true;
        this.changeDetector.markForCheck();

        // TODO: The backend has no full rendering of the page yet (with portal rendering that is)
        // Therefore we have to use the aloha-page renderer for this in the meantime.

        this.subscription = forkJoin([
            this.http.get(ALOHAPAGE_URL, {
                params: {
                    real: 'newedit',
                    realid: pageId,
                    nodeid: nodeId,
                    sid: this.appState.now.auth.sid,
                    type: 'json',
                },
                observe: 'body',
                responseType: 'json',
            }),
            this.client.page.get(pageId, {
                construct: false,
                nodeId: nodeId,
                update: true,
            }),
        ]).subscribe({
            next: ([renderRes, loadRes]: [AlohaRenderResult, PageResponse]) => {
                const pageDoc = Document.parseHTMLUnsafe(renderRes.content);
                this.loadedPage = loadRes.page;

                this.updateIFrame(pageDoc);

                this.loading = false;
                this.changeDetector.markForCheck();
            },
            error: (err) => {
                this.errorHandler.catch(err);
                this.loading = false;
                this.changeDetector.markForCheck();
            },
        });
    }

    private updateIFrame(pageDoc: Document): void {
        // Remove aloha placeholder elements
        Array.from(document.querySelectorAll('aloha_settings,aloha_scripts')).forEach((el) => {
            el.remove();
        });

        // Disable all clicks on anchors or buttons which would cause a navigation
        pageDoc.querySelectorAll('a, button').forEach((el) => {
            el.addEventListener('click', (event) => {
                cancelEvent(event);
            });
        });

        // Add custom styles
        const styleEl = pageDoc.createElement('style');
        styleEl.textContent = STYLES;
        pageDoc.head.append(styleEl);

        const inheritedLabel = this.i18n.instant('tag_inheritance.indicator_inherited');
        const localizedLabel = this.i18n.instant('tag_inheritance.indicator_localized');

        // Handle all the tags
        Object.entries(this.loadedPage.tags).forEach(([key, tag]) => {
            // Only handle root/template-tags
            if (!tag.rootTag) {
                return;
            }

            const el = pageDoc.querySelector(`.GENTICS_tagname_${tag.name}, [data-gcn-tagid="${tag.id}"]`);
            if (el == null) {
                console.warn(`Could not find root tag "${tag.name}" in document!`);
                return;
            }

            // Add the classes for styling
            el.classList.add(CLASS_EDITABLE);
            if (tag.inherited) {
                el.classList.add(CLASS_INHERITED);
            }

            // Add translated labels as attributes
            el.setAttribute(ATTR_INHERITED_LABEL, inheritedLabel);
            el.setAttribute(ATTR_LOCALIZED_LABEL, localizedLabel);

            el.addEventListener('click', (event) => {
                cancelEvent(event);
                el.classList.toggle(CLASS_INHERITED);
                this.setTagInheritance(key, !el.classList.contains(CLASS_INHERITED));
            });
        });

        const idoc = this.iframe.nativeElement.contentDocument;
        // Clear the whole iframe first
        Array.from(idoc.childNodes).forEach((node) => {
            node.remove();
        });

        idoc.append(...Array.from(pageDoc.children));
    }

    private setTagInheritance(key: string, inherited: boolean): void {
        this.loadedPage.tags[key].inherited = inherited;
        if (this.changedTags.has(key)) {
            this.changedTags.delete(key);
        } else {
            this.changedTags.add(key);
        }

        if (this.changedTags.size === 0) {
            this.modifiedTags.set(null);
            return;
        }

        const diff: Record<string, boolean> = {};
        this.changedTags.forEach((tagName) => {
            diff[tagName] = this.loadedPage.tags[tagName].inherited;
        });

        this.modifiedTags.set(diff);
    }
}
