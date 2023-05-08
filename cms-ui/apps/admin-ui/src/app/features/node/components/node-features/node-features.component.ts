import { ChangesOf, FormControlOnChangeFn, ObservableStopper } from '@admin-ui/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    HostListener,
    Input,
    OnChanges,
    OnDestroy
} from '@angular/core';
import { AbstractControl, ControlValueAccessor, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Index, IndexByKey, NodeFeature, NodeFeatureModel } from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
import { isEqual as _isEqual } from 'lodash';
import { Subject } from 'rxjs';
import { map, takeUntil, takeWhile } from 'rxjs/operators';

export type NodeFeaturesFormData = Partial<Index<NodeFeature, boolean>>;

/**
 * Creates a dynamic form based on `availableFeatures` and shows a checkbox for
 * each avaiable node feature.
 */
@Component({
    selector: 'gtx-node-features',
    templateUrl: './node-features.component.html',
    styleUrls: ['./node-features.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(NodeFeaturesComponent)],
})
export class NodeFeaturesComponent implements OnDestroy, OnChanges, ControlValueAccessor {

    fgFeatures: UntypedFormGroup;

    /** An array of all node features that are available. */
    @Input()
    availableFeatures: NodeFeatureModel[];

    @Input()
    public disabled = false;

    /**
     * This subject is used to decouple the `registerOnChange()` subscription
     * from the `valueChanges` observable of the current `fgFeatures` FormGroup.
     *
     * This is needed, because we need to recreate `fgFeatures` whenever the
     * `availableFeatures` change.
     */
    private valueChangesSubj$ = new Subject();

    private currentValue: NodeFeaturesFormData = {};

    private stopper = new ObservableStopper();

    descriptionVisible: NodeFeatureModel = null;
    descriptionHideTimeout: any;
    descriptionShowTimeout: any;
    descriptionPosition: string;

    constructor(
        private changeDetector: ChangeDetectorRef,
    ) {}

    ngOnDestroy(): void {
        this.stopper.stop();
    }

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.availableFeatures && !_isEqual(changes.availableFeatures.previousValue, changes.availableFeatures.currentValue)) {
            this.recreateForm(changes.availableFeatures.currentValue);
        }
        if (changes.disabled) {
            this.setDisabledState(this.disabled);
        }
    }

    writeValue(value: NodeFeaturesFormData): void {
        if (value) {
            this.currentValue = value;
            this.fgFeatures.patchValue(value);
        } else {
            this.currentValue = {};
            this.fgFeatures.reset();
        }
        this.fgFeatures.markAsPristine();
    }

    registerOnChange(fn: FormControlOnChangeFn<NodeFeaturesFormData>): void {
        this.valueChangesSubj$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(fn);
    }

    registerOnTouched(fn: any): void { }

    setDisabledState(isDisabled: boolean): void {
        if (isDisabled) {
            this.fgFeatures.disable({ emitEvent: false });
        } else {
            this.fgFeatures.enable({ emitEvent: false });
        }
    }

    private recreateForm(availableFeatures: NodeFeatureModel[]): void {
        const controls: IndexByKey<AbstractControl> = {};

        if (Array.isArray(availableFeatures)) {
            availableFeatures.forEach(feature => {
                controls[feature.id] = new UntypedFormControl(this.currentValue[feature.id]);
            });
        }

        const formGroup = new UntypedFormGroup(controls);
        this.fgFeatures = formGroup;

        // Publish the changes from the current formGroup using valueChangeSubj$ until the
        // fgFeatures FormGroup changes again.
        formGroup.valueChanges.pipe(
            takeWhile(() => this.fgFeatures === formGroup),
            map((formData: NodeFeaturesFormData) => formGroup.valid ? formData : null),
            takeUntil(this.stopper.stopper$),
        ).subscribe(value => this.valueChangesSubj$.next(value));
    }

    toggleDescription(event: Event, feature: NodeFeatureModel): void {
        if (this.isMobile() && (event.type === 'mouseenter' || event.type === 'mouseleave')) {
             return;
        }
        if (event.type === 'click') {
            event.stopPropagation();
            this.descriptionVisible = this.descriptionVisible ? null : feature;
        }

        if (event.type === 'mouseleave') {
            clearTimeout(this.descriptionShowTimeout);
            this.descriptionHideTimeout = setTimeout(() => {
                this.descriptionVisible = null;
                this.changeDetector.detectChanges();
            }, 200);
        }

        if (event.type === 'mouseenter') {
            clearTimeout(this.descriptionHideTimeout);
            this.descriptionShowTimeout = setTimeout(() => {
                this.descriptionVisible = feature;
                this.changeDetector.detectChanges();
            }, 200);
        }

        if (this.isMobile()) {
            this.descriptionPosition = 'bottom';
        } else {
            this.descriptionPosition = 'right';
        }
    }

    onHoverState(hover: boolean, feature: NodeFeatureModel): void {
        this.descriptionVisible = hover ? feature : null;
        clearTimeout(this.descriptionHideTimeout);
    }

    @HostListener('window:resize', ['$event'])
    onResize(event: Event): void {
        if (this.isMobile()) {
            this.descriptionVisible = null;
        }
    }

    /**
     * Returns if it's mobile device.
     */
    isMobile(): boolean {
        if (navigator.userAgent.match(/Android/i)
            || navigator.userAgent.match(/webOS/i)
            || navigator.userAgent.match(/iPhone/i)
            || navigator.userAgent.match(/iPad/i)
            || navigator.userAgent.match(/iPod/i)
            || navigator.userAgent.match(/BlackBerry/i)
            || navigator.userAgent.match(/Windows Phone/i)
            ) {
                return true;
            } else {
                return false;
            }
    }

}
