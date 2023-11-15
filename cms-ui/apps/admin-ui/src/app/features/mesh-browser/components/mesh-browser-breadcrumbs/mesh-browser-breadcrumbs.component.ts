import { Component, Input, OnInit } from '@angular/core';
import { MeshBrowserNavigatorService } from '../../providers';
import { NavigationEntry } from '../../models/mesh-browser-models';


@Component({
    selector: 'gtx-mesh-browser-breadcrumbs',
    templateUrl: './mesh-browser-breadcrumbs.component.html',
    styleUrls: ['./mesh-browser-breadcrumbs.component.scss'],
})
export class MeshBrowserBreadcrumbComponent implements OnInit {

    @Input()
    public currentProject: string;

    @Input()
    public currentBranchUuid: string;

    @Input()
    public currentNodeUuid: string;

    @Input()
    public currentLanguage: string;

    public breadcrumbs: Array<NavigationEntry> = [];


    constructor(
        protected navigationService: MeshBrowserNavigatorService,
    ) { }

    async ngOnInit(): Promise<void> {
        const breadcrumbs =
            await this.navigationService.getBreadcrumbNavigation(
                this.currentProject,
                { nodeUuid: this.currentNodeUuid },
                this.currentBranchUuid,
            );
        this.breadcrumbs = breadcrumbs;
    }

}
