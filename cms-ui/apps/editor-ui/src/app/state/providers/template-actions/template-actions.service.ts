import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { TemplateLinkRequestOptions } from '../../../common/models';
import { Api } from '../../../core/providers/api';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';

@Injectable()
export class TemplateActionsService {

    constructor(
        private api: Api,
        private errorHandler: ErrorHandler,
        private notification: I18nNotification,
    ) {}

    /**
     * Links a template to folders
     *
     * @param templateId to be linked to `folderIds`
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
     * @param templateIds to be linked to `folderIds`
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
     * @param templateId to be unlinked from folders listed in options
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
     * @param templateIds to be unlinked from `folderIds`
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
