import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnDestroy } from '@angular/core';
import { AlohaAttributeButtonComponent, OverlayElementControl } from '@gentics/aloha-models';
import { wasClosedByUser } from '@gentics/cms-integration-api-models';
import { generateFormProvider } from '@gentics/ui-core';
import { AlohaIntegrationService, DynamicOverlayService } from '../../providers';
import { patchMultipleAlohaFunctions } from '../../utils';
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
    implements OnDestroy {

    protected inputDropdown: OverlayElementControl<string>;

    constructor(
        changeDetector: ChangeDetectorRef,
        element: ElementRef<HTMLElement>,
        aloha: AlohaIntegrationService,
        protected overlay: DynamicOverlayService,
    ) {
        super(changeDetector, element, aloha);
    }

    public override ngOnDestroy(): void {
        super.ngOnDestroy();

        if (this.inputDropdown != null) {
            this.inputDropdown.close();
        }
    }

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        patchMultipleAlohaFunctions(this.settings, {
            setIcon: icon => {
                this.settings.icon = icon;
                this.changeDetector.markForCheck();
            },
            setIconOnly: only => {
                this.settings.iconOnly = only;
                this.changeDetector.markForCheck();
            },
            setIconHollow: hollow => {
                this.settings.iconHollow = hollow;
                this.changeDetector.markForCheck();
            },
            setText: text => {
                this.settings.text = text;
                this.changeDetector.markForCheck();
            },
            setTooltip: tooltip => {
                this.settings.tooltip = tooltip;
                this.changeDetector.markForCheck();
            },
            setTargetElement: element => {
                if (element == null || (element ).length === 0) {
                    this.settings.targetElement = null;
                } else {
                    this.settings.targetElement = element;
                }
                this.changeDetector.markForCheck();
            },
            activateInput: active => {
                this.settings.inputActive = active;
                this.changeDetector.markForCheck();
            },
        })
    }

    public handleClick(): void {
        if (!this.settings) {
            this.aloha.restoreSelection();
            return;
        }
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.click?.();
        this.aloha.restoreSelection(true);
    }

    public handleSecondaryClick(): void {
        if (!this.settings) {
            this.aloha.restoreSelection();
            return;
        }

        if (this.inputDropdown != null || this.settings.targetElement == null) {
            this.aloha.restoreSelection();
            return;
        }

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        const initialValue: string = (this.settings.targetElement as JQuery).attr(this.settings.targetAttribute) as any || '';

        this.overlay.openDynamicDropdown({
            type: 'input',
            initialValue: initialValue,
            options: {
                label: this.settings?.inputLabel,
            },
        }, this.slot)
            .then(ctl => {
                this.inputDropdown = ctl;
                return ctl.value;
            })
            .then(value => {
                this.inputDropdown = null;
                this.triggerChange(value);
            })
            .catch(err => {
                this.inputDropdown = null;
                if (wasClosedByUser(err)) {
                    return;
                }
                console.error(err);
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
