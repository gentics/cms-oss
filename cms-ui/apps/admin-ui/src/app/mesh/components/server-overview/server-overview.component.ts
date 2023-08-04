import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import {
    ClusterConfigResponse,
    CoordinatorConfig,
    CoordinatorMasterResponse,
    CoordinatorMode,
    LocalConfigModel,
    PluginResponse,
    ServerInfoModel,
    StatusResponse,
} from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';

@Component({
    selector: 'gtx-mesh-server-overview',
    templateUrl: './server-overview.component.html',
    styleUrls: ['./server-overview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ServerOverviewComponent implements OnInit {

    public readonly CoordinatorMode = CoordinatorMode;

    public loading = false;

    public serverInfo: ServerInfoModel;
    public serverConfig: LocalConfigModel;
    public serverStatus: StatusResponse;

    public coordConfig: CoordinatorConfig;
    public coordMaster: CoordinatorMasterResponse;

    public clusterConfig: ClusterConfigResponse;

    public plugins: PluginResponse[];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected api: MeshRestClientService,
    ) {}

    ngOnInit(): void {
        this.loadContent();
    }

    public loadContent(): void {
        this.loading = true;

        Promise.all([
            this.api.server.info().then(info => {
                this.serverInfo = info;
                this.changeDetector.markForCheck();
            }).catch(() => {}),

            this.api.server.config().then(config => {
                this.serverConfig = config;
                this.changeDetector.markForCheck();
            }).catch(() => {}),

            this.api.server.status().then(status => {
                this.serverStatus = status;
                this.changeDetector.markForCheck();
            }).catch(() => {}),

            this.api.coordinator.config().then(config => {
                this.coordConfig = config;
                this.changeDetector.markForCheck();
            }).catch(() => {}),

            this.api.coordinator.master().then(master => {
                this.coordMaster = master;
                this.changeDetector.markForCheck();
            }).catch(() => {}),

            this.api.cluster.config().then(config => {
                this.clusterConfig = config;
                this.changeDetector.markForCheck();
            }).catch(() => {}),

            this.api.plugins.list().then(plugins => {
                this.plugins = plugins.data;
                this.changeDetector.markForCheck();
            }).catch(() => {}),
        ]).then(() => {
            this.loading = false;
            this.changeDetector.markForCheck();
        });
    }
}
