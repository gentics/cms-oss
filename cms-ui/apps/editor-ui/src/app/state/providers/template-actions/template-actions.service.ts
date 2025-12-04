import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { TemplateLinkRequestOptions } from '@gentics/cms-models';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { Api } from '../../../core/providers/api';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';

@Injectable()
export class TemplateActionsService {

    constructor(
        private api: Api,
        private errorHandler: ErrorHandler,
        private notification: I18nNotificationService,
    ) {}

    /**
     * Links a template to folders
     *
     * @param nodeId The ID of the node the folders reside in
     * @param templateId to be linked to `folderIds`
     * @param folderIds The Ids the template should be linked to
     * @param recursive If the template should be applied to all sub-folders as well.
     * @returns TRUE if action was successful, otherwise FALSE
     */
    linkTemplateToFolders(
        nodeId: number,
        templateId: number,
        folderIds: number[],
        recursive: boolean = false,
    ): Observable<boolean> {
        const options: TemplateLinkRequestOptions = {
            folderIds,
            nodeId,
            recursive,
        };
        return this.api.template.linkTemplateToFolders(templateId, options).pipe(
            switchMap(() => of(true)),
            tap(() => this.notification.show({
                type: 'success',
                message: 'message.template_link_success_singular',
            })),
            catchError((error) => {
                this.errorHandler.catch(error, { notification: true });
                return of(false);
            }),
        );
    }

    /**
     * Links multiple templates to folders
     *
     * @param nodeId The ID of the node the folders reside in
     * @param templateIds The ids of the templates which should be linked
     * @param folderIds The Ids the template should be linked to
     * @param recursive If the template should be applied to all sub-folders as well.
     * @returns TRUE if action was successful, otherwise FALSE
     */
    linkTemplatesToFolders(
        nodeId: number,
        templateIds: number[],
        folderIds: number[],
        recursive: boolean = false,
    ): Observable<boolean> {
        return this.api.template.linkTemplatesToFolders({
            nodeId,
            templateIds,
            folderIds,
            recursive,
        }).pipe(
            map(response => response.responseInfo.responseCode === 'OK'),
            tap(() => this.notification.show({
                type: 'success',
                message: 'message.template_link_success_plural',
            })),
            catchError((error) => {
                this.errorHandler.catch(error, { notification: true });
                return of(false);
            }),
        );
    }

    /**
     * Unlinks a template from folders
     *
     * @param nodeId The ID of the node the folders reside in
     * @param templateId to be unlinked to `folderIds`
     * @param folderIds The Ids the template should be linked to
     * @param recursive If the template should be applied to all sub-folders as well.
     * @returns TRUE if action was successful, otherwise FALSE
     */
    unlinkTemplateFromFolders(
        nodeId: number,
        templateId: number,
        folderIds: number[],
        recursive: boolean = false,
    ): Observable<boolean> {
        const options: TemplateLinkRequestOptions = {
            folderIds,
            nodeId,
            recursive,
        };
        return this.api.template.unlinkTemplateFromFolders(templateId, options).pipe(
            switchMap(() => of(true)),
            tap(() => this.notification.show({
                type: 'success',
                message: 'message.template_unlink_success_singular',
            })),
            catchError((error) => {
                this.errorHandler.catch(error, { notification: true });
                return of(false);
            }),
        );
    }

    /**
     * Unlinks multiple templates from folders
     *
     * @param nodeId The ID of the node the folders reside in
     * @param templateIds to be unlinked to `folderIds`
     * @param folderIds The Ids the template should be linked to
     * @param recursive If the template should be applied to all sub-folders as well.
     * @returns TRUE if action was successful, otherwise FALSE
     */
    unlinkTemplatesFromFolders(
        nodeId: number,
        templateIds: number[],
        folderIds: number[],
        recursive: boolean = false,
    ): Observable<boolean> {
        return this.api.template.unlinkTemplatesFromFolders({
            nodeId,
            templateIds,
            folderIds,
            recursive,
        }).pipe(
            map(response => response.responseInfo.responseCode === 'OK'),
            tap(() => this.notification.show({
                type: 'success',
                message: 'message.template_unlink_success_plural',
            })),
            catchError((error) => {
                this.errorHandler.catch(error, { notification: true });
                return of(false);
            }),
        );
    }

}
