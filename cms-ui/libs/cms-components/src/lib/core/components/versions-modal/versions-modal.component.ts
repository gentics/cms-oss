import { Component, Input } from '@angular/core';
import { VersionCompatibility, NodeVersionInfo } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-versions-modal',
    templateUrl: './versions-modal.component.html',
    styleUrls: ['./versions-modal.component.scss'],
})
export class VersionModalComponent extends BaseModal<void> {

    public readonly VersionCompatibility = VersionCompatibility;

    @Input()
    cmpVersion: string;

    @Input()
    cmsVersion: string;

    @Input()
    nodeVersions: { nodeName: string; nodeInfo: NodeVersionInfo; }[] = [];

    @Input()
    compatibilityState: VersionCompatibility;
}
