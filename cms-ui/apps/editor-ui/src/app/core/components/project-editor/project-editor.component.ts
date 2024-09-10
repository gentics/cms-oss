import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { EditorOutlet, EditorState } from '@editor-ui/app/common/models';
import { isEqual } from 'lodash-es';
import { Subscription } from 'rxjs';
import { distinctUntilChanged, filter } from 'rxjs/operators';
import { PresentationService } from '../../../shared/providers/presentation/presentation.service';
import { ApplicationStateService, FocusEditorAction, FocusListAction } from '../../../state';
import { ErrorHandler } from '../../providers/error-handler/error-handler.service';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';
import { NavigationService } from '../../providers/navigation/navigation.service';

@Component({
    selector: 'project-editor',
    templateUrl: './project-editor.component.html',
    styleUrls: ['./project-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectEditorComponent implements OnInit, OnDestroy {

    public readonly EditorOutlet = EditorOutlet;

    editorFocused: boolean;
    editorOpen: boolean;
    focusMode: boolean;
    activeNodeId: number;
    headerHeight: string;

    splitFocus: 'left' | 'right' = 'left';

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
        private presentation: PresentationService,
        private errorHandler: ErrorHandler,
        private entityResolver: EntityResolver,
        private navigationService: NavigationService,
    ) {}

    ngOnInit(): void {
        this.subscriptions.push(this.appState.select(state => state.editor).pipe(
            distinctUntilChanged(isEqual),
        ).subscribe((editorState: EditorState) => {
            this.editorFocused = editorState.editorIsFocused;
            this.focusMode = editorState.focusMode;
            this.editorOpen = editorState.editorIsOpen;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.folder.activeNode).subscribe(nodeId => {
            this.activeNodeId = nodeId;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.presentation.headerHeight$.pipe(
            filter((headerHeight: string) => typeof headerHeight === 'string'),
        ).subscribe(height => {
            this.headerHeight = height;
            this.changeDetector.markForCheck();
        }));

        // this.subscriptions.push(this.appState.select(state => state.folder.activeNode).pipe(
        //     filter(activeNode => activeNode != null),
        // ).subscribe(activeNode => {
        //     const node = this.entityResolver.getNode(activeNode);
        //     if (node) {
        //         this.navigationService.list(node.id, node.folderId).navigate();
        //     }
        // },
        // error => this.errorHandler.catch(error),
        // ));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    setSplitFocus(focus: 'left' | 'right'): void {
        if (focus === 'right') {
            this.appState.dispatch(new FocusEditorAction());
        } else {
            this.appState.dispatch(new FocusListAction());
        }
    }
}
