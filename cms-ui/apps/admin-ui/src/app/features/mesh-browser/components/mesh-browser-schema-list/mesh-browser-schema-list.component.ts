import { AppStateService, SchemasLoaded } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BranchReference } from '@gentics/mesh-models';
import { SchemaContainer } from '../../models/mesh-browser-models';
import { MeshBrowserLoaderService } from '../../providers';


@Component({
    selector: 'gtx-mesh-browser-schema-list',
    templateUrl: './mesh-browser-schema-list.component.html',
    styleUrls: ['./mesh-browser-schema-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserSchemaListComponent implements OnInit, OnChanges {

    @Input()
    public currentProject: string;

    @Input()
    public languages: Array<string> = [];

    @Input()
    public currentLanguage: string;

    @Input()
    public currentBranch: BranchReference;

    @Output()
    public nodeChange = new EventEmitter<string>();

    @Input()
    public currentNodeUuid: string;

    public schemas: Array<SchemaContainer> = [];

    public noSchemaElements = true;


    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
        protected route: ActivatedRoute,
        protected appState: AppStateService,
    ) { }


    async ngOnInit(): Promise<void> {
        if (!this.currentNodeUuid) {
            this.currentNodeUuid = await this.loader.getRootNodeUuid(this.currentProject);
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.noSchemaElements = true;

        this.route.params.subscribe((params) => {
            if (params.parent) {
                this.currentNodeUuid = params.parent;
            }
        });

        let nodeUuid = this.currentNodeUuid;

        if (changes?.currentNodeUuid?.currentValue) {
            nodeUuid = changes?.currentNodeUuid?.currentValue;
        }

        this.loadSchemas(nodeUuid);
    }

    private async loadSchemas(nodeUuid: string): Promise<void> {
        this.noSchemaElements = true;

        this.schemas = await this.loader.listNonEmptyProjectSchemas(this.currentProject, nodeUuid);
        this.schemas = this.schemas.sort((a,b) => (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0));
        this.appState.dispatch(new SchemasLoaded(this.schemas));

        this.changeDetector.markForCheck();
    }

    public nodeChanged(nodeUuid: string): void {
        if (nodeUuid === this.currentNodeUuid) {
            return;
        }
        this.currentNodeUuid = nodeUuid;
        this.nodeChange.emit(nodeUuid);
    }

    public elementsLoaded(numberOfElements: number): void {
        if (numberOfElements > 0) {
            this.noSchemaElements = false;
        }
    }

}

