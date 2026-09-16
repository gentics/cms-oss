import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationExtras, Router } from '@angular/router';
import { AccessControlledType, GcmsPermission, Group, NormalizableEntityType } from '@gentics/cms-models';
import { TrableRow, getFullPrimaryPath } from '@gentics/ui-core';
import { Observable, firstValueFrom } from 'rxjs';
import { distinctUntilChanged, map, take } from 'rxjs/operators';
import {
    AdminUIEntityDetailRoutes,
    GroupBO,
    GroupDetailTabs,
    ROUTE_ENTITY_LOADED,
    ROUTE_ENTITY_RESOLVER_KEY,
} from '../../../../common';
import { PermissionsService } from '../../../../core';
import { BaseTableMasterComponent } from '../../../../shared/components';
import {
    AppStateService,
    FocusEditor,
    GROUP_VIEW_MODE_LIST,
    GROUP_VIEW_MODE_TREE,
    GroupViewMode,
    SetUserSettingAction,
} from '../../../../state';

/**
 * `GET /group/{id}/users`, which backs the "Group Users" tab, additionally requires view
 * permission on the user administration (`GroupResourceImpl.java:551-557`,
 * `@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)`).
 */
const GROUP_USERS_TAB_PERMISSIONS = {
    type: AccessControlledType.USER_ADMIN,
    permissions: GcmsPermission.READ,
};

@Component({
    selector: 'gtx-group-master',
    templateUrl: './group-master.component.html',
    styleUrls: ['./group-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class GroupMasterComponent extends BaseTableMasterComponent<Group, GroupBO> implements OnInit {

    public readonly GROUP_VIEW_MODE_TREE = GROUP_VIEW_MODE_TREE;

    /** The currently active view mode. Persisted per user as the `admin_groupViewMode` setting. */
    public viewMode: GroupViewMode = GROUP_VIEW_MODE_LIST;

    protected entityIdentifier: NormalizableEntityType = 'group';

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected permissions: PermissionsService,
    ) {
        super(changeDetector, router, route, appState);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(this.appState.select(
            (state) => state.ui.settings?.[state.auth.user?.id]?.groupViewMode,
        ).pipe(
            // The value comes back from server storage unvalidated (`user-settings.service.ts:86-88`
            // stores `data[k]` raw), so anything that is not the tree mode falls back to the list.
            map((raw) => raw === GROUP_VIEW_MODE_TREE ? GROUP_VIEW_MODE_TREE : GROUP_VIEW_MODE_LIST),
            distinctUntilChanged(),
        ).subscribe((viewMode) => {
            this.viewMode = viewMode;
            this.changeDetector.markForCheck();
        }));
    }

    public toggleViewMode(): void {
        const newMode: GroupViewMode = this.viewMode === GROUP_VIEW_MODE_TREE ? GROUP_VIEW_MODE_LIST : GROUP_VIEW_MODE_TREE;
        this.appState.dispatch(new SetUserSettingAction('groupViewMode', newMode));
    }

    /**
     * The tab a tree node click lands on: the group's users when the user may read them,
     * the group properties otherwise.
     *
     * Without the check, a user who has `GROUP_ADMIN` view but no `USER_ADMIN` view would get a
     * 403 error notification on *every* group click, because the users tab immediately issues
     * `GET /group/{id}/users`. The server denies correctly either way - this only stops the UI
     * from walking into a wall it can see coming.
     */
    protected resolveLandingTab(): Observable<GroupDetailTabs> {
        return this.permissions.checkPermissions(GROUP_USERS_TAB_PERMISSIONS).pipe(
            map((mayReadUsers) => mayReadUsers ? GroupDetailTabs.GROUP_USERS : GroupDetailTabs.PROPERTIES),
            take(1),
        );
    }

    /**
     * Navigates to the group detail of the clicked tree node and opens it on the landing tab
     * determined by {@link resolveLandingTab}.
     *
     * Same navigation shape as {@link BaseTableMasterComponent.navigateToEntityDetails}, extended
     * by the tab segment - the inherited `detailPath` is a single segment and cannot carry it.
     */
    public async handleTreeRowClick(row: TrableRow<GroupBO>): Promise<void> {
        const tab = await firstValueFrom(this.resolveLandingTab());
        const fullUrl = getFullPrimaryPath(this.route);
        const commands: any[] = [
            fullUrl,
            { outlets: { detail: [AdminUIEntityDetailRoutes.GROUP, row.id, tab] } },
        ];
        const extras: NavigationExtras = {
            relativeTo: this.route,
            state: {
                [ROUTE_ENTITY_LOADED]: true,
                [ROUTE_ENTITY_RESOLVER_KEY]: row.item,
            },
        };

        await this.router.navigate(commands, extras);
        this.appState.dispatch(new FocusEditor());
    }
}
