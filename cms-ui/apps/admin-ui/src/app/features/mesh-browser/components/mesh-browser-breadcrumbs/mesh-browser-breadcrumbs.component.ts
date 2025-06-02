import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BreadcrumbNode } from '../../models/mesh-browser-models';

@Component({
    selector: 'gtx-mesh-browser-breadcrumbs',
    templateUrl: './mesh-browser-breadcrumbs.component.html',
    styleUrls: ['./mesh-browser-breadcrumbs.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MeshBrowserBreadcrumbComponent {

    @Input()
    public entries: BreadcrumbNode[] = [];

    @Input()
    public language: string;

}
