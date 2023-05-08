import { BO_DISPLAY_NAME, BO_ID, BO_NODE_ID, BO_PERMISSIONS, ContentItem, ContentItemBO, ContentItemTypes } from '@admin-ui/common';
import { BaseTrableLoaderService } from '@admin-ui/core/providers/base-trable-loader/base-trable-loader.service';
import { Injectable } from '@angular/core';
import { Folder, GcmsPermission, INVERSE_GCMS_PERMISSIONS } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { flatMap } from 'lodash-es';
import { forkJoin, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface ContentItemTrableLoaderOptions {
    includeRoot?: boolean;
    rootId?: number;
    selectable: ContentItemTypes[];
    listable: Exclude<ContentItemTypes, 'folder' | 'node' | 'channel'>[];
}

@Injectable()
export class ContentItemTrableLoaderService extends BaseTrableLoaderService<ContentItem, ContentItemBO, ContentItemTrableLoaderOptions> {

    constructor(
        protected api: GcmsApi,
    ) {
        super();
    }

    protected loadEntityChildren(parent: ContentItemBO | null, options?: ContentItemTrableLoaderOptions): Observable<ContentItemBO[]> {
        let parentId = options?.rootId ?? 0;
        let loader: Observable<ContentItem[]>;

        if (!parent && options?.includeRoot) {
            loader = this.api.folders.getItem(parentId, 'folder').pipe(
                map(res => [res.folder]),
            );
        } else {
            parentId = parent?.id ?? parentId;
            loader = this.api.folders.getFolders(parentId).pipe(
                map(res => res.folders),
            );
            if (parentId) {
                loader = forkJoin([loader, ...options.listable.map(type => this.getTypedLoader(type, parentId))]).pipe(
                    map(res => flatMap(res)),
                );
            }
        }

        return loader.pipe(
            map(entities => entities.map(entity => this.mapToBusinessObject(entity, parent))),
        );
    }

    protected getTypedLoader(type: ContentItemTypes, parentId?: number): Observable<ContentItem[]> {
        switch (type) {
            case 'folder':
                return this.api.folders.getFolders(parentId).pipe(
                    map(res => res.folders),
                );
            case 'file':
                return this.api.folders.getFiles(parentId).pipe(
                    map(res => res.files),
                );
            case 'form':
                return this.api.folders.getForms(parentId).pipe(
                    map(res => res.items),
                );
            case 'image':
                return this.api.folders.getImages(parentId).pipe(
                    map(res => res.files),
                );
            case 'page':
                return this.api.folders.getPages(parentId).pipe(
                    map(res => res.pages),
                );
            case 'template':
                return this.api.folders.getTemplates(parentId).pipe(
                    map(res => res.templates),
                );
        }
    }

    protected override hasChildren(entity: ContentItemBO, options?: ContentItemTrableLoaderOptions): boolean {
        if (entity.type === 'node' || entity.type === 'channel') {
            return true;
        } else if (entity.type === 'folder') {
            return entity.hasSubfolders;
        } else {
            return false;
        }
    }

    protected override canBeSelected(entity: ContentItemBO, options?: ContentItemTrableLoaderOptions): boolean {
        return options.selectable.includes(entity.type);
    }

    public mapToBusinessObject(item: ContentItem, parent: ContentItemBO | null): ContentItemBO {
        let nodeId: number;

        if (item.type === 'node' || item.type === 'channel' || item.type === 'folder') {
            nodeId = (item as any as Folder).nodeId;
        } else if (parent.type === 'node' || parent.type === 'channel' || parent.type === 'folder') {
            nodeId = (parent as any as Folder).nodeId;
        } else {
            nodeId = parent[BO_NODE_ID];
        }

        return {
            ...item,
            // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
            [BO_ID]: `${item.type}.${item.id}${(item as any).nodeId ? `:${(item as any).nodeId}` : ''}`,
            [BO_PERMISSIONS]: this.privilegesToPermissions(item),
            [BO_DISPLAY_NAME]: item.name,
            [BO_NODE_ID]: nodeId,
        };
    }

    protected privilegesToPermissions(item: ContentItem): GcmsPermission[] {
        if (item.type !== 'folder') {
            return [];
        }

        return Object.entries(item?.privilegeMap?.privileges || {})
            .filter(([, permitted]) => permitted)
            .map(([key]) => INVERSE_GCMS_PERMISSIONS[key])
            .filter(perm => !!perm);
    }

}
