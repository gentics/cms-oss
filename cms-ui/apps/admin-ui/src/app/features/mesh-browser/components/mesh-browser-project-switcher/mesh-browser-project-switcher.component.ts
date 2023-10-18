import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { MeshBrowserLoaderService } from '../../providers';


@Component({
    selector: 'gtx-mesh-browser-project-switcher',
    templateUrl: './mesh-browser-project-switcher.component.html',
    styleUrls: ['./mesh-browser-project-switcher.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserProjectSwitcherComponent implements OnInit {

    @Input()
    public projects: Array<string> = [];

    @Input()
    public branches: Array<string> = [];

    @Input()
    public currentProject: string;

    @Input()
    public currentBranch: string;

    @Output()
    public projectChangedEvent = new EventEmitter<string>();


    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
    ) {

    }

    ngOnInit(): void { }


    public projectChanged(project: string): void {
        console.log('project has changed', project);
        this.projectChangedEvent.emit(project);
    }

}

