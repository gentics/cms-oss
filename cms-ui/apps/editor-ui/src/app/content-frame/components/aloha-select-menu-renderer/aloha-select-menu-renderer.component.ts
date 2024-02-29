import { ChangeDetectionStrategy, Component, ElementRef, EventEmitter, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { AlohaSelectMenuComponent, MultiStepSelectMenuOption, SelectMenuOption, SelectMenuSelectEvent } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { Subscription, combineLatest } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';
import { applyControl } from '../../utils';

@Component({
    selector: 'gtx-aloha-select-menu-renderer',
    templateUrl: './aloha-select-menu-renderer.component.html',
    styleUrls: ['./aloha-select-menu-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaSelectMenuRendererComponent)],
})
export class AlohaSelectMenuRendererComponent
    extends BaseAlohaRendererComponent<AlohaSelectMenuComponent, SelectMenuSelectEvent<any>>
    implements OnInit, OnDestroy {

    @Output()
    public multiStepActivation = new EventEmitter<string>();

    @ViewChild('nextStep')
    public nextStep: ElementRef<HTMLDivElement>;

    public activeMultiStep: MultiStepSelectMenuOption<any> | null = null;
    public componentRequiresConfirm = false;
    public control: FormControl<any>;
    public hasLeftIcon = false;
    public hasRightIcon = false;
    public nextStepReady = false;

    protected ctlSub: Subscription[] = [];

    public ngOnInit(): void {
        super.ngOnInit();

        this.checkIcons();
    }

    public ngOnDestroy(): void {
        super.ngOnDestroy();

        this.clearControlSubscriptions();
    }

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        if (!this.settings) {
            return;
        }

        this.settings.setIconsOnly = (iconsOnly) => {
            this.settings.iconsOnly = iconsOnly;
            this.changeDetector.markForCheck();
        };
        this.settings.setOptions = (options) => {
            this.settings.options = options;
            this.checkIcons();
            this.changeDetector.markForCheck();
        };
    }

    protected checkIcons(): void {
        const arr = (this.settings?.options || []) as SelectMenuOption[];
        this.hasLeftIcon = false;
        this.hasRightIcon = false;

        for (const opt of arr) {
            if (opt.icon) {
                this.hasLeftIcon = true;
            }
            if (opt.isMultiStep) {
                this.hasRightIcon = true;
            }
            if (this.hasLeftIcon && this.hasRightIcon) {
                break;
            }
        }
    }

    public handleOptionClick(option: SelectMenuOption): void {
        if (!option.isMultiStep) {
            this.nextStepReady = false;
            this.element.nativeElement.style.height = '';
            this.element.nativeElement.style.width = '';

            if (this.activeMultiStep) {
                this.clearControlSubscriptions();
                this.activeMultiStep = null;
                this.control = null;
            }

            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            this.settings.onSelect?.({ id: option.id });
            this.triggerChange({ id: option.id });
            return;
        }

        this.clearControlSubscriptions();
        this.componentRequiresConfirm = option.multiStepContext.requiresConfirm;
        this.control = new FormControl(option.multiStepContext.initialValue);
        const sub = applyControl(this.control, option.multiStepContext);
        if (sub) {
            this.ctlSub.push(sub);
        }

        this.ctlSub.push(combineLatest([
            this.control.valueChanges,
            this.control.statusChanges,
        ]).pipe(
            filter(([_, status]) => status === 'VALID'),
            map(([value]) => value),
        ).subscribe(() => {
            if (this.componentRequiresConfirm) {
                return;
            }
            this.triggerMultiStepValueChange();
        }));
        this.activeMultiStep = option;

        setTimeout(() => {
            // const rect = this.nextStep.nativeElement.getBoundingClientRect();
            // this.element.nativeElement.style.height = `${rect.height}px`;
            // this.element.nativeElement.style.width = `${rect.width}px`;

            this.nextStepReady = true;
            this.changeDetector.markForCheck();
        });

        this.multiStepActivation.emit(this.activeMultiStep.id);
    }

    public stepBack(): void {
        this.activeMultiStep = null;
        this.multiStepActivation.emit(null);
    }

    protected triggerMultiStepValueChange(): void {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings?.onSelect?.({ id: this.activeMultiStep.id, value: this.control.value });
        this.triggerChange({ id: this.activeMultiStep.id, value: this.control.value });
    }

    public multiStepConfirm(): void {
        this.triggerMultiStepValueChange();
        this.manualConfirm.emit();
    }

    public handleComponentRequire(confirm: boolean): void {
        this.componentRequiresConfirm = confirm;
    }

    public handleComponentManualConfirm(): void {
        this.multiStepConfirm();
    }

    protected clearControlSubscriptions(): void {
        this.ctlSub.forEach(s => s.unsubscribe());
        this.ctlSub = [];
    }
}
