import { Component, Input } from '@angular/core';
import { GtxVersionCompatibility, GtxVersionNodeInfo } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-versions-modal',
    templateUrl: './versions-modal.component.html',
    styleUrls: ['./versions-modal.component.scss'],
})
export class VersionModalComponent extends BaseModal<void> {

    public readonly GtxVersionCompatibility = GtxVersionCompatibility;

    @Input()
    cmpVersion: string;

    @Input()
    cmsVersion: string;

    @Input()
    nodeVersions: { nodeName: string; nodeInfo: GtxVersionNodeInfo; }[] = [];

    @Input()
    compatibilityState: GtxVersionCompatibility;
}
