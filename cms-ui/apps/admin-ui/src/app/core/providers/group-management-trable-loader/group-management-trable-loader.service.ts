import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, GroupBO, forEachTreeNode } from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { Group, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { TrableRow } from '@gentics/ui-core';
import { Observable, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseTrableLoaderService } from '../base-trable-loader/base-trable-loader.service';
import { EntityManagerService } from '../entity-manager';

export interface GroupManagementTrableLoaderOptions {
    /**
     * Reserved for future server-side filtering. The tree is filtered client-side in
     * `GroupManagementTrableComponent`, because `GET /group/load` does not support filtering.
     */
    query?: string;
}

/**
 * Trable loader for the group *management* tree (the switchable master view of `/groups`).
 *
 * In contrast to the permission-oriented `GroupTrableLoaderService`, this loader is **eager**:
 * `GET /group/load` already answers with the complete, recursively nested group tree
 * (`GroupResourceImpl.load()` -> `ModelBuilder.getGroup(group, includeChildren = true)`),
 * so no per-expand request is necessary. The three overrides below are what turns the
 * lazy default behaviour of `BaseTrableLoaderService` into an eager one.
 */
@Injectable()
export class GroupManagementTrableLoaderService extends BaseTrableLoaderService<Group, GroupBO, GroupManagementTrableLoaderOptions> {

    constructor(
        protected api: GcmsApi,
        protected entityManager: EntityManagerService,
    ) {
        super();
    }

    /**
     * Loads the root elements (whole tree) or serves the children of an already loaded parent.
     *
     * The children are deliberately *not* fetched via `getSubgroups()`: that endpoint answers
     * without the `children` property, which would make {@link hasChildren} report every
     * grandchild as a leaf as soon as a row is reloaded.
     */
    protected loadEntityChildren(parent: GroupBO | null): Observable<GroupBO[]> {
        if (parent != null) {
            return of((parent.children || []).map((child) => this.mapToBusinessObject(child)));
        }

        return this.api.group.getGroupsTree().pipe(
            tap((res) => this.storeGroupsInEntityState(res.groups)),
            map((res) => res.groups.map((group) => this.mapToBusinessObject(group))),
        );
    }

    /**
     * Puts every group of the tree into `state.entity.group`, which several consumers read
     * unguarded - `MoveGroupsModalComponent.getModalTitle()` and
     * `GroupOperations.addSubgroupToParent()` among them. The flat list view gets this for free
     * through `BaseTableLoaderService`; the trable base class has no entity manager at all.
     *
     * The groups are flattened and stripped of their `children` on purpose:
     * `GcmsNormalizer.processDenormalizedGroup()` mutates its input and *deletes* `children` from
     * every group below the first level, which would truncate the tree that is being displayed.
     * Flat, `children`-free copies leave the tree untouched, and because `AddEntities` merges
     * shallowly, an already stored `children` list is not overwritten either.
     */
    private storeGroupsInEntityState(groups: Group<Raw>[]): void {
        const flattened: Group<Raw>[] = [];

        forEachTreeNode(groups, (group) => {
            // `_children` is only destructured to strip it off; it is deliberately unused.
            const { children: _children, ...withoutChildren } = group;
            flattened.push(withoutChildren as Group<Raw>);
        });

        this.entityManager.addEntities('group', flattened);
    }

    /**
     * Loads a single group. `GET /group/{id}` answers *without* the `children` property, so the
     * already known subtree is carried over - otherwise the reloaded row would degrade to a leaf.
     */
    protected loadEntityRow(entity: GroupBO): Observable<GroupBO> {
        return this.api.group.getGroup(entity.id).pipe(
            map((res) => this.mapToBusinessObject({
                ...res.group,
                children: res.group.children || entity.children,
            })),
        );
    }

    /**
     * Only groups which actually have subgroups get an expander. The default implementation
     * returns `true` unconditionally, which renders an expander on every leaf.
     */
    protected override hasChildren(entity: GroupBO): boolean {
        return Array.isArray(entity.children) && entity.children.length > 0;
    }

    /**
     * Maps the entity and its whole subtree to trable rows.
     *
     * `loaded` is set to `true` because the children are already present. This makes
     * `gtx-trable` emit `rowExpand` instead of `loadRow` on expansion (no redundant request)
     * and lets it omit the expander icon on leaves, which it only does for loaded rows.
     */
    protected override mapToTrableRow(
        entity: GroupBO,
        parent?: TrableRow<GroupBO>,
        options?: GroupManagementTrableLoaderOptions,
    ): TrableRow<GroupBO> {
        const row = super.mapToTrableRow(entity, parent, options);

        row.children = (entity.children || [])
            .map((child) => this.mapToTrableRow(this.mapToBusinessObject(child), row, options));
        row.hasChildren = row.children.length > 0;
        row.loaded = true;

        return row;
    }

    public mapToBusinessObject(group: Group<Raw>): GroupBO {
        return {
            ...group,
            [BO_ID]: String(group.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: group.name,
        };
    }
}
