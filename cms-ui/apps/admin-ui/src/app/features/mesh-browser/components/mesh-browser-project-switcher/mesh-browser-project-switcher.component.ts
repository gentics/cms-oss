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
    public projects: Array<string> = [];

    @Input()
    public branches: Array<string> = [];

    @Input()
    public currentProject: string;

    @Input()
    public currentBranch: BranchReference;

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

