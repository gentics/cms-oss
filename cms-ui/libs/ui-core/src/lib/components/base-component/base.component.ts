import { ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { Subscription } from 'rxjs';
import { coerceInstance, CoerceOption } from '../../utils';

/**
 * Base Component implementation which has common basics implemented which are used
 * in various components.
 */
@Component({ template: '' })
export class BaseComponent implements OnChanges, OnDestroy {

    /**
     * If this control is disabled or not.
     */
    @Input()
    public disabled = false;

    /**
     * An array of boolean inputs which will automatically coerced to proper boolean
     * values on changes, when provided via input bindings.
     *
     * @deprecated Use the {@link Input.transform} option with the {@link transformToBoolean} transformer instead.
     * Will be removed in the next major verison.
     */
    // eslint-disable-next-line @typescript-eslint/naming-convention
    protected readonly booleanInputs: CoerceOption<this>[] = ['disabled'];

    /**
     * Array of subscriptions to clean up at the end of the component life-cycle (ngOnDestroy)
     */
    // eslint-disable-next-line @typescript-eslint/naming-convention
    protected readonly subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
    ) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (!this.booleanInputs) {
            return;
        }

        const changed = coerceInstance(this, this.booleanInputs, changes);
        // Special case when it's the `disabled` input, then we also need to call the disabled change hook
        if (changed.includes('disabled')) {
            this.onDisabledChange();
        }
    }

    ngOnDestroy(): void {
        if (this.subscriptions) {
            this.subscriptions.forEach(subscription => subscription.unsubscribe());
        }
    }

    /** Optional hook which is called whenever the disabled state has been changed. */
    protected onDisabledChange(): void { /* No-op */ }
}
