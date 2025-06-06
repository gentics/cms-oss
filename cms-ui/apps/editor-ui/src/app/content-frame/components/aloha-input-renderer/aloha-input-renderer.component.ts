import { ChangeDetectionStrategy, Component, ElementRef, ViewChild } from '@angular/core';
import { AlohaInputComponent } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { patchMultipleAlohaFunctions } from '../../utils';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

let componentId = 0;

@Component({
    selector: 'gtx-aloha-input-renderer',
    templateUrl: './aloha-input-renderer.component.html',
    styleUrls: ['./aloha-input-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaInputRendererComponent)],
    standalone: false
})
export class AlohaInputRendererComponent extends BaseAlohaRendererComponent<AlohaInputComponent, string> {

    @ViewChild('input')
    public inputRef: ElementRef<HTMLInputElement>;

    public uid = `aloha-input-${componentId++}`;

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        patchMultipleAlohaFunctions(this.settings, {
            setLabel: (label) => {
                this.settings.label = label;
                this.changeDetector.markForCheck();
            },
            setHint: (hint) => {
                this.settings.hint = hint;
                this.changeDetector.markForCheck();
            },
        });
    }

    public handleInputChange(force = false): void {
        if (!force && (!this.inputRef || !this.inputRef.nativeElement)) {
            return;
        }
        const newValue = this.inputRef.nativeElement.value;
        if (force || (newValue !== this.value)) {
            this.triggerChange(newValue);
        }
    }

    public handleKeyDown(event: KeyboardEvent): void {
        if (event.key === 'Enter') {
            // Force an update to the value before confirming it
            this.handleInputChange(true);
            this.manualConfirm.emit();
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
