import { ChangeDetectionStrategy, Component, Inject, Input, OnChanges, Optional, SimpleChanges } from '@angular/core';
import { Version, VersionCompatibility, NodeVersionInfo } from '@gentics/cms-models';
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
    versionData: Version;

    /** Displays aggregated state of software components compatibility */
    compatibilityState: VersionCompatibility;

    cmpVersion: string;
    cmsVersion: string;
    nodeVersions: { nodeName: string; nodeInfo: NodeVersionInfo; }[] = [];

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

    private setData(versionData: Version): void {
        if (!versionData || !versionData.nodeInfo) {
            return;
        }
        this.cmpVersion = versionData.cmpVersion;
        this.cmsVersion = versionData.version;

        this.nodeVersions = [];
        const entries: { [key: string]: NodeVersionInfo; } = versionData.nodeInfo;
        let state: VersionCompatibility = VersionCompatibility.SUPPORTED;
        for (const key in entries) {
            if (Object.prototype.hasOwnProperty.call(entries, key)) {
                const element: NodeVersionInfo = entries[key];
                this.nodeVersions.push({ nodeName: key, nodeInfo: element });

                if (element.compatibility === VersionCompatibility.UNKNOWN) {
                    state = VersionCompatibility.UNKNOWN;
                } else if (element.compatibility === VersionCompatibility.NOT_SUPPORTED) {
                    state = VersionCompatibility.NOT_SUPPORTED;
                }
            }
        }
        this.compatibilityState = state;
    }
}
