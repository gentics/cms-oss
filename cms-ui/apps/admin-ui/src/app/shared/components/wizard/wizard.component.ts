import { ObservableStopper } from '@admin-ui/common';
import {
    AfterContentInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ContentChildren,
    EventEmitter,
    Input,
    OnDestroy,
    Output,
    QueryList,
} from '@angular/core';
import { takeUntil } from 'rxjs/operators';
import { WizardStepDirective } from '../../directives/wizard-step/wizard-step.directive';

/**
 * Generic component for defining a wizard.
 *
 * Use the `WizardService` for displaying a wizard realized using this component.
 *
 * @example
 * ```
 * <gtx-wizard [title]="'example.title' | i18n">
 *
 *     <gtx-wizard-step
 *         [title]="'example.step_one' | i18n"
 *         [nextEnabled]="myFrom.valid"
 *         (activate)="onStepOneActivate($event)"
 *         [nextClick]="onStepOneNextClick"
 *     >
 *         <ng-template>
 *             <!-- Content of this step -->
 *         </ng-template>
 *     </gtx-wizard-step>
 *
 *     <gtx-wizard-step ...>
 *         ...
 *     </gtx-wizard-step>
 *
 *     ...
 *
 * </gtx-wizard>
 *
 *
 * // To show a wizard:
 * this.wizardService.showWizard(MyWizardComponent)
 *     .then(result => ...);
 * ```
 */
@Component({
    selector: 'gtx-wizard',
    templateUrl: './wizard.component.html',
    styleUrls: ['./wizard.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class WizardComponent implements AfterContentInit, OnDestroy {

    /** The title that will be displayed throughout the wizard. */
    @Input()
    title: string;

    /** The title of the "Finish" button - if not set, it will be "Finish". */
    @Input()
    finishButtonTitle: string;

    /**
     * Emits when the wizard has completed successfully.
     *
     * The argument of this event is the value that the `WizardStepNextClickFn`
     * of the last step resolved to.
     */
    @Output()
    wizardFinish = new EventEmitter<any>();

    /** Emits when the wizard is canceled by the user. */
    @Output()
    wizardCancel = new EventEmitter<void>();

    /** The distinct steps of this wizard. */
    @ContentChildren(WizardStepDirective)
    steps: QueryList<WizardStepDirective<any, any>>;

    /** The index of the current step (used to assign the active tab). */
    currentStepIndex = -1;

    /** The active WizardStep object. */
    currentStep: WizardStepDirective<any, any>;

    /** `true` while a `WizardStepNextClickFn` is executing. */
    nextClickHandlerExecuting = false;

    private wizardSteps: WizardStepDirective<any, any>[];

    private stopper = new ObservableStopper();

    constructor(
        private changeDetector: ChangeDetectorRef,
    ) { }

    ngAfterContentInit(): void {
        this.steps.changes.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((steps: QueryList<WizardStepDirective<any, any>>) => {
            this.wizardSteps = steps.toArray();
            if (!this.currentStep && this.wizardSteps.length > 0) {
                this.currentStep = this.wizardSteps[0];

                // The the active tab index must be set after the gtx-tabs have discovered
                // their content children. That's why we need to use setTimeout() here.
                setTimeout(() => {
                    this.currentStepIndex = 0;
                    this.changeDetector.markForCheck();
                });
            }
        });
        this.steps.notifyOnChanges();
    }

    ngOnDestroy(): void {
        this.stopper.stop();
    }

    onPrevButtonClick(): void {
        if (this.currentStepIndex > 0) {
            this.switchToStep(this.currentStepIndex - 1);
        }
    }

    onNextButtonClick(): void {
        this.nextClickHandlerExecuting = true;
        const nextClickFn = this.currentStep.nextClick;
        const proceed = nextClickFn ? nextClickFn() : Promise.resolve();

        proceed.then(stepOutput => {
            if (this.currentStepIndex < this.wizardSteps.length - 1) {
                this.switchToStep(this.currentStepIndex + 1);
                this.currentStep.activate.emit(stepOutput);
            } else {
                this.wizardFinish.emit(stepOutput);
            }
        }).catch(() => {})
            .finally(() => this.nextClickHandlerExecuting = false);
    }

    onCancelButtonClick(): void {
        this.wizardCancel.emit();
    }

    private switchToStep(stepIndex: number): void {
        this.currentStepIndex = stepIndex;
        const nextStep = this.wizardSteps[stepIndex];
        this.currentStep = nextStep;
        this.changeDetector.markForCheck();
    }

}
