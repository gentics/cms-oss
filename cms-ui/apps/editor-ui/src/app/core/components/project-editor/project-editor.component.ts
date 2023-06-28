import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { filter } from 'rxjs/operators';
import { PresentationService } from '../../../shared/providers/presentation/presentation.service';
import { ApplicationStateService, FocusEditorAction, FocusListAction } from '../../../state';

@Component({
    selector: 'project-editor',
    templateUrl: './project-editor.component.html',
    styleUrls: ['./project-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectEditor implements OnInit {

    editorFocused$: Observable<boolean>;
    editorOpen$: Observable<boolean>;
    focusMode$: Observable<boolean>;
    activeNodeId$: Observable<number>;

    splitFocus: 'left' | 'right' = 'left';

    headerHeight$: Observable<string>;

    constructor(
        private appState: ApplicationStateService,
        private presentation: PresentationService,
    ) {
        this.editorFocused$ = appState.select(state => state.editor.editorIsFocused);
        this.focusMode$ = appState.select(state => state.editor.focusMode);
        this.editorOpen$ = appState.select(state => state.editor.editorIsOpen);
        this.activeNodeId$ = appState.select(state => state.folder.activeNode);
    }

    ngOnInit(): void {
        this.headerHeight$ = this.presentation.headerHeight$.pipe(
            filter((headerHeight: string) => typeof headerHeight === 'string'),
        );
    }

    setSplitFocus(focus: 'left' | 'right'): void {
        if (focus === 'right') {
            this.appState.dispatch(new FocusEditorAction());
        } else {
            this.appState.dispatch(new FocusListAction());
        }
    }
}
