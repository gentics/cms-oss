import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { Branch, MeshBrowserLoaderService } from '../../providers';


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
    public currentBranch: Branch;

    @Output()
    public projectChangedEvent = new EventEmitter<string>();

    @Output()
    public branchChangedEvent = new EventEmitter<Branch>();


    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
    ) { }

    public projectChanged(project: string): void {
        this.projectChangedEvent.emit(project);
    }

    public branchChanged(branch: Branch): void {
        this.branchChangedEvent.emit(branch);
    }

}

