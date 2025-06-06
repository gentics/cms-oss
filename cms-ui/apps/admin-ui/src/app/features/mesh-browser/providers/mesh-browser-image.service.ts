import { Injectable } from '@angular/core';
import { RequestMethod } from '@gentics/mesh-rest-client';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { ResizeMode } from '../models/mesh-browser-models';

const MODE = ResizeMode.SMART;
const WIDTH = 1200;
const HEIGHT = 500;

@Injectable()
export class MeshBrowserImageService {

    constructor(protected meshClient: MeshRestClientService) {}

    public getImageUrlForBinaryField(
        project: string,
        nodeUuid: string,
        branchUuid: string,
        language: string,
        binaryFieldName: string,
    ): string {
        const basePath = `${project}/nodes/${nodeUuid}/binary/${binaryFieldName}`;
        const request = this.meshClient.prepareRequest(RequestMethod.GET, basePath, {
            lang: language,
            branch: branchUuid,
            // ...this.createAdditionalImageParams(),
        }, {});

        let fullUrl = request.url;

        const params = new URLSearchParams(request.params).toString();
        if (params) {
            fullUrl += `?${params}`;
        }

        return fullUrl;
    }

    private createAdditionalImageParams(): Record<string, string> {
        const additionalImageParams: Record<string, string> = {
            w: `${WIDTH}`,
            h: `${HEIGHT}`,
            crop: MODE,
        };

        return additionalImageParams;
    }
}
