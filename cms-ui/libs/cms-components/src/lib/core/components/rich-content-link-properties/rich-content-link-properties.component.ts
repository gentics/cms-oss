import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { FormProperties, generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';
import { RichContentLink, RichContentLinkType, RichContentType } from '../../../common/models';
import { BasePropertiesComponent } from '../base-properties/base-properties.component';

@Component({
    selector: 'gtx-rich-content-link-properties',
    templateUrl: './rich-content-link-properties.component.html',
    styleUrls: ['./rich-content-link-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(RichContentLinkPropertiesComponent),
        generateValidatorProvider(RichContentLinkPropertiesComponent),
    ],
})
export class RichContentLinkPropertiesComponent extends BasePropertiesComponent<RichContentLink> {

    protected createForm(): FormGroup {
        return new FormGroup<FormProperties<RichContentLink>>({
            type: new FormControl(RichContentType.LINK),
            linkType: new FormControl(this.value?.linkType ?? RichContentLinkType.URL, Validators.required),
            displayText: new FormControl(this.value?.displayText || ''),
            url: new FormControl(this.value?.url, Validators.required),
            nodeId: new FormControl(this.value?.nodeId, Validators.required),
            itemId: new FormControl(this.value?.itemId, Validators.required),
            langCode: new FormControl(this.value?.langCode),
            target: new FormControl(this.value?.target || '_top'),
        });
    }

    protected configureForm(value: RichContentLink, loud?: boolean): void {
        const options = { emitEvent: loud, onlySelf: loud };
        setControlsEnabled(this.form, ['url'], value?.linkType === RichContentLinkType.URL, options);
        setControlsEnabled(this.form, ['nodeId', 'itemId'], value?.linkType !== RichContentLinkType.URL);
        setControlsEnabled(this.form, ['langCode'], value?.linkType === RichContentLinkType.PAGE);
    }

    protected assembleValue(value: RichContentLink): RichContentLink {
        return value;
    }
}
