import { BREADCRUMB_RESOLVER, ResolveBreadcrumbFn } from '@admin-ui/core';
import { AppStateService, SetUIFocusEntity } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, Type } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FieldType } from '@gentics/mesh-models';
import { of } from 'rxjs';
import { MeshField } from '../../models/mesh-browser-models';
import { MeshBrowserLoaderService } from '../../providers';

@Component({
    selector: 'gtx-mesh-browser-editor',
    templateUrl: './mesh-browser-editor.component.html',
    styleUrls: ['./mesh-browser-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserEditorComponent implements OnInit {

    private currentNodeId: string;

    private project: string;

    private language: string;

    public fields: Array<MeshField> = [];


    static [BREADCRUMB_RESOLVER]: ResolveBreadcrumbFn = (route, injector) => {
        const appState = injector.get<AppStateService>(AppStateService as Type<AppStateService>);
        return of({ title: 'Node Content', doNotTranslate: true });
    }

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected route: ActivatedRoute,
        protected router: Router,
        protected appState: AppStateService,
        protected loader: MeshBrowserLoaderService,
    ) { }

    ngOnInit(): void {
        console.log('init');
        console.log('r: ',this.route.snapshot);

        this.route.parent.params.subscribe((params) => {
            console.log(params);
        });


        this.project = 'Manual'; // fix me
        this.language = 'en';
        this.currentNodeId = this.route.snapshot.params.id;
        this.init()
    }

    async init(): Promise<void> {
        const delay = ms => new Promise(res => setTimeout(res, ms));
        await delay(100); // todo: remove me after fixing the init issue of mesh client

        await this.mapResponseToSchemaFields()
        this.changeDetector.markForCheck();
    }

    private async mapResponseToSchemaFields(): Promise<void> {
        const response = await this.loader.getNodeByUuid(this.project, this.currentNodeId)

        console.log(response)

        if (!response.fields) {
            return;
        }

        for (const [key, value] of Object.entries(response.fields)) {
            if (typeof value === 'string') {
                this.fields.push({
                    label: key,
                    value: value,
                    type: FieldType.STRING,
                })
            }
            if (typeof value === 'object') {
                this.fields.push({
                    label: key,
                    value: `${this.loader.getMeshUrl()}/${this.project}/nodes/${this.currentNodeId}/binary/binarycontent?lang=${this.language}`,
                    type: FieldType.BINARY,
                })
            }
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
