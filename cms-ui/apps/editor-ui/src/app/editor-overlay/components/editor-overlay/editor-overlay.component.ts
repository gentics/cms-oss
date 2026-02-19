import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { IModalInstance } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { Subscription, merge } from 'rxjs';
import { distinctUntilChanged, filter, map, switchMapTo } from 'rxjs/operators';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { ApplicationStateService, EditorStateUrlOptions, EditorStateUrlParams, NodeSettingsActionsService } from '../../../state';
import { EditorOverlayService } from '../../providers/editor-overlay.service';

/**
 * EditorOverlay component is provides a container for the router outlet 'modal'.
 * This can be extended with modal type editors, the details are passed via the router.
 */

@Component({
    selector: 'editor-overlay',
    template: '',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class EditorOverlay implements OnInit, OnDestroy {

    private subscriptions: Subscription[] = [];

    constructor(
        private appState: ApplicationStateService,
        private route: ActivatedRoute,
        private navigationService: NavigationService,
        private nodeSettingsActions: NodeSettingsActionsService,
        private editorOverlayService: EditorOverlayService,
    ) {}

    ngOnInit(): void {
        const onLogin$ = this.appState.select((state) => state.auth).pipe(
            distinctUntilChanged(isEqual, (state) => state.user?.id),
            filter((state) => state.isLoggedIn === true),
        );

        const modalRouteParams$ = onLogin$.pipe(switchMapTo(this.route.params));

        const modalEditorState$ = modalRouteParams$.pipe(
            filter((params) => Object.keys(params).length >= 4),
            map((params: EditorStateUrlParams) => this.updateEditorState(params)),
        );

        const modalDisplayByType$ = modalRouteParams$.pipe(
            filter((params) => Object.keys(params).length === 1 && params.type),
            map((params) => params && this.openModalByType(params.type)),
        );

        const urlSub = merge(modalEditorState$, modalDisplayByType$).pipe(
            switchMapTo(this.editorOverlayService.editorOverlayOnClose$),
        ).subscribe((modal) => {
            this.closeEditor(modal);
        });

        this.subscriptions.push(urlSub);
    }

    /**
     * Takes the "modal" aux route params and uses these to call the correct editorActions method.
     */
    private updateEditorState(urlParams: EditorStateUrlParams): void {
        let { itemId, nodeId, type, editMode } = urlParams;
        itemId = Number(itemId);
        nodeId = Number(nodeId);

        // Make sure, that the correct nodeId's settings are loaded
        if (typeof this.appState.now.nodeSettings.node[nodeId] === 'undefined') {
            this.nodeSettingsActions.loadNodeSettings(nodeId);
        }

        const options = this.navigationService.deserializeOptions<EditorStateUrlOptions>(urlParams.options);

        switch (editMode) {
            case 'edit':
                if (type === 'image') {
                    this.editorOverlayService.editImage({ nodeId: nodeId, itemId: itemId });
                }
                break;
            default:
        }
    }

    /**
     * Takes the "modal" aux route single param to call a modal by name
     */
    private openModalByType(type: string): void {
        switch (type) {
            case 'publishQueue':
                this.editorOverlayService.displayPublishQueue();
                break;
            default:
        }
    }

    /**
     * Tell the NavigationService that the user initiated a close action.
     */
    closeEditor(modal?: IModalInstance<any>): void {
        this.navigationService.instruction({ modal: null }).navigate({ replaceUrl: true });
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach((s) => s.unsubscribe());
    }
}
