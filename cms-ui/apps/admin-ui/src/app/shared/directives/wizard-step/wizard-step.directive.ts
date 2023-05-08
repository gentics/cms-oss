import { coerceToBoolean, WizardStepNextClickFn } from '@admin-ui/common';
import {
    ContentChild,
    Directive,
    EventEmitter,
    Input,
    Output,
    TemplateRef,
} from '@angular/core';

/**
 * Defines a single step of a wizard.
 *
 * Each step can have an input parameter of type `I` and and output
 * of type `O`.
 * The input is delivered through the `activate` event and the output
 * needs to be returned by the arrow function assigned to the
 * `nextClick` `@Input`.
 *
 * The content of the step needs to be defined using an `<ng-template>`
 *
 * @example
 * ```
 * <gtx-wizard-step
 *      [title]="'example.step_one' | i18n"
 *      [nextEnabled]="myFrom.valid"
 *      (activate)="onStepOneActivate($event)"
 *      [nextClick]="onStepOneNextClick"
 *  >
 *      <ng-template>
 *          <!-- Content of this step -->
 *      </ng-template>
 *  </gtx-wizard-step>
 * ```
 */
@Directive({
    // tslint:disable-next-line: directive-selector
    selector: 'gtx-wizard-step',
})
export class WizardStepDirective<I, O> {

    /**
     * Determines if the "Next" button is enabled.
     * If this is the last wizard step, this controls the "Finish" button.
     */
    @Input()
    get nextEnabled(): boolean {
        return this._nextEnabled;
    }
    set nextEnabled(value: boolean) {
        this._nextEnabled = coerceToBoolean(value);
    }

    /**
     * Emits when this wizard step is activated after the "Next" button of
     * the previous step has been clicked and the promise of the `WizardStepNextClickFn`
     * has resolved. It is not called when clicking the "Previous" button.
     *
     * The argument of the event is the value that the promise of
     * the previous step's `WizardStepNextClickFn` has resolved to, or
     * `undefined` if this is the first step.
     */
    @Output()
    activate = new EventEmitter<I>();

    /**
     * @optional Called when the "Next" or "Finish" button is clicked.
     *
     * If not set, the wizard will advance to the next step immediately
     * and the output of this step will be `void`.
     */
    @Input()
    nextClick: WizardStepNextClickFn<O>;

    /** The human-readable title of this wizard step. */
    @Input()
    title: string;

    /** The content of the wizard step. */
    @ContentChild(TemplateRef)
    contentTemplate: TemplateRef<any>;

    private _nextEnabled: boolean;

}
