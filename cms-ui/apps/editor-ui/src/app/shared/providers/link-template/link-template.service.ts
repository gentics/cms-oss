import { Injectable } from '@angular/core';
import { Raw, Template } from '@gentics/cms-models';
import { concat, forkJoin, from, Observable } from 'rxjs';
import { map, switchMap, tap, toArray } from 'rxjs/operators';
import { FolderActionsService, TemplateActionsService } from '../../../state';

/**
 * This service is for managing assignments of nodes, folders and templates
 */
@Injectable({
    providedIn: 'root',
})
export class LinkTemplateService {

    constructor(
        private templateActions: TemplateActionsService,
        private folderActions: FolderActionsService,
    ) { }

    /**
     * Set the linked templates of a folder
     * @param nodeId of node containing `folderId` and `templateIds`
     * @param folderId the `templateIds` will be linked to
     * @param templateIds to linked to `folderId`
     * @param recursive if TRUE selected templates will be linked to all child folders of `folderId` as well
     * @param searchTerm limits the amount of total node templates filtering by name enabling to optimize amount of requests required
     * @return TRUE if all requests where successful, otherwise return FALSE
     */
    changeTemplatesOfFolder(
        nodeId: number,
        folderId: number,
        templateIds: number[],
        recursive: boolean = false,
        searchTerm: string,
    ): Observable<boolean> {
        return forkJoin([
            this.folderActions.getTemplatesRaw(nodeId, folderId, true),
            this.folderActions.getAllTemplatesOfNode(nodeId, searchTerm),
        ]).pipe(
            // extract IDs from objects
            map(([templateIdsCurrentlyLinked, allTemplates]: [Template<Raw>[], Template<Raw>[]]) => [
                templateIdsCurrentlyLinked.map(template => template.id),
                allTemplates.map(template => template.id),
            ]),
            // link desired templates and unlink unwanted templates
            switchMap(([templateIdsCurrentlyLinked, allTemplates]: [number[], number[]]) => {
                // use different approaches to minimize amount of requests required:
                if (recursive === false) {
                    const templatesShallBeLinked = templateIds;
                    const templatesShallNotBeLinked = allTemplates.filter(id => !templatesShallBeLinked.includes(id));
                    const templatesCurrentlyLinked = templateIdsCurrentlyLinked;
                    const templatesCurrentlyNotLinked = allTemplates.filter(id => !templatesCurrentlyLinked.includes(id));

                    const templatesToLink = templatesShallBeLinked.filter(id => !templatesCurrentlyLinked.includes(id));
                    const templatesToUnlink = templatesShallNotBeLinked.filter(id => !templatesCurrentlyNotLinked.includes(id));

                    return concat(
                        this.removeTemplatesFromFolder(nodeId, folderId, templatesToUnlink, recursive),
                        this.addTemplatesToFolder(nodeId, folderId, templatesToLink, recursive),
                    ).pipe(
                        toArray(),
                        map(successList => successList.every(isSuccessful => isSuccessful)),
                    );
                }

                // if all shall be linked
                if (allTemplates.length === templateIds.length) {
                    return this.addTemplatesToFolder(nodeId, folderId, templateIds, recursive);
                // if all shall be unlinked
                } else if (templateIds.length === 0) {
                    return this.removeTemplatesFromFolder(nodeId, folderId, allTemplates, recursive);
                }

                // needs to unlink all before linking (most expensive)
                return concat(
                    this.removeTemplatesFromFolder(nodeId, folderId, allTemplates, recursive),
                    this.addTemplatesToFolder(nodeId, folderId, templateIds, recursive),
                ).pipe(
                    toArray(),
                    map(successList => successList.every(isSuccessful => isSuccessful)),
                );
            }),
            // refresh templates in state
            switchMap(success => {
                return from(this.folderActions.getTemplates(folderId)).pipe(
                    map(() => success),
                );
            }),
        );
    }

    addTemplatesToFolder(
        nodeId: number,
        folderId: number,
        templateIds: number[],
        recursive: boolean,
    ): Observable<boolean> {
        return this.templateActions.linkTemplatesToFolders(nodeId, templateIds, [folderId], recursive)
    }

    removeTemplatesFromFolder(
        nodeId: number,
        folderId: number,
        templateIds: number[],
        recursive: boolean,
    ): Observable<boolean> {
        return this.templateActions.unlinkTemplatesFromFolders(nodeId, templateIds, [folderId], recursive);
    }

}
