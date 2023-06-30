import { Component, OnDestroy, OnInit } from '@angular/core';
import { File, Folder, Form, Image, ItemType, NodeFeature, NodeFeatures, Page, SortField } from '@gentics/cms-models';
import { IModalDialog, ModalService } from '@gentics/ui-core';
import { isEqual } from 'lodash';
import { Observable, Subject } from 'rxjs';
import { distinctUntilChanged, map, publishReplay, refCount, takeUntil, withLatestFrom } from 'rxjs/operators';
import { ApplicationStateService, WastebinActionsService } from '../../../state';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../providers/error-handler/error-handler.service';
import { I18nService } from '../../providers/i18n/i18n.service';
import { SortingModal } from './../../../shared/components/sorting-modal/sorting-modal.component';

@Component({
    selector: 'wastebin-modal',
    templateUrl: './wastebin-modal.tpl.html',
    styleUrls: ['./wastebin-modal.scss'],
})
export class WastebinModal implements OnInit, OnDestroy, IModalDialog {

    folders$: Observable<Folder[]>;
    forms$: Observable<Form[]>;
    pages$: Observable<Page[]>;
    files$: Observable<File[]>;
    images$: Observable<Image[]>;
    fetching$: Observable<boolean>;
    empty$: Observable<boolean>;

    filterTerm = '';
    sortBy: SortField;
    sortOrder: 'asc' | 'desc';
    selection: { [type: string]: number[] } = {};
    nodeId: number;

    private nodeFeatIsActiveForms$: Observable<boolean>;
    private destroyed$ = new Subject<void>();

    constructor(
        private wastebinActions: WastebinActionsService,
        private entityResolver: EntityResolver,
        private errorHandler: ErrorHandler,
        private appState: ApplicationStateService,
        private modalService: ModalService,
        private i18n: I18nService,
    ) { }

