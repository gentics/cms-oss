import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { RESIZE_MODE } from '../models/mesh-browser-models';


const MODE = RESIZE_MODE.PROP;
const WIDTH = 1200;
const HEIGHT = 500;


@Injectable()
export class MeshBrowserImageService {

    private sid: number;


    constructor(
        protected meshClient: MeshRestClientService,
        protected appState: AppStateService,
    ) {
        this.sid = this.appState.now.auth.sid
    }

    public getOrCreateImagePathForBinaryField(project: string, nodeUuid: string, branchUuid: string, language: string, binaryFieldName: string): string {
        const basePath = `${this.getMeshUrl()}/${project}/nodes/${nodeUuid}/binary/${binaryFieldName}`;
        const queryParams = `sid=${this.sid}&lang=${language}&branch=${branchUuid}`
        const imageQueryParams = this.createAdditionalImageParams();

        return `${basePath}?${queryParams}&${imageQueryParams}`;
    }

    private getMeshUrl(): string {
        return this.meshClient.getConfig().connection.basePath;
    }

    private createAdditionalImageParams(): string {
        const additionalImageParams = `w=${WIDTH}&h=${HEIGHT}&crop=${MODE}`;

        return additionalImageParams;
    }

}
