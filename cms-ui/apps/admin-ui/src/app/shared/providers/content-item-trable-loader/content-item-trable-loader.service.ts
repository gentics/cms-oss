import { BO_DISPLAY_NAME, BO_ID, BO_NODE_ID, BO_PERMISSIONS, ContentItem, ContentItemBO, ContentItemTypes } from '@admin-ui/common';
import { BaseTrableLoaderService } from '@admin-ui/core/providers/base-trable-loader/base-trable-loader.service';
import { Injectable } from '@angular/core';
import { Folder, GcmsPermission, INVERSE_GCMS_PERMISSIONS } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { flatMap } from 'lodash-es';
import { forkJoin, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

type ListableEntityType = Exclude<ContentItemTypes, 'folder' | 'node' | 'channel'>;

const ENTITY_TYPES: ListableEntityType[] = [
    'file',
    'form',
    'image',
    'page',
    'template',
];

export interface ContentItemTrableLoaderOptions {
    includeRoot?: boolean;
    rootId?: number;
    selectable: ContentItemTypes[];
    listable: ListableEntityType[];
}

@Injectable()
export class ContentItemTrableLoaderService extends BaseTrableLoaderService<ContentItem, ContentItemBO, ContentItemTrableLoaderOptions> {

    constructor(
        protected client: GCMSRestClientService,
    ) {
        super();
    }

    protected loadEntityRow(entity: ContentItemBO, options?: ContentItemTrableLoaderOptions): Observable<ContentItemBO> {
        switch (entity.type) {
            case 'channel':
            case 'node':
                return this.client.node.get(entity.id).pipe(
                    map(res => this.mapToBusinessObject(res.node, null)),
                );
            case 'file':
                return this.client.file.get(entity.id).pipe(
                    map(res => this.mapToBusinessObject(res.file, null)),
                );
            case 'folder':
                return this.client.folder.get(entity.id).pipe(
                    map(res => this.mapToBusinessObject(res.folder, null)),
                );
            case 'form':
                return this.client.form.get(entity.id).pipe(
                    map(res => this.mapToBusinessObject(res.item, null)),
                );
            case 'image':
                return this.client.image.get(entity.id).pipe(
                    map(res => this.mapToBusinessObject(res.image, null)),
                );
            case 'page':
                return this.client.page.get(entity.id).pipe(
                    map(res => this.mapToBusinessObject(res.page, null)),
                );
            case 'template':
                return this.client.template.get(entity.id).pipe(
                    map(res => this.mapToBusinessObject(res.template, null)),
                );
        }
    }

    protected loadEntityChildren(parent: ContentItemBO | null, options?: ContentItemTrableLoaderOptions): Observable<ContentItemBO[]> {
        let parentId = options?.rootId ?? 0;
        let loader: Observable<ContentItem[]>;

        if (!parent && options?.includeRoot) {
            loader = this.client.folder.folders(parentId).pipe(
                map(res => [res.folder]),
            );
        } else {
            parentId = parent?.id ?? parentId;
            loader = this.client.folder.folders(parentId).pipe(
                map(res => res.folders),
            );
            if (parentId) {
                const typeLoaders = (options.listable || [])
                    .filter(type => ENTITY_TYPES.includes(type))
                    .map(type => this.getTypedChildrenLoader(type, parentId));
                loader = forkJoin([loader, ...typeLoaders]).pipe(
                    map(res => flatMap(res)),
                );
            }
        }

        return loader.pipe(
            map(entities => entities.map(entity => this.mapToBusinessObject(entity, parent))),
        );
    }

    protected getTypedChildrenLoader(type: ContentItemTypes, parentId?: number): Observable<ContentItem[]> {
        switch (type) {
            case 'folder':
                return this.client.folder.folders(parentId).pipe(
                    map(res => res.folders),
                );
            case 'file':
                return this.client.folder.files(parentId).pipe(
                    map(res => res.files),
                );
            case 'form':
                return this.client.form.list({ folderId: parentId }).pipe(
                    map(res => res.items),
                );
            case 'image':
                return this.client.folder.images(parentId).pipe(
                    map(res => res.files),
                );
            case 'page':
                return this.client.folder.pages(parentId).pipe(
                    map(res => res.pages),
                );
            case 'template':
                return this.client.folder.templates(parentId).pipe(
                    map(res => res.templates),
                );
        }
    }

    protected override hasChildren(entity: ContentItemBO, options?: ContentItemTrableLoaderOptions): boolean {
        return entity.type === 'node' || entity.type === 'channel' || entity.type === 'folder';
    }

    protected override canBeSelected(entity: ContentItemBO, options?: ContentItemTrableLoaderOptions): boolean {
        return options.selectable.includes(entity.type);
    }

    public mapToBusinessObject(item: ContentItem, parent?: ContentItemBO | null): ContentItemBO {
        let nodeId: number;

        if (item.type === 'node' || item.type === 'channel' || item.type === 'folder') {
            nodeId = (item as any as Folder).nodeId;
        } else if (parent != null) {
            if (parent.type === 'node' || parent.type === 'channel' || parent.type === 'folder') {
                nodeId = (parent as any as Folder).nodeId;
            } else {
                nodeId = parent[BO_NODE_ID];
            }
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
