import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { AlohaAttributeButtonComponent, ButtonIcon, OverlayElementControl } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { AlohaIntegrationService, DynamicOverlayService } from '../../providers';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-attribute-button-renderer',
    templateUrl: './aloha-attribute-button-renderer.component.html',
    styleUrls: ['./aloha-attribute-button-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaAttributeButtonRendererComponent)],
})
export class AlohaAttributeButtonRendererComponent
    extends BaseAlohaRendererComponent<AlohaAttributeButtonComponent, string>
    implements OnChanges, OnDestroy {

    public hasText = false;
    public hasIcon = false;
    public iconToRender: string;

    protected inputDropdown: OverlayElementControl<string>;

    constructor(
        changeDetector: ChangeDetectorRef,
        element: ElementRef<HTMLElement>,
        aloha: AlohaIntegrationService,
        protected overlay: DynamicOverlayService,
    ) {
        super(changeDetector, element, aloha);
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        this.hasText = !!this.settings?.text || !!this.settings?.html;

        this.iconToRender = typeof this.settings?.icon === 'string'
            ? this.settings?.icon
            : this.settings?.icon?.primary;
        this.hasIcon = !!this.iconToRender;
    }

    public override ngOnDestroy(): void {
        super.ngOnDestroy();

        if (this.inputDropdown != null) {
            this.inputDropdown.close();
        }
    }

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        if (!this.settings) {
            return;
        }

        this.settings.setIcon = (icon: ButtonIcon) => {
            this.settings.icon = icon;
            this.iconToRender = typeof this.settings?.icon === 'string'
                ? this.settings?.icon
                : this.settings?.icon?.primary;
            this.hasIcon = !!this.iconToRender;
            this.changeDetector.markForCheck();
        };
        this.settings.setText = (text: string) => {
            this.settings.text = text;
            this.hasText = !!this.settings.text || !!this.settings.html;
            this.changeDetector.markForCheck();
        };
        this.settings.setTooltip = (tooltip: string) => {
            this.settings.tooltip = tooltip;
            this.changeDetector.markForCheck();
        };
    }

    public handleClick(): void {
        if (!this.settings) {
            return;
        }
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.click?.();
    }

    public handleSecondaryClick(): void {
        if (!this.settings) {
            return;
        }

        if (this.inputDropdown != null || this.settings.targetElement == null) {
            return;
        }

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        const initialValue: string = (this.settings.targetElement as JQuery).attr(this.settings.targetAttribute) as any || '';

        this.overlay.openDynamicDropdown({
            type: 'input',
            initialValue: initialValue,
        }, this.slot).then(ctl => {
            this.inputDropdown = ctl;
            return ctl.value;
        }).then(value => {
            this.inputDropdown = null;
            this.triggerChange(value);
        }).catch(err => {
            this.inputDropdown = null;
        });
    }

    protected override onValueChange(): void {
        super.onValueChange();

        if (this.settings.targetElement == null || this.settings.targetAttribute == null) {
            return;
        }

        (this.settings.targetElement as JQuery).attr(this.settings.targetAttribute, this.value);
    }
}
