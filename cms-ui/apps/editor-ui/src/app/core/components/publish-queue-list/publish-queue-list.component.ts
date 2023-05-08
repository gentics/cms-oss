import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, OnInit, Output } from '@angular/core';
import { ItemType, Page } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { UserSettingsService } from '../../../core/providers/user-settings/user-settings.service';
import { DisplayFieldSelector } from '../../../shared/components/display-field-selector/display-field-selector.component';
import { ApplicationStateService } from '../../../state';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';

/**
 * A table which displays the pages in the publish queue.
 *
 * If readOnly is not set to true, A page can be clicked, or one or more pages
 * may be published or assigned for revision.
 */
@Component({
    selector: 'publish-queue-list',
    templateUrl: './publish-queue-list.tpl.html',
    styleUrls: ['./publish-queue-list.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
    })
export class PublishQueueList implements OnInit, OnChanges {

    @Input()
    pages: Page[];

    @Input()
    readOnly = false;

    @Output()
    pageClick = new EventEmitter<Page>();

    @Output()
    selectionChange = new EventEmitter<Page[]>();

    private selected: { [id: number]: boolean } = {};

    displayFields$: Observable<string[]>;

    constructor(
        private appState: ApplicationStateService,
        private entityResolver: EntityResolver,
        private errorHandler: ErrorHandler,
        private modalService: ModalService,
        private userSettings: UserSettingsService,
    ) { }

    ngOnInit(): void {
        this.displayFields$ = this.appState.select(state => state.publishQueue.pages.displayFields).pipe(
            map(() => {
                return [
                    // to prevent user confusion these fields will always show
                    'queuedPublish',
                    'queuedOffline',
                ];
            }));
    }

    ngOnChanges(): void {
        this.pages.forEach(page => {
            if (!this.selected.hasOwnProperty(page.id.toString())) {
                this.selected[page.id] = false;
            }
        });
    }

    pageClicked(e: MouseEvent, page: Page): void {
        e.preventDefault();
        this.pageClick.emit(page);
    }

    toggleSelect(pageId: number, isSelected: boolean): void {
        this.selected[pageId] = isSelected;
        this.emitSelectionChange();
    }

    toggleSelectall(): void {
        let value = !this.areAllSelected();
        Object.keys(this.selected).forEach(id => this.selected[+id] = value);
        this.emitSelectionChange();
    }

    selectDisplayFields(): void {
        const type = 'page';
        const fields = this.appState.now.publishQueue.pages.displayFields;
        this.modalService.fromComponent(DisplayFieldSelector, {}, { type, fields })
            .then(modal => modal.open())
            .then(selection => this.updateDisplayFields(type, selection))
            .catch(this.errorHandler.catch);
    }

    updateDisplayFields(type: ItemType, fields: string[]): void {
        this.userSettings.setPublishQueueDisplayFields(type, fields);
    }

    private emitSelectionChange(): void {
        const selectedIds = Object.keys(this.selected)
            .filter(id => this.selected[+id])
            .map(id => +id);
        const selectedPages = this.pages.filter(p => -1 < selectedIds.indexOf(p.id));
        this.selectionChange.emit(selectedPages);
    }

    private getEditorName(editorId: number): string {
        const editor = this.entityResolver.getUser(editorId);
        return `${editor.firstName} ${editor.lastName}`;
    }

    private areAllSelected(): boolean {
        return 0 < this.pages.length && this.selectedIds().length === this.pages.length;
    }

    /**
     * Returns an array of the selected page ids.
     */
    private selectedIds(): number[] {
        return Object.keys(this.selected)
            .filter(id => !!this.selected[+id])
            .map(id => +id);
    }
}