    ngOnInit(): void {
        this.nodeFeatIsActiveForms$ = this.nodeFeatureIsActive(this.nodeId, NodeFeature.FORMS);

        this.nodeFeatureIsActive(this.nodeId, NodeFeature.FORMS);
        this.nodeId = this.nodeId || this.appState.now.folder.activeNode;
        this.sortBy = this.appState.now.wastebin.sortBy;
        this.sortOrder = this.appState.now.wastebin.sortOrder;

        this.nodeFeatIsActiveForms$.pipe(
            takeUntil(this.destroyed$),
        ).subscribe(isActive => {
            this.wastebinActions.getWastebinContents(this.nodeId, this.sortBy, this.sortOrder, isActive);
        });

        const wastebinState$ = this.appState.select(state => state.wastebin).pipe(
            publishReplay(1),
            refCount(),
        );
        this.folders$ = wastebinState$.pipe(
            map(state => state.folder.list.map(id => this.entityResolver.getFolder(id))),
        );
        this.forms$ = wastebinState$.pipe(
            withLatestFrom(this.nodeFeatIsActiveForms$),
            map(([state, featureIsActive]) => featureIsActive ? state.form.list.map(id => this.entityResolver.getForm(id)) : []),
        );
        this.pages$ = wastebinState$.pipe(
            map(state => state.page.list.map(id => this.entityResolver.getPage(id))),
        );
        this.files$ = wastebinState$.pipe(
            map(state => state.file.list.map(id => this.entityResolver.getFile(id))),
        );
        this.images$ = wastebinState$.pipe(
            map(state => state.image.list.map(id => this.entityResolver.getImage(id))),
        );

        this.fetching$ = wastebinState$.pipe(
            map(state =>
                state.file.requesting ||
                state.folder.requesting ||
                state.form.requesting ||
                state.image.requesting ||
                state.page.requesting,
            ),
            distinctUntilChanged(isEqual),
        );

        this.empty$ = wastebinState$.pipe(
            map(state =>
                !state.file.requesting && !state.file.list.length &&
                !state.folder.requesting && !state.folder.list.length &&
                !state.form.requesting && !state.form.list.length &&
                !state.image.requesting && !state.image.list.length &&
                !state.page.requesting && !state.page.list.length,
            ),
            distinctUntilChanged(isEqual),
        );

        wastebinState$.pipe(
            takeUntil(this.destroyed$),
            map(state => {
                return {
                    sortBy: state.sortBy,
                    sortOrder: state.sortOrder,
                };
            }),
            distinctUntilChanged((newValue, oldValue) => {
                return newValue.sortBy === oldValue.sortBy && newValue.sortOrder === oldValue.sortOrder;
            }),
        ).subscribe(state => {
            this.sortBy = state.sortBy;
            this.sortOrder = state.sortOrder;
            this.wastebinActions.getWastebinContents(this.nodeId, state.sortBy, state.sortOrder);
        });
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    /**
     * Returns true is at least one item is selected.
     */
    itemsSelected(): boolean {
        return this.selectionCount() > 0;
    }

    /**
     * Returns the number of selected items.
     */
    selectionCount(): number {
        const selection = Object.keys(this.selection)
            .reduce((selection: number[], type: string) => selection.concat(this.selection[type]), []);
        return selection.length;
    }

    /**
     * Open the modal for selecting sort option.
     */
    selectSorting(): void {
        const locals: Partial<SortingModal> = {
            itemType: 'wastebin',
            sortBy: this.sortBy,
            sortOrder: this.sortOrder,
        };

        this.modalService.fromComponent(SortingModal, {}, locals)
            .then(modal => modal.open())
            .then(sorting => {
                this.updateSorting(sorting);
            })
            .catch(this.errorHandler.catch);
    }

    updateSorting( sorting: { sortBy: SortField; sortOrder: 'asc' | 'desc'; }): void {
        this.wastebinActions.setSorting( sorting.sortBy, sorting.sortOrder);
    }

    selectionChanged(type: ItemType, selection: number[]): void {
        this.selection[type] = selection;
    }

    restoreSelected(): void {
        Object.entries(this.selection).forEach(([type, selection]) => {
            if (!selection.length) {
                return;
            }

            this.wastebinActions.restoreItemsFromWastebin(type as any, selection)
                .then(() => this.updateSelectionAndPages(type));
        });
    }

    removeSelected(): void {
        if (!this.itemsSelected()) {
            return;
        }

        const itemCount = this.selectionCount();
        this.modalService.dialog({
            title: this.i18n.translate('modal.are_you_sure_modal_title'),
            body: this.i18n.translate(
                itemCount > 1 ? 'modal.wastebin_are_you_sure_body_plural' : 'modal.wastebin_are_you_sure_body_singular',
                { count: itemCount },
            ),
            buttons: [
                { label: this.i18n.translate('common.cancel_button'), type: 'secondary', flat: true, returnValue: false },
                { label: this.i18n.translate('editor.delete_from_wastebin_label'), type: 'alert', returnValue: true },
            ],
        })
            .then(modal => modal.open())
            .then(shouldContinue => {
                if (!shouldContinue) {
                    return;
                }

                Object.entries(this.selection).forEach(([type, selection]) => {
                    if (!selection) {
                        return;
                    }

                    this.wastebinActions.deleteItemsFromWastebin(type as any, selection)
                        .then(() => this.updateSelectionAndPages(type));
                });
            });
    }

    /**
     * Reset the selected items of the given type, and in the case of pages, we need to fetch the
     * list newly due to the complex way that language variants interact.
     */
    private updateSelectionAndPages(type: string): void {
        this.selection[type] = [];
        if (type === 'page') {
            this.wastebinActions.getWastebinContentsByType(this.nodeId, 'page', this.sortBy, this.sortOrder);
        }
    }

    nodeFeatureIsActive(nodeId: number, nodeFeature: keyof NodeFeatures): Observable<boolean> {
        return this.appState.select(state => state.features.nodeFeatures).pipe(
            map(nodeFeatures => {
                const activeNodeFeatures: (keyof NodeFeatures)[] = nodeFeatures[nodeId];
                return Array.isArray(activeNodeFeatures) && activeNodeFeatures.includes(nodeFeature);
            }),
        );
    }

    closeFn(val?: any): void { }
    cancelFn(val?: any): void {}

    registerCloseFn(close: (val: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }

}
