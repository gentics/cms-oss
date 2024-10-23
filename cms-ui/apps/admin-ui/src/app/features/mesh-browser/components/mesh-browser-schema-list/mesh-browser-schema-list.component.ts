import { ContentRepositoryBO } from '@admin-ui/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BranchReference } from '@gentics/mesh-models';
import { ChangesOf } from '@gentics/ui-core';
import { MeshBrowserLoaderService } from '../../providers';
import { LoadingState } from '../mesh-browser-schema-items/mesh-browser-schema-items.component';

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
    public language: string;

    @Input()
    public branch: BranchReference;

    @Input({ required: true })
    public node: string;

    @Output()
    public nodeChange = new EventEmitter<string>();

    public allSchemasLoaded = false;
    public noSchemaElements = true;
    public hadIntiialLoad = false;

    protected loadingSchemas: string[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
        protected route: ActivatedRoute,
    ) { }

    ngOnChanges(changes: ChangesOf<this>): void {
        this.resetLoadingState();
    }

    private resetLoadingState(): void {
        this.allSchemasLoaded = false;
        this.loadingSchemas = this.projectSchemas.slice();
        this.noSchemaElements = true;
    }

    public nodeChangeHandler(nodeUuid: string): void {
        if (nodeUuid === this.node) {
            return;
        }
        this.nodeChange.emit(nodeUuid);
    }

    public elementsLoaded(schemaName: string, loadingState: LoadingState): void {
        // Remove this loaded schema from the list
        this.loadingSchemas = this.loadingSchemas.filter(name => name !== schemaName);
        // Determine loading state
        this.allSchemasLoaded = this.loadingSchemas.length === 0;
        // If this has now initially loaded items
        if (this.allSchemasLoaded) {
            this.hadIntiialLoad = true;
        }

        if (loadingState.hasElements) {
            this.noSchemaElements = false;
        }

        this.changeDetector.markForCheck();
    }
}
