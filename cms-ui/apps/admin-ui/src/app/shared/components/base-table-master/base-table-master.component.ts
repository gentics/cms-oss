import { BusinessObject } from '@admin-ui/common';
import { AppStateService, FocusEditor } from '@admin-ui/state';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NormalizableEntityType } from '@gentics/cms-models';
import { TableRow } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { Subscription } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';

@Component({ template: '' })
export abstract class BaseTableMasterComponent<T, O = T & BusinessObject> implements OnInit, OnDestroy {

    public activeEntity: string;

    protected abstract entityIdentifier: NormalizableEntityType;
    protected detailPath: string;

    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected router: Router,
        protected route: ActivatedRoute,
        protected appState: AppStateService,
    ) {}

    ngOnInit(): void {
        this.setupActiveEntityWatcher();
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public handleRowClick(row: TableRow<O>): void {
        this.navigateToEntityDetails(row.id);
    }

    protected setupActiveEntityWatcher(): void {
        // Setup active entity loading from state
        this.subscriptions.push(this.appState.select(state => state.ui).pipe(
            map(uiState => uiState.focusEntityType === this.entityIdentifier ? uiState.focusEntityId : null),
            distinctUntilChanged(isEqual),
        ).subscribe(entityId => {
            this.activeEntity = String(entityId);
            this.changeDetector.markForCheck();
        }));
    }

    protected async navigateToEntityDetails(entityId: string | number): Promise<void> {
        await this.router.navigate(
            [{ outlets: { detail: [this.detailPath || this.entityIdentifier, entityId] } }],
            { relativeTo: this.route },
        );
        this.appState.dispatch(new FocusEditor());
    }
}
