import { ChangeDetectionStrategy, Component, HostListener } from '@angular/core';
import { ExternalAssetReference } from '@gentics/cms-integration-api-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'external-assets-modal',
    templateUrl: './external-assets-modal.component.html',
    styleUrls: ['./external-assets-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExternalAssetsModalComponent extends BaseModal<ExternalAssetReference[]> {

    /** Modal title */
    title: string;

    /** URI of iframe src attribute */
    iframeSrcUrl: string;

    /** Listen to child iframe data propagation */
    @HostListener('window:message', ['$event'])
    public onPostMessage(event: MessageEvent): void {
        try {
            let response: ExternalAssetReference[] = event.data;
            if (typeof response === 'string') {
                response = JSON.parse(response);
            }
            response.forEach(this.validate);
            this.closeFn(response);
        } catch (error) {
            console.error(error);
            this.cancelFn(error);
            throw new Error(`Invalid response of asset_management from ${this.iframeSrcUrl}`);
        }
    }

    cancelButtonClicked(): void {
        // undefine return value to indicate user action
        this.closeFn(undefined);
    }

    private validate(data: ExternalAssetReference): void {
        const checkProp = (prop: keyof ExternalAssetReference): void => {
            if (!data[prop]) {
                throw new Error(`Invalid response of asset_management from ${this.iframeSrcUrl}: Property "${prop}" not provided or invalid!`);
            }
        };
        Object.keys(data).forEach(checkProp);
    }
}
