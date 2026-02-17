import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { WindowRef } from '@gentics/cms-components';
import { KeycloakService, SKIP_KEYCLOAK_PARAMETER_NAME } from '@gentics/cms-components/auth';
import { TagEditorChange, TagEditorChangeMessage } from '@gentics/cms-integration-api-models';
import { EntityType } from '@gentics/cms-models';
import { environment } from '../../../../environments/environment';
import { AppStateService } from '@admin-ui/state';

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
    standalone: false
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
        private appState: AppStateService,
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
            this.baseUrl = new URL(environment.editorUrl, this.windowRef.nativeWindow.location.toString());
        }
    }

    updateURLs(settings: TagEditorURL): void {
        this.verifyBaseUrl();
        const tmpUrl = new URL(this.baseUrl);

        // If the admin-ui has been opened to skip the sso-/keycloak-login,
        // then we want to open the editor-ui with the same flag.
        if (this.appState.now.auth.ssoSkipped) {
            tmpUrl.searchParams.set(SKIP_KEYCLOAK_PARAMETER_NAME, '');
        }

        // Since we use hash-routing for the apps, we have to write it like this
        tmpUrl.hash = `#/tag-editor/${settings.nodeId}/${settings.entityType}/${settings.entityId}/${settings.tagName}`;

        // These are search-params for the app route. Therefore, we have to pass them manually
        // at the end, as the params from `tmpUrl` are for the app, but not the route.
        const params = new URLSearchParams();
        params.set('title', this.withTitle ? 'true' : 'false');
        params.set('transparent', this.transparent ? 'true' : 'false');

        this.tagEditorUrl = `${tmpUrl.toString()}?${params.toString()}`;
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
