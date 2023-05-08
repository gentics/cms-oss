import { ChangeDetectionStrategy, Component, HostListener } from '@angular/core';
import { IModalDialog } from '@gentics/ui-core';

export interface GtxExternalAssetManagementApiResponse {
    data: GtxExternalAssetManagementApiRootObject[];
}

export interface GtxExternalAssetManagementApiRootObject {
    name: string;
    fileCategory: string;
    '@odata.mediaReadLink': string;
    description?: string;
    niceUrl?: string;
    alternateUrls?: string[];
    properties?: { [key: string]: any };
}

@Component({
    selector: 'external-assets-modal',
    templateUrl: './external-assets-modal.component.html',
    styleUrls: ['./external-assets-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExternalAssetsModalComponent implements IModalDialog {

    /** Modal title */
    title: string;

    /** URI of iframe src attribute */
    iframeSrcUrl: string;

    /** Listen to child iframe data propagation */
    @HostListener('window:message', ['$event']) onPostMessage(event: { data: string }): void {
        try {
            const response: GtxExternalAssetManagementApiRootObject[] = JSON.parse(event.data);
            response.forEach(this.validate);
            this.closeFn({ data: response });
        } catch (error) {
            console.error(error);
            this.cancelFn(error);
            throw new Error(`Invalid response of asset_management from ${this.iframeSrcUrl}`);
        }
    }

    closeFn = (apiResponse: GtxExternalAssetManagementApiResponse) => {};
    cancelFn = (val?: any) => undefined;

    registerCloseFn(close: (response: GtxExternalAssetManagementApiResponse) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }

    cancelButtonClicked(): void {
        // undefine return value to indicate user action
        this.closeFn(undefined);
    }

    private validate(data: GtxExternalAssetManagementApiRootObject): void {
        const checkProp = (prop: keyof GtxExternalAssetManagementApiRootObject): void => {
            if (!data[prop]) {
                throw new Error(`Invalid response of asset_management from ${this.iframeSrcUrl}: Property "${prop}" not provided or invalid!`);
            }
        };
        Object.keys(data).forEach(checkProp);
    }
}
