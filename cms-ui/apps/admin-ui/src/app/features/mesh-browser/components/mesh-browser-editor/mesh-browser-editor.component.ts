import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FieldType, NodeResponse, SchemaResponse } from '@gentics/mesh-models';
import { MeshRestClientResponse } from '@gentics/mesh-rest-client';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { ChangesOf } from '@gentics/ui-core';
import { ROUTE_MESH_BRANCH_ID, ROUTE_MESH_CURRENT_NODE_ID, ROUTE_MESH_LANGUAGE, ROUTE_MESH_PROJECT_ID } from '../../../../common';
import { AppStateService } from '../../../../state/providers/app-state/app-state.service';
import { SetUIFocusEntity } from '../../../../state/ui/ui.actions';
import { BreadcrumbNode } from '../../models/mesh-browser-models';
import { MeshBrowserImageService, MeshBrowserLoaderService, MeshBrowserNavigatorService } from '../../providers';

export interface DisplayField {
    id: string;
    label: string;
    value: string;
    type: FieldType;
}

@Component({
    selector: 'gtx-mesh-browser-editor',
    templateUrl: './mesh-browser-editor.component.html',
    styleUrls: ['./mesh-browser-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class MeshBrowserEditorComponent implements OnChanges {

    public readonly FieldType = FieldType;

    @Input({ alias: ROUTE_MESH_PROJECT_ID })
    public project: string;

    @Input({ alias: ROUTE_MESH_CURRENT_NODE_ID })
    public node: string;

    @Input({ alias: ROUTE_MESH_BRANCH_ID })
    public branch: string;

    @Input({ alias: ROUTE_MESH_LANGUAGE })
    public language: string;

    public loading = false;

    public fields: Array<DisplayField> = [];
    public title: string;
    public version: string;
    public breadcrumb: BreadcrumbNode[] = [];
    public isContainer = false;

    private availableLanguages: string[] = [];
    private resolvedProject: string;
    private loadRequest: MeshRestClientResponse<any> | null = null;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected route: ActivatedRoute,
        protected router: Router,
        private appState: AppStateService,
        protected mesh: MeshRestClientService,
        protected loader: MeshBrowserLoaderService,
        protected imageService: MeshBrowserImageService,
        protected navigator: MeshBrowserNavigatorService,
    ) { }

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.project || changes.node || this.branch || changes.language) {
            this.resolveContent();
        }
    }

    public identify(_index: number, field: DisplayField): string {
        return field.id;
    }

    public loadNode(nodeUuid: string): void {
        if (nodeUuid == null) {
            return;
        }

        this.navigator.navigateToDetails(this.route, {
            project: this.project,
            branch: this.branch,
            node: nodeUuid,
            language: this.language,
        });
    }

    private async resolveContent(): Promise<void> {
        this.loading = true;
        this.changeDetector.markForCheck();

        if (this.loadRequest) {
            this.loadRequest.cancel();
        }

        if (this.project !== this.resolvedProject) {
            this.availableLanguages = ((await this.mesh.language.list(this.project).send()).data ?? [])
                .map((lang) => lang.languageTag)
                .sort((lang) => lang === this.language ? -1 : 1);
            this.resolvedProject = this.project;
        }

        this.loadRequest = this.mesh.nodes.get(this.project, this.node, {
            lang: this.availableLanguages.join(','),
            branch: this.branch,
            // FIXME: Has to fixed in the Mesh JS Client; Ticket SUP-17656
            fields: [['uuid', 'fields', 'version', 'displayName', 'schema', 'breadcrumb'].join(',') as any],
        });

        try {
            const response: NodeResponse = await this.loadRequest.send();

            const schema = await this.mesh.schemas.get(response.schema.uuid).send();

            this.title = response?.displayName ?? response.uuid;
            this.version = response.version;
            this.breadcrumb = response.breadcrumb;
            this.fields = this.createDisplayFields(response, schema);
            this.isContainer = schema.container ?? false;
            this.loading = false;

            this.changeDetector.markForCheck();
        } catch (err) {
            this.loading = false;
            this.changeDetector.markForCheck();
            // TODO: Ignore abort error
        }
    }

    private createDisplayFields(node: NodeResponse, schema: SchemaResponse): DisplayField[] {
        return schema.fields.map((field) => {
            let value: any = node.fields[field.name];

            switch (field.type) {
                case FieldType.BINARY: {
                    value = this.getImagePath(field.name);
                    break;
                }
            }

            return {
                id: field.name,
                label: field.label || field.name,
                type: field.type,
                value,
            };
        });
    }

    private getImagePath(fieldName: string): string {
        return this.imageService.getImageUrlForBinaryField(
            this.project,
            this.node,
            this.branch,
            this.language,
            fieldName,
        );
    }

    async detailsClose(): Promise<void> {
        const relativeToRoute = this.route.parent.parent || this.route.parent;
        const navigationSucceeded = await this.router.navigate([{ outlets: { detail: null } }], { relativeTo: relativeToRoute });
        if (navigationSucceeded) {
            this.appState.dispatch(new SetUIFocusEntity(null, null, null));
        }
    }

}
