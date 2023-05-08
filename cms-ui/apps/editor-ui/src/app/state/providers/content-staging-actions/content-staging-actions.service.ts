import { Injectable } from '@angular/core';
import { StageableItem, contentPackageSchema } from '@editor-ui/app/common/models';
import { Api } from '@editor-ui/app/core/providers/api';
import { ContentPackageBO, Page } from '@gentics/cms-models';
import { normalize, schema } from 'normalizr';
import { map } from 'rxjs/operators';
import {
    AddContentStagingEntryAction,
    AddEntitiesAction,
    ClearEntitiesAction,
    ContentPackageErrorAction,
    ContentPackageSuccessAction,
    LoadContentPackagesAction,
    RemoveContentStagingEntryAction,
} from '../../modules';
import { ApplicationStateService } from '../application-state/application-state.service';

@Injectable()
export class ContentStagingActionsService {

    constructor(
        private api: Api,
        private appState: ApplicationStateService,
    ) { }

    loadPackages(): void {
        this.appState.dispatch(new LoadContentPackagesAction());
        this.appState.dispatch(new ClearEntitiesAction('contentPackage'));

        this.api.contentStaging.listContentPackages().pipe(
            map(res => res.items.map(pkg => ({ ...pkg, id: pkg.name }) as ContentPackageBO)),
        ).subscribe(map => {
            const normalized = normalize(map, new schema.Array(contentPackageSchema));
            this.appState.dispatch(new AddEntitiesAction(normalized));
            this.appState.dispatch(new ContentPackageSuccessAction());
        }, err => {
            this.appState.dispatch(new ContentPackageErrorAction(err));
        });
    }

    async stageAllPageVariants(page: Page, pkg: string): Promise<void> {
        const allPages = this.appState.now.entities.page;
        const allIds: string[] = Object.values(page.languageVariants)
            .map(variantId => allPages[variantId]?.globalId);

        for (const idToStage of allIds) {
            await this.api.contentStaging.assignEntityToContentPackage(pkg, 'page', idToStage).toPromise();
            this.appState.dispatch(new AddContentStagingEntryAction(idToStage, {
                included: true,
                packageName: pkg,
                recent: true,
            }));
        }
    }

    async unstageAllPageVariants(page: Page, pkg: string): Promise<void> {
        const allPages = this.appState.now.entities.page;
        const allIds: string[] = Object.values(page.languageVariants)
            .map(variantId => allPages[variantId]?.globalId);

        for (const idToUnstage of allIds) {
            await this.api.contentStaging.removeEntityFromContentPackage(pkg, 'page', idToUnstage).toPromise();
            this.appState.dispatch(new RemoveContentStagingEntryAction(idToUnstage));
        }
    }

    async stageItem(item: StageableItem, pkg: string, recursive: boolean = false): Promise<void> {
        await this.api.contentStaging.assignEntityToContentPackage(pkg, item.type, item.globalId, {
            recursive: recursive,
        }).toPromise();

        this.appState.dispatch(new AddContentStagingEntryAction(item.globalId, {
            included: true,
            packageName: pkg,
            recent: true,
        }));
    }

    async unstageItem(item: StageableItem, pkg: string): Promise<void> {
        await this.api.contentStaging.removeEntityFromContentPackage(pkg, item.type, item.globalId).toPromise();
        this.appState.dispatch(new RemoveContentStagingEntryAction(item.globalId));
    }
}
