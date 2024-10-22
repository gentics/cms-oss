import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { BranchReference } from '@gentics/mesh-models';
import { MeshBrowserLoaderService } from '../../providers';

@Component({
    selector: 'gtx-mesh-browser-project-switcher',
    templateUrl: './mesh-browser-project-switcher.component.html',
    styleUrls: ['./mesh-browser-project-switcher.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserProjectSwitcherComponent {

    @Input()
    public availableProjects: Array<string> = [];

    @Input()
    public availableBranches: Array<string> = [];

    @Input()
    public project: string;

    @Input()
    public branch: BranchReference;

    @Output()
    public projectChange = new EventEmitter<string>();

    @Output()
    public branchChange = new EventEmitter<BranchReference>();

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
    ) { }

    public projectChangeHandler(project: string): void {
        this.projectChange.emit(project);
    }

    public branchChangeHandler(branch: BranchReference): void {
        this.branchChange.emit(branch);
    }

}

