import { ROUTE_MESH_BRANCH_ID, ROUTE_MESH_CURRENT_NODE_ID, ROUTE_MESH_LANGUAGE, ROUTE_MESH_PROJECT_ID } from '@admin-ui/common';
import { AppStateService, SchemasLoaded, SetUIFocusEntity } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FieldType } from '@gentics/mesh-models';
import { MeshField, SchemaContainer } from '../../models/mesh-browser-models';
import { MeshBrowserCanActivateGuard, MeshBrowserImageService, MeshBrowserLoaderService } from '../../providers';


@Component({
    selector: 'gtx-mesh-browser-editor',
    templateUrl: './mesh-browser-editor.component.html',
    styleUrls: ['./mesh-browser-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserEditorComponent  implements OnInit, OnChanges {

    @Input({ alias: ROUTE_MESH_PROJECT_ID})
    public project: string;

    @Input({ alias: ROUTE_MESH_CURRENT_NODE_ID})
    public currentNodeUuid: string;

    @Input({ alias: ROUTE_MESH_BRANCH_ID})
    public currentBranchUuid: string;

    @Input({ alias: ROUTE_MESH_LANGUAGE})
    public currentLanguage: string;

    private currentNodeSchemaName: string;

    public fields: Array<MeshField> = [];

    public title: string;


    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected route: ActivatedRoute,
        protected router: Router,
        protected appState: AppStateService,
        protected loader: MeshBrowserLoaderService,
        protected imageService: MeshBrowserImageService,
        protected activationGuard: MeshBrowserCanActivateGuard,
    ) { }

    async ngOnInit(): Promise<void> {
        const canActivate = await this.activationGuard.canActivate(this.route.snapshot)
        if (!canActivate) {
            this.detailsClose();
            return;
        }
    }

    async updateComponent(): Promise<void> {
        this.currentNodeSchemaName = await this.loader.getSchemaNameForNode(this.project, {nodeUuid: this.currentNodeUuid})
        await this.mapResponseToSchemaFields()
        this.changeDetector.markForCheck();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.currentNodeId || this.currentBranchUuid || changes.currentLanguage) {
            this.updateComponent();
        }
    }

    private async mapResponseToSchemaFields(): Promise<void> {
        const response = await this.loader.getNodeByUuid(this.project, this.currentNodeUuid, {
            lang: this.currentLanguage,
            branch: this.currentBranchUuid,
        })

        const currentSchema = await this.getCurrentSchema();

        this.title = response.fields.name as unknown as string;

        if (!this.title) {
            this.title = this.currentNodeUuid;
        }

        this.fields = [];

        for (const fieldDefinition of currentSchema.fields) {
            let fieldValue = response.fields[fieldDefinition.name] as unknown as string;

            if (fieldDefinition.type === FieldType.BINARY) {
                fieldValue = this.getImagePath(fieldDefinition.name);
            }

            if (fieldDefinition.type === FieldType.NODE) {
                console.log(fieldDefinition);
                const node = response.fields[fieldDefinition.name] as unknown as object;
                fieldValue = node['displayName'] ?? node['uuid'];
            }

            this.fields.push({
                label: fieldDefinition.name,
                value: fieldValue,
                type: fieldDefinition.type,
            });
        }
    }

    private async getCurrentSchema(): Promise<SchemaContainer> {
        let currentSchema = this.appState.now.mesh.schemas.find(schema => schema.name === this.currentNodeSchemaName)
        if( !currentSchema ){
            const schemas =  await this.loader.listProjectSchemas(this.project);
            currentSchema = schemas.find(schema => schema.name === this.currentNodeSchemaName)
            this.appState.dispatch(new SchemasLoaded(schemas));
        }

        return currentSchema;
    }

    private getImagePath(fieldName: string): string {
        return this.imageService.getImageUrlForBinaryField(
            this.project,
            this.currentNodeUuid,
            this.currentBranchUuid,
            this.currentLanguage,
            fieldName,
        );
    }

    async detailsClose(): Promise<void> {
        const relativeToRoute = this.route.parent.parent || this.route.parent;
        const navigationSucceeded = await this.router.navigate([ { outlets: { detail: null } } ], { relativeTo: relativeToRoute });
        if (navigationSucceeded) {
            this.appState.dispatch(new SetUIFocusEntity(null, null, null));
        }
    }

}
