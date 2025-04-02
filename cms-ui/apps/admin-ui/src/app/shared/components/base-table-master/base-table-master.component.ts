import { AdminUIEntityDetailRoutes, BusinessObject, EditableEntity, ROUTE_ENTITY_LOADED, ROUTE_ENTITY_RESOLVER_KEY } from '@admin-ui/common';
import { AppStateService, FocusEditor } from '@admin-ui/state';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationExtras, Router } from '@angular/router';
import { NormalizableEntityType } from '@gentics/cms-models';
import { TableRow, getFullPrimaryPath } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { Subscription } from 'rxjs';
import { delay, distinctUntilChanged, map } from 'rxjs/operators';

@Component({ template: '' })
export abstract class BaseTableMasterComponent<T, O = T & BusinessObject> implements OnInit, OnDestroy {

    public activeEntity: string;

    protected abstract entityIdentifier: NormalizableEntityType | EditableEntity;
    protected detailPath?: AdminUIEntityDetailRoutes;

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
        this.navigateToEntityDetails(row);
    }

    protected setupActiveEntityWatcher(): void {
        // Setup active entity loading from state
        this.subscriptions.push(this.appState.select(state => state.ui).pipe(
            map(uiState => uiState.focusEntityType === this.entityIdentifier ? uiState.focusEntityId : null),
            distinctUntilChanged(isEqual),
            delay(0),
        ).subscribe(entityId => {
            this.activeEntity = String(entityId);
            this.changeDetector.markForCheck();
        }));
    }

    protected navigateWithEntity(): boolean {
        return true;
    }

    protected async navigateToEntityDetails(row: TableRow<O>): Promise<void> {
        const fullUrl = getFullPrimaryPath(this.route);
        const commands: any[] = [
            fullUrl,
            { outlets: { detail: [this.detailPath || this.entityIdentifier, row.id] } },
        ];
        const extras: NavigationExtras = { relativeTo: this.route };

        if (this.navigateWithEntity()) {
            extras.state = {
                [ROUTE_ENTITY_LOADED]: true,
                [ROUTE_ENTITY_RESOLVER_KEY]: row.item,
            };
        }

        await this.router.navigate(commands, extras);
        this.appState.dispatch(new FocusEditor());
    }
}
