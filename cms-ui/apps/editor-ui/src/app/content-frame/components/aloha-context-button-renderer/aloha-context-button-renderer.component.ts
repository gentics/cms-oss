import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnDestroy } from '@angular/core';
import { AlohaContextButtonComponent, DynamicDropdownConfiguration, DynamicFormModalConfiguration, OverlayElementControl } from '@gentics/aloha-models';
import { ModalCloseError, ModalClosingReason } from '@gentics/cms-integration-api-models';
import { generateFormProvider } from '@gentics/ui-core';
import { AlohaIntegrationService, DynamicOverlayService } from '../../providers';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';
import { patchMultipleAlohaFunctions } from '../../utils';

@Component({
    selector: 'gtx-aloha-context-button-renderer',
    templateUrl: './aloha-context-button-renderer.component.html',
    styleUrls: ['./aloha-context-button-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaContextButtonRendererComponent)],
})
export class AlohaContextButtonRendererComponent<T>
    extends BaseAlohaRendererComponent<AlohaContextButtonComponent<T>, void>
    implements OnDestroy {

    protected openControl: OverlayElementControl<T>;

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
        this.closeAndClearContext();
    }

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        patchMultipleAlohaFunctions(this.settings as AlohaContextButtonComponent<any>, {
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
            isOpen: () => {
                return this.openControl?.isOpen?.() ?? false;
            },
            closeContext: () => {
                this.closeAndClearContext();
                this.aloha.restoreSelection();
            },
        });
    }

    public handleClick(): void {
        if (!this.settings) {
            this.aloha.restoreSelection();
            return;
        }

        this.triggerTouch();
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.click?.();

        this.handleContext();
    }

    protected handleContext(): void {
        const context: DynamicDropdownConfiguration<T> | DynamicFormModalConfiguration<T> =
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            typeof this.settings.context === 'function' ? this.settings.context() : this.settings.context;

        if (context == null) {
            this.aloha.restoreSelection();
            return;
        }

        context.openerReference = this.settings?.name;
        let ctl: Promise<OverlayElementControl<T>>;

        if (this.settings.contextType === 'dropdown') {
            ctl = this.overlay.openDynamicDropdown(context as DynamicDropdownConfiguration<T>, this.slot);
        } else if (this.settings.contextType === 'modal') {
            ctl = this.overlay.openDynamicModal(context as DynamicFormModalConfiguration<T>);
        } else {
            this.aloha.restoreSelection();
            return;
        }

        ctl.then(actualCtl => {
            this.openControl = actualCtl;
            return actualCtl.value;
        }).then(value => {
            this.settings.value = value;
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            this.settings?.contextResolve?.(value);
        }).catch(error => {
            if (error instanceof ModalCloseError && error.reason !== ModalClosingReason.ERROR) {
                // This is a "notification" error which can be safely dismissed.
                return;
            }

            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            this.settings?.contextReject?.(error);
        }).finally(() => {
            this.closeAndClearContext();
            this.aloha.restoreSelection();
        })
    }

    protected closeAndClearContext(): void {
        if (this.openControl == null) {
            return;
        }
        this.openControl.close();
        this.openControl = null;
    }
}
