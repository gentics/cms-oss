import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { WindowRef } from '@gentics/cms-components';
import { TagEditorChange, TagEditorChangeMessage } from '@gentics/cms-integration-api-models';
import { EntityType } from '@gentics/cms-models';
import { environment } from '../../../../environments/environment';

interface TagEditorURL {
    nodeId?: number;
    entityType: EntityType;
    entityId: number | string;
    tagName: string;
}

@Component({
    selector: 'gtx-tag-editor-wrapper',
    templateUrl: './tag-editor-wrapper.component.html',
    styleUrls: ['./tag-editor-wrapper.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TagEditorWrapperComponent implements OnDestroy, OnChanges {

    @Input()
    public baseUrl: URL;

    @Input()
    public nodeId: number;

    @Input()
    public entityType: EntityType;

    @Input()
    public entityId: number | string;

    @Input()
    public tagName: string;

    @Input()
    public withTitle = true;

    @Input()
    public transparent = false;

    @Output()
    public change = new EventEmitter<TagEditorChange>();

    public iframe: HTMLIFrameElement;
    public tagEditorUrl: string;

    private msgFn = (event) => this.messageHandler(event)

    constructor(
        private windowRef: WindowRef,
    ) { }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.nodeId || changes.entityType || changes.entityId || changes.tagName) {
            this.updateURLs({
                nodeId: this.nodeId,
                entityType: this.entityType,
                entityId: this.entityId,
                tagName: this.tagName,
            });
        }
    }

    ngOnDestroy(): void {
        this.clearMessageFunction();
    }

    iframeLoaded(iframe: HTMLIFrameElement): void {
        this.iframe = iframe;
        this.clearMessageFunction();
        this.registerMessageFunction();
    }

    registerMessageFunction(): void {
        try {
            this.windowRef.nativeWindow.addEventListener('message', this.msgFn, false);
        } catch (e) {
            console.warn('Could not attach message handler to tag-editor iframe!', e);
        }
    }

    clearMessageFunction(): void {
        try {
            this.windowRef.nativeWindow.removeEventListener('message', this.msgFn, false);
        } catch (e) {
            console.warn('Could not remove message handler to tag-editor iframe!', e);
        }
    }

    verifyBaseUrl(): void {
        if (this.baseUrl == null) {
            this.baseUrl = new URL(this.windowRef.nativeWindow.location.toString());
        }
    }

    updateURLs(settings: TagEditorURL): void {
        this.verifyBaseUrl();
        const tmpUrl = this.baseUrl;

        // When developing locally, the editor-ui is served on root under a different port.
        // In production, the editor is always available under /editor/.
        tmpUrl.pathname = (environment?.production ?? (environment as any)?.environment?.production) ? '/editor/' : '/';

        tmpUrl.hash = `#/tag-editor/${settings.nodeId}/${settings.entityType}/${settings.entityId}/${settings.tagName}`;
        // The way angular parses the query-params is different from how the URL.toString() prints it.
        // Therefore manual setting like this.
        this.tagEditorUrl = tmpUrl.toString() + `?title=${this.withTitle ? 'true' : 'false'}&transparent=${this.transparent ? 'true' : 'false'}`;
    }

    messageHandler(event: MessageEvent<TagEditorChangeMessage>): void {
        const tmpUrl = this.baseUrl;
        // Ignore invalid messages
        if (event.origin !== tmpUrl.origin
            || event.data == null
            || typeof event.data !== 'object'
            || event.data.type !== 'tag-editor-change'
        ) {
            return;
        }

        const { type: _, ...msg } = event.data;
        this.change.emit(msg);
    }
}
