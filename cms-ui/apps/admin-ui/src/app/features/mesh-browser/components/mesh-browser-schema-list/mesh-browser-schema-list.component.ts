import { AppStateService, SchemasLoaded } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BranchReference, Schema } from '@gentics/mesh-models';
import { ChangesOf } from '@gentics/ui-core';
import { SchemaContainer } from '../../models/mesh-browser-models';
import { MeshBrowserLoaderService } from '../../providers';

@Component({
    selector: 'gtx-mesh-browser-schema-list',
    templateUrl: './mesh-browser-schema-list.component.html',
    styleUrls: ['./mesh-browser-schema-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserSchemaListComponent implements OnChanges {

    @Input()
    public project: string;

    @Input()
    public availableLanguages: Array<string> = [];

    @Input()
    public language: string;

    @Input()
    public branch: BranchReference;

    @Input()
    public node: string;

    @Output()
    public nodeChange = new EventEmitter<string>();

    public schemas: Array<SchemaContainer> = [];
    public isLoading = false;
    public allSchemasLoaded = false;
    public noSchemaElements = true;

    protected loadingSchemas: string[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
        protected route: ActivatedRoute,
        protected appState: AppStateService,
    ) { }

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.node) {
            this.loadSchemas();
        }
    }

    public identify(_index: number, item: Schema): string {
        return item.name;
    }

    private async loadSchemas(): Promise<void> {
        this.isLoading = true;
        this.allSchemasLoaded = false;
        this.loadingSchemas = this.schemas.map(schema => schema.name);
        this.noSchemaElements = true;
        this.changeDetector.markForCheck();

        if (!this.node) {
            this.node = await this.loader.getRootNodeUuid(this.project);
        }

        this.schemas = await this.loader.listNonEmptyProjectSchemas(this.project, this.node);
        this.schemas.sort((a,b) => (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0));
        await this.appState.dispatch(new SchemasLoaded(this.schemas)).toPromise();

        this.isLoading = false;
        this.changeDetector.markForCheck();
    }

    public nodeChangeHandler(nodeUuid: string): void {
        if (nodeUuid === this.node) {
            return;
        }
        this.node = nodeUuid;
        this.nodeChange.emit(nodeUuid);
    }

    public elementsLoaded(schemaName: string, numberOfElements: number): void {
        // Remove this loaded schema from the list
        this.loadingSchemas = this.loadingSchemas.filter(name => name !== schemaName);
        this.allSchemasLoaded = this.loadingSchemas.length === 0;

        if (numberOfElements > 0) {
            this.noSchemaElements = false;
        }

        this.changeDetector.markForCheck();
    }
}
