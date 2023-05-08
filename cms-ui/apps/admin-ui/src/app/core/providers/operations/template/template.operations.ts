import { applyInstancePermissions, discard, PackageEntityOperations } from '@admin-ui/common';
import { Injectable, Injector } from '@angular/core';
import {
    EntityIdType,
    Folder,
    ModelType,
    Raw,
    Template,
    TemplateBO,
    TemplateCreateRequest,
    TemplateLinkListOptions,
    TemplateLinkRequestOptions,
    TemplateLinkResponse,
    TemplateFolderListRequest,
    TemplateRequestOptions,
    TemplateSaveOptions,
    TemplateSaveRequest,
    TemplateListRequest,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { forkJoin, Observable } from 'rxjs';
import { map, mergeMap, switchMap, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class TemplateOperations
    extends ExtendedEntityOperationsBase<'template'>
    implements PackageEntityOperations<TemplateBO<Raw>>
{

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entityManager: EntityManagerService,
        private notification: I18nNotificationService,
    ) {
        super(injector, 'template');
    }

    create(request: TemplateCreateRequest, notify: boolean = true): Observable<TemplateBO<Raw>> {
        return this.api.template.createTemplate(request).pipe(
            map(res => this.mapToBusinessObject(res.template)),
            tap(template => {
                this.entityManager.addEntity(this.entityIdentifier, template);

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'template.created',
                        translationParams: {
                            name: template.name,
                        },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a list of all templates and adds them to the AppState.
     */
    getAll(options?: TemplateFolderListRequest): Observable<TemplateBO<Raw>[]> {
        return this.api.template.getTemplates(options).pipe(
            map(res => applyInstancePermissions(res)),
            map(res => res.items.map(item => this.mapToBusinessObject(item))),
            tap(templates => this.entityManager.addEntities(this.entityIdentifier, templates)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single template and add it to the AppState.
     */
    get(templateId: EntityIdType, options?: TemplateRequestOptions): Observable<TemplateBO<Raw>> {
        return this.api.template.getTemplate(templateId, options).pipe(
            map(res => this.mapToBusinessObject(res.template)),
            tap(template => this.entityManager.addEntity(this.entityIdentifier, template)),
            this.catchAndRethrowError(),
        );
    }

    getAllFromPackage(packageId: string, options?: any): Observable<TemplateBO<ModelType.Raw>[]> {
        return this.api.devTools.getTemplates(packageId, options).pipe(
            map(res => res.items.map(item  => this.mapToBusinessObject(item))),
            this.catchAndRethrowError(),
        );
    }

    getFromPackage(packageId: string, entityId: string): Observable<TemplateBO<ModelType.Raw>> {
        return this.api.devTools.getTemplate(packageId, entityId).pipe(
            map(res => this.mapToBusinessObject(res.template)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a list of all templates and adds them to the AppState.
     */
    getAllOfAllNodes(options?: TemplateListRequest): Observable<TemplateBO<Raw>[]> {
        return this.api.node.getNodes().pipe(
            map(res => res.items.map(node => node.id)),
            mergeMap(nodeIds => {
                const requests = nodeIds.map(nodeId => this.getAllOfNode(nodeId, options));
                return forkJoin(requests).pipe(
                    map(items => [].concat.apply([], items)),
                );
            }),
        );
    }

    getAllOfNode(nodeId: number, options?: TemplateListRequest): Observable<TemplateBO<Raw>[]> {
        return this.api.node.getNodeTemplates(nodeId, options).pipe(
            map(res => applyInstancePermissions(res)),
            map(res => res.items.map(item => this.mapToBusinessObject(item))),
            tap(templates => this.entityManager.addEntities(this.entityIdentifier, templates)),
            this.catchAndRethrowError(),
        );
    }

    unlock(templateId: EntityIdType): Observable<void> {
        return this.api.template.unlock(templateId).pipe(
            switchMap(() => this.entityManager.getEntity(this.entityIdentifier, templateId)),
            discard(normalizedTemplate => {
                // Denormalize the template
                const template = this.entityManager.denormalizeEntity(this.entityIdentifier, normalizedTemplate);
                // Unlock the template in the state
                template.locked = false;
                this.entityManager.addEntity(this.entityIdentifier, template);
            }),
        );
    }

    update(templateId: EntityIdType, body: TemplateSaveRequest, options?: TemplateSaveOptions): Observable<TemplateBO<Raw>> {
        return this.api.template.updateTemplate(templateId, body).pipe(
            // Load the template again, as currently the API does not respond with the updated model.
            switchMap(() => this.get(templateId, options)),
            tap(template => {
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_updated',
                    translationParams: {
                        name: template.name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    delete(templateId: EntityIdType): Observable<void> {
        return this.api.template.deleteTemplate(templateId);
    }

    getLinkedFolders(templateId: EntityIdType, options?: TemplateLinkListOptions): Observable<Folder<Raw>[]> {
        return this.api.template.getLinkedFolders(templateId, options).pipe(
            map(res => res.items),
            this.catchAndRethrowError(),
        );
    }

    linkFolders(templateId: EntityIdType, body: TemplateLinkRequestOptions): Observable<TemplateLinkResponse> {
        return this.api.template.linkTemplateToFolders(templateId, body);
    }

    unlinkFolders(templateId: EntityIdType, body: TemplateLinkRequestOptions): Observable<TemplateLinkResponse> {
        return this.api.template.unlinkTemplateFromFolders(templateId, body);
    }

    hasViewPermission(templateId: EntityIdType): Observable<boolean> {
        return this.api.permissions.getTemplateViewPermissions(templateId).pipe(
            map(res => res.granted),
        );
    }

    hasEditPermission(templateId: EntityIdType): Observable<boolean> {
        return this.api.permissions.getTemplateEditPermissions(templateId).pipe(
            map(res => res.granted),
        );
    }

    hasDeletePermission(templateId: EntityIdType): Observable<boolean> {
        return this.api.permissions.getTemplateDeletePermissions(templateId).pipe(
            map(res => res.granted),
        );
    }

    private mapToBusinessObject<T extends ModelType>(template: Template<T>): TemplateBO<T> {
        return {
            ...template,
            id: template.id.toString(),
        };
    }

}
