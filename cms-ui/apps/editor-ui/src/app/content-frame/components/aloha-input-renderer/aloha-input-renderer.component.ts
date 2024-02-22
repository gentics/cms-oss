import { ChangeDetectionStrategy, Component, ElementRef, ViewChild } from '@angular/core';
import { AlohaInputComponent } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-input-renderer',
    templateUrl: './aloha-input-renderer.component.html',
    styleUrls: ['./aloha-input-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaInputRendererComponent)],
})
export class AlohaInputRendererComponent extends BaseAlohaRendererComponent<AlohaInputComponent, string> {

    @ViewChild('input')
    public inputRef: ElementRef<HTMLInputElement>;

    public handleInputChange(event: Event): void {
        if (!this.inputRef || !this.inputRef.nativeElement) {
            return;
        }
        const newValue = this.inputRef.nativeElement.value;
        if (newValue !== this.value) {
            this.triggerChange(newValue);
        }
    }

    protected override onValueChange(): void {
        super.onValueChange();

        if (!this.settings) {
            return;
        }

        this.settings.value = this.value;
    }
}
