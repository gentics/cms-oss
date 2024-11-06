import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { NodeVersionInfo, Version, VersionCompatibility, Node } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
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

    @Input()
    public nodes: Node[] = [];

    public compatibilityState: VersionCompatibility = VersionCompatibility.UNKNOWN;
    public nodeVersions: { nodeName: string; nodeInfo: NodeVersionInfo; }[] = [];

    constructor(
        protected modals: ModalService,
    ) { }

    ngOnChanges(changes: SimpleChanges): void {
        this.updateInternalState();
    }

    openModal(): void {
        this.modals.fromComponent(VersionModalComponent, {
            closeOnEscape: true,
            closeOnOverlayClick: true,
        }, {
            cmpVersion: this.versionData.cmpVersion,
            cmsVersion: this.versionData.version,
            nodeVersions: this.nodeVersions,
            compatibilityState: this.compatibilityState,
        })
            .then(instance => instance.open())
            .catch(() => {
                // Ignore all errors, we don't care as there's nothing the modal actual does
                // other than displaying the info.
            });
    }

    private updateInternalState(): void {
        let state: VersionCompatibility = VersionCompatibility.SUPPORTED;

        this.nodeVersions  = (this.nodes || [])
            .slice()
            .sort((a, b) => (a.name || '').localeCompare(b.name || ''))
            .map(node => {
                const info = this.versionData?.nodeInfo[node.name];

                // If it's a Node which is not part of CMP (i.E. has no Mesh CR), then we flag it as unknown and proceed
                if (!info) {
                    return {
                        nodeName: node.name,
                        nodeInfo: {
                            compatibility: VersionCompatibility.UNKNOWN,
                        },
                    };
                }

                if (info.compatibility === VersionCompatibility.UNKNOWN) {
                    state = VersionCompatibility.UNKNOWN;
                } else if (info.compatibility === VersionCompatibility.NOT_SUPPORTED) {
                    state = VersionCompatibility.NOT_SUPPORTED;
                }

                return {
                    nodeName: node.name,
                    nodeInfo: info,
                };
            });

        this.compatibilityState = state;
    }
}
