import { ContentRepositoryBO } from '@admin-ui/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChangesOf } from '@gentics/ui-core';
import { MeshBrowserLoaderService } from '../../providers';
import { LoadingState } from '../mesh-browser-schema-items/mesh-browser-schema-items.component';

/**
 * A single renderable schema, which has the info from the latest inputs.
 * These are deliberately not passed through to the schema-items component,
 * as this would cause them to update/load info before we want to.
 */
interface SchemaRenderInfo {
    schemaName: string;

    contentRepository: ContentRepositoryBO;
    project: string;
    availableLanguages: string[];
    branch: string;
    node: string;
    language: string;
}

@Component({
    selector: 'gtx-mesh-browser-schema-list',
    templateUrl: './mesh-browser-schema-list.component.html',
    styleUrls: ['./mesh-browser-schema-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserSchemaListComponent implements OnChanges {

    @Input()
    public contentRepository: ContentRepositoryBO;

    @Input()
    public project: string;

    @Input()
    public projectSchemas: string[] = [];

    @Input()
    public availableLanguages: Array<string> = [];

    @Input()
    public branch: string;

    @Input({ required: true })
    public node: string;

    @Input()
    public language: string;

    @Output()
    public nodeChange = new EventEmitter<string>();

    // Loading info
    public allSchemasLoaded = false;
    public noSchemaElements = true;

    /** If it's loading the `schemasToRender` */
    public isLoadingSchemas = false;
    /** A subset of `projectSchemas` which actually have nodes within this current node. */
    public schemasToRender: SchemaRenderInfo[] = [];

    /** Names of the schemas which are currently still loading. */
    protected loadingLists = new Set<string>();

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
        protected route: ActivatedRoute,
    ) { }

    ngOnChanges(changes: ChangesOf<this>): void {
        if (
            changes.contentRepository
            || changes.project
            || changes.projectSchemas
            || changes.availableLanguages
            || changes.branch
            || changes.node
        ) {
            this.reloadSchemasToRender();
        }
    }

    private async reloadSchemasToRender(): Promise<void> {
        this.allSchemasLoaded = false;
        this.noSchemaElements = true;
        this.isLoadingSchemas = true;
        this.changeDetector.markForCheck();

        const loadData: Partial<SchemaRenderInfo> = {
            contentRepository: this.contentRepository,
            project: this.project,
            branch: this.branch,
            availableLanguages: this.availableLanguages,
            node: this.node,
            language: this.language,
        };

        const schemaNamesToRender = await this.loader.getSchemaNamesWithNodes(
            this.project,
            this.branch,
            this.node,
            this.availableLanguages,
            this.projectSchemas,
        );
        this.schemasToRender = schemaNamesToRender.map(name => {
            const clone = structuredClone(loadData);
            clone.schemaName = name;
            return clone as SchemaRenderInfo;
        });
        this.loadingLists = new Set(schemaNamesToRender);
        this.isLoadingSchemas = false;

        this.changeDetector.markForCheck();
    }

    public identify(_index: number, renderInfo: SchemaRenderInfo): string {
        return renderInfo.schemaName;
    }

    public nodeChangeHandler(nodeUuid: string): void {
        if (nodeUuid === this.node) {
            return;
        }
        this.nodeChange.emit(nodeUuid);
    }

    public elementsLoaded(schemaName: string, loadingState: LoadingState): void {
        // Remove this loaded schema from the list
        this.loadingLists.delete(schemaName);
        // Determine loading state
        this.allSchemasLoaded = this.loadingLists.size === 0;

        if (loadingState.hasElements) {
            this.noSchemaElements = false;
        }

        this.changeDetector.markForCheck();
    }
}
