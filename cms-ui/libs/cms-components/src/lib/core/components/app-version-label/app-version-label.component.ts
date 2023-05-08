import { ChangeDetectionStrategy, Component, Inject, Input, OnChanges, Optional, SimpleChanges } from '@angular/core';
import { GtxVersion, GtxVersionCompatibility, GtxVersionNodeInfo } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { environment } from '../../../common/utils';
import { VersionModalComponent } from '../versions-modal';

@Component({
    selector: 'gtx-app-version-label',
    templateUrl: './app-version-label.component.html',
    styleUrls: ['./app-version-label.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GtxAppVersionLabelComponent implements OnChanges {
    /** Name of the application */
    @Input()
    appTitle: string;

    /** App stack Version information */
    @Input()
    versionData: GtxVersion;

    /** Displays aggregated state of software components compatibility */
    compatibilityState: GtxVersionCompatibility;

    cmpVersion: string;
    cmsVersion: string;
    nodeVersions: { nodeName: string; nodeInfo: GtxVersionNodeInfo; }[] = [];

    protected modalVisible = false;

    constructor(
        @Inject(environment) @Optional() public environment: string,
        private modals: ModalService,
    ) { }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.versionData.currentValue) {
            this.setData(changes.versionData.currentValue);
        }
    }

    containerClicked(): void {
        if (!this.versionData || this.modalVisible) {
            return;
        }

        this.modals.fromComponent(VersionModalComponent, {
            onClose: () => {
                this.modalVisible = false;
            },
        }, {
            cmpVersion: this.cmpVersion,
            cmsVersion: this.cmsVersion,
            nodeVersions: this.nodeVersions,
            compatibilityState: this.compatibilityState,
        }).then(dialog => dialog.open());
    }

    private setData(versionData: GtxVersion): void {
        if (!versionData || !versionData.nodeInfo) {
            return;
        }
        this.cmpVersion = versionData.cmpVersion;
        this.cmsVersion = versionData.version;

        this.nodeVersions = [];
        const entries: { [key: string]: GtxVersionNodeInfo; } = versionData.nodeInfo;
        let state: GtxVersionCompatibility = GtxVersionCompatibility.SUPPORTED;
        for (const key in entries) {
            if (Object.prototype.hasOwnProperty.call(entries, key)) {
                const element: GtxVersionNodeInfo = entries[key];
                this.nodeVersions.push({ nodeName: key, nodeInfo: element });

                if (element.compatibility === GtxVersionCompatibility.UNKNOWN) {
                    state = GtxVersionCompatibility.UNKNOWN;
                } else if (element.compatibility === GtxVersionCompatibility.NOT_SUPPORTED) {
                    state = GtxVersionCompatibility.NOT_SUPPORTED;
                }
            }
        }
        this.compatibilityState = state;
    }
}
