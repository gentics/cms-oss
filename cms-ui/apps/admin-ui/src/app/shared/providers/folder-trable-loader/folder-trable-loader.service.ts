import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, FolderBO } from '@admin-ui/common';
import { BaseTrableLoaderService } from '../../providers/base-trable-loader/base-trable-loader.service';
import { Injectable } from '@angular/core';
import { Folder, GcmsPermission, INVERSE_GCMS_PERMISSIONS, Node } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { TrableRow } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface FolderTrableLoaderOptions {
    includeRoot?: boolean;
    rootId?: number;
}

@Injectable()
export class FolderTrableLoaderService extends BaseTrableLoaderService<Folder, FolderBO, FolderTrableLoaderOptions> {

    constructor(
        protected api: GcmsApi,
    ) {
        super();
    }

    protected loadEntityRow(entity: FolderBO, options?: FolderTrableLoaderOptions): Observable<FolderBO> {
        return this.api.folders.getItem(entity.id, 'folder').pipe(
            map(res => this.mapToBusinessObject(res.folder)),
        );
    }

    protected loadEntityChildren(parent: FolderBO | null, options?: FolderTrableLoaderOptions): Observable<FolderBO[]> {
        let parentId = options?.rootId ?? 0;
        let loader: Observable<Folder[]>;

        if (!parent && options?.includeRoot) {
            loader = this.api.folders.getItem(parentId, 'folder').pipe(map(res => [res.folder]));
        } else {
            parentId = parent?.id ?? parentId;
            loader = this.api.folders.getFolders(parentId).pipe(map(res => res.folders));
        }

        return loader.pipe(
            map(folders => folders.map(folder => this.mapToBusinessObject(folder))),
        );
    }

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    protected override hasChildren(entity: FolderBO, options?: FolderTrableLoaderOptions): boolean {
        return entity.hasSubfolders;
    }

    public mapToBusinessObject(folder: Folder): FolderBO {
        return {
            ...folder,
            [BO_ID]: `${(folder as any as Node).folderId || folder.id}${folder.channelId > 0 ? `:${folder.channelId}` : ''}`,
            [BO_PERMISSIONS]: this.privilegesToPermissions(folder),
            [BO_DISPLAY_NAME]: folder.name,
        };
    }

    protected privilegesToPermissions(folder: Folder): GcmsPermission[] {
        return Object.entries(folder?.privilegeMap?.privileges || {})
            .filter(([, permitted]) => permitted)
            .map(([key]) => INVERSE_GCMS_PERMISSIONS[key])
            .filter(perm => !!perm);
    }

    protected override mapToTrableRow(
        entity: FolderBO,
        parent?: TrableRow<FolderBO>,
        options?: FolderTrableLoaderOptions,
    ): TrableRow<FolderBO> {
        const mapped = super.mapToTrableRow(entity, parent, options);
        if (!entity.hasSubfolders) {
            mapped.hasChildren = false;
            mapped.loaded = true;
        }

        return mapped;
    }

}
