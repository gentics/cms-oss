import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { AlohaContextButtonComponent } from '@gentics/aloha-models';
import { OverlayElementControl } from '@gentics/cms-integration-api-models';
import { ModalCloseError, ModalClosingReason, generateFormProvider } from '@gentics/ui-core';
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
    implements OnChanges, OnDestroy {

    public hasText = false;
    public hasIcon = false;

    protected openControl: OverlayElementControl<T>;

    constructor(
        changeDetector: ChangeDetectorRef,
        element: ElementRef<HTMLElement>,
        aloha: AlohaIntegrationService,
        protected overlay: DynamicOverlayService,
    ) {
        super(changeDetector, element, aloha);
    }

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        if (!this.settings) {
            return;
        }

        this.settings.isOpen = () => {
            return this.openControl != null && this.openControl.isOpen();
        };
        this.settings.closeContext = () => {
            this.closeAndClearContext();
        };
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);
        this.hasText = !!this.settings?.text || !!this.settings?.html;
        this.hasIcon = !!this.settings?.icon;
    }

    public override ngOnDestroy(): void {
        super.ngOnDestroy();
        this.closeAndClearContext();
    }

    public handleClick(): void {
        if (!this.settings) {
            return;
        }

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        const context = typeof this.settings.context === 'function' ? this.settings.context() : this.settings.context;
        let ctl: Promise<OverlayElementControl<T>>;

        if (this.settings.contextType === 'dropdown') {
            ctl = this.overlay.openDynamicDropdown(context, this.slot);
        } else if (this.settings.contextType === 'modal') {
            ctl = this.overlay.openDynamicModal(context);
        } else {
            return;
        }

        ctl.then(actualCtl => {
            this.openControl = actualCtl;
            return actualCtl.value;
        }).then(value => {
            this.settings.value = value;
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            this.settings?.contextResolve?.(value);
            this.closeAndClearContext();
        }).catch(error => {
            if (error instanceof ModalCloseError && error.reason !== ModalClosingReason.ERROR) {
                // This is a "notification" error which can be safely dismissed.
                this.closeAndClearContext();
                return;
            }

            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            this.settings?.contextReject?.(error);
            this.closeAndClearContext();
        });
    }

    protected closeAndClearContext(): void {
        if (this.openControl == null) {
            return;
        }
        this.openControl.close();
        this.openControl = null;
    }
}
