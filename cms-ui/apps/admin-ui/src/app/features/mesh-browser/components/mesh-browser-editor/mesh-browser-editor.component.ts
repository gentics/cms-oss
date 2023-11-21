import { ROUTE_MESH_BRANCH_ID, ROUTE_MESH_CURRENT_NODE_ID, ROUTE_MESH_LANGUAGE, ROUTE_MESH_PROJECT_ID } from '@admin-ui/common';
import { AppStateService, SetUIFocusEntity } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FieldType, SchemaField } from '@gentics/mesh-models';
import { MeshField } from '../../models/mesh-browser-models';
import { MeshBrowserImageService, MeshBrowserLoaderService } from '../../providers';


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

    public fields: Array<MeshField> = [];

    public title: string;


    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected route: ActivatedRoute,
        protected router: Router,
        protected appState: AppStateService,
        protected loader: MeshBrowserLoaderService,
        protected imageService: MeshBrowserImageService,
    ) { }

    ngOnInit(): void {
        this.updateComponent()
    }

    async updateComponent(): Promise<void> {
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

        if (!response.fields) {
            return;
        }

        this.title = response.fields.name as unknown as string;

        if (!this.title) {
            this.title = this.currentNodeUuid;
        }

        this.fields = [];

        for (const [key, value] of Object.entries(response.fields)) {
            // type should be from api response
            const fieldType = this.getFieldType(value);

            if (fieldType === FieldType.STRING || fieldType === FieldType.LIST) {
                this.fields.push({
                    label: key,
                    value: value as unknown as string,
                    type: fieldType,
                })
            }
            else if (fieldType === FieldType.NODE) {
                const fieldObject = value as unknown as object;
                this.fields.push({
                    label: key,
                    value: fieldObject['uuid'],
                    type: FieldType.STRING,
                })
            }
            else if (fieldType === FieldType.BINARY) {
                this.fields.push({
                    label: key,
                    value: this.getImagePath(key),
                    type: FieldType.BINARY,
                })
            }
        }
    }

    private getImagePath(fieldName: string) {
        return this.imageService.getImageUrlForBinaryField(
            this.project,
            this.currentNodeUuid,
            this.currentBranchUuid,
            this.currentLanguage,
            fieldName,
        );
    }

    private getFieldType(field: SchemaField): FieldType {
        if (typeof field === 'object') {
            if (field.constructor === Array) {
                return FieldType.LIST
            }

            const fieldObject = field as unknown as object;
            if(fieldObject['binaryUuid']) {
                return FieldType.BINARY;
            }
            else if(fieldObject['uuid']) {
                return FieldType.NODE;
            }
        }
        else if (typeof field === 'string') {
            return FieldType.STRING;
        }
    }

    async detailsClose(): Promise<void> {
        const relativeToRoute = this.route.parent.parent || this.route.parent;
        const navigationSucceeded = await this.router.navigate([ { outlets: { detail: null } } ], { relativeTo: relativeToRoute });
        if (navigationSucceeded) {
            this.appState.dispatch(new SetUIFocusEntity(null, null, null));
        }
    }

}
