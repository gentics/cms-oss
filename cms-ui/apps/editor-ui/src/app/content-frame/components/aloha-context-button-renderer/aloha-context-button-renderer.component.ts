import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnDestroy } from '@angular/core';
import { AlohaContextButtonComponent, OverlayElementControl } from '@gentics/aloha-models';
import { ModalCloseError, ModalClosingReason } from '@gentics/cms-integration-api-models';
import { generateFormProvider } from '@gentics/ui-core';
import { AlohaIntegrationService, DynamicOverlayService } from '../../providers';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

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

        if (!this.settings) {
            return;
        }

        this.settings.setIcon = icon => {
            this.settings.icon = icon;
            this.changeDetector.markForCheck();
        };
        this.settings.setIconOnly = only => {
            this.settings.iconOnly = only;
            this.changeDetector.markForCheck();
        }
        this.settings.setIconHollow = hollow => {
            this.settings.iconHollow = hollow;
            this.changeDetector.markForCheck();
        };
        this.settings.setText = text => {
            this.settings.text = text;
            this.changeDetector.markForCheck();
        };
        this.settings.setTooltip = tooltip => {
            this.settings.tooltip = tooltip;
            this.changeDetector.markForCheck();
        };

        this.settings.isOpen = () => {
            return this.openControl != null && this.openControl.isOpen();
        };
        this.settings.closeContext = () => {
            this.closeAndClearContext();
            this.aloha.restoreSelection();
        };
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
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        const context = typeof this.settings.context === 'function' ? this.settings.context() : this.settings.context;

        if (context == null) {
            this.aloha.restoreSelection();
            return;
        }

        let ctl: Promise<OverlayElementControl<T>>;

        if (this.settings.contextType === 'dropdown') {
            ctl = this.overlay.openDynamicDropdown(context, this.slot);
        } else if (this.settings.contextType === 'modal') {
            ctl = this.overlay.openDynamicModal(context);
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
