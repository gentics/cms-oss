import { DataSourceHandlerService } from '@admin-ui/core';
import { MarkupLanguageDataService } from '@admin-ui/shared';
import { animate, state, style, transition, trigger } from '@angular/animations';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
} from '@angular/core';
import { AbstractControl, ControlValueAccessor, FormControl, UntypedFormArray, UntypedFormControl, Validators } from '@angular/forms';
import { CONTROL_INVALID_VALUE } from '@gentics/cms-components';
import { DataSource, Language, MarkupLanguage, Raw, TagPart } from '@gentics/cms-models';
import { ModalService, generateFormProvider } from '@gentics/ui-core';
import { isEqual } from'lodash-es'
import { cloneDeep } from 'lodash-es';
import { Subscription, combineLatest } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, map } from 'rxjs/operators';
import { ConstructPartPropertiesMode } from '../construct-part-properties/construct-part-properties.component';
import { CreateConstructPartModalComponent } from '../create-construct-part-modal/create-construct-part-modal.component';

interface DisplayItem {
    item: TagPart;
    state: {
        keyword: string;
        collapsed: boolean;
        hidden: boolean;
    };
}

@Component({
    selector: 'gtx-construct-part-list',
    templateUrl: './construct-part-list.component.html',
    styleUrls: ['./construct-part-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(ConstructPartListComponent)],
    animations: [
        trigger('slideAnim', [
            state('in', style({
                opacity: 1,
                height: '*',
                'padding-top': '*',
                'padding-bottom': '*',
                'margin-top': '*',
                'margin-bottom': '*',
            })),
            transition(':enter', [
                style({
                    opacity: 0,
                    height: '0rem',
                    'padding-top': '0',
                    'padding-bottom': '0',
                    'margin-top': '0',
                    'margin-bottom': '0',
                }),
                animate(100),
            ]),
            transition(':leave',
                animate(100, style({
                    opacity: 0,
                    height: '0rem',
                    'padding-top': '0',
                    'padding-bottom': '0',
                    'margin-top': '0',
                    'margin-bottom': '0',
                })),
            ),
        ]),
    ],
})
export class ConstructPartListComponent implements OnInit, OnDestroy, ControlValueAccessor {

    public readonly ConstructPartPropertiesMode = ConstructPartPropertiesMode;

    @Input()
    public allowCreation = false;

    @Input()
    public supportedLanguages: Language[];

    @Input()
    public initialValue = true;

    @Output()
    public initialValueChange = new EventEmitter<boolean>();

    public form: UntypedFormArray;
    public displayItems: DisplayItem[] = [];
    public allCollapsed = true;

    public markupLanguages: MarkupLanguage<Raw>[];
    public dataSources: DataSource<Raw>[];

    protected internalParts: TagPart[] = [];
    protected clonedParts: TagPart[] = [];

    private disabled = false;
    private subscription: Subscription;
    private partSubscriptions: Subscription[] = [];
    private otherSubscriptions: Subscription[] = [];
    private cvaChange: (value: any) => void;
    private cvaTouch: () => void;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private modals: ModalService,
        private dataSourceHandler: DataSourceHandlerService,
        private markupLanguageData: MarkupLanguageDataService,
    ) { }

    ngOnInit(): void {
        this.otherSubscriptions.push(this.markupLanguageData.watchAllEntities().subscribe(languages => {
            this.markupLanguages = languages;
            this.changeDetector.markForCheck();
        }));
        this.otherSubscriptions.push(this.dataSourceHandler.listMapped().subscribe(res => {
            this.dataSources = res.items;
            this.changeDetector.markForCheck();
        }));

        this.form = new UntypedFormArray([], (ctl) => {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            if ((ctl.value || []).find(value => value === CONTROL_INVALID_VALUE)) {
                return { invalid: true };
            }
        });

        if (this.disabled) {
            this.form.disable({ emitEvent: false });
        }

        this.subscription = combineLatest([
            this.form.valueChanges,
            this.form.statusChanges,
        ]).pipe(
            // Do not emit values if disabled/pending
            filter(([, status]) => status !== 'DISABLED' && status !== 'PENDING'),
            map(([value, status]) => {
                if (status === 'VALID') {
                    return value;
                }
                return CONTROL_INVALID_VALUE;
            }),
            distinctUntilChanged(isEqual),
            debounceTime(100),
        ).subscribe(value => {
            // Only trigger a change if the value actually changed or gone invalid.
            // Ignores the first value change, as it's a value from the initial setup.
            if (value === CONTROL_INVALID_VALUE || (!this.initialValue && !isEqual(this.clonedParts, value))) {
                this.triggerChange(value);
            }
            // Set it, in case that the parent-component has no binding for it
            this.initialValue = false;
            this.initialValueChange.emit(false);
        });
    }

    ngOnDestroy(): void {
        if (this.subscription) {
            this.subscription.unsubscribe();
        }
        if (this.partSubscriptions) {
            this.partSubscriptions.forEach(s => s.unsubscribe());
            this.partSubscriptions = [];
        }
        this.otherSubscriptions.forEach(s => s.unsubscribe());
    }

    writeValue(parts: any): void {
        parts = parts || [];

        if (!Array.isArray(parts)) {
            return;
        }

        // If the amount of parts has changed, we need to rebuild the form
        if (parts.length !== this.internalParts.length) {
            // Unsubscribe from all controls, as they will be removed
            if (this.partSubscriptions) {
                this.partSubscriptions.forEach(s => s.unsubscribe());
                this.partSubscriptions = [];
            }

            this.displayItems = [];
            this.form.clear({ emitEvent: false });
            this.internalParts = parts;
            this.clonedParts = cloneDeep(parts);

            parts.forEach((singlePart, index) => {
                const ctl = new UntypedFormControl(singlePart);
                this.form.push(ctl);
                this.observeSingleTag(ctl, index);

                this.displayItems.push({
                    item: singlePart,
                    state: {
                        keyword: singlePart.keyword,
                        collapsed: true,
                        hidden: false,
                    },
                });
            });
        } else {
            this.internalParts = parts;
            this.clonedParts = cloneDeep(parts);
            this.form.setValue(parts);
            this.displayItems = [];

            parts.forEach((singlePart, index) => {
                this.displayItems.push({
                    item: singlePart,
                    state: {
                        collapsed: true,
                        hidden: false,
                        keyword: singlePart.keyword,
                        ...this.displayItems[index]?.state,
                    },
                });
            });
        }

        this.toggleRowAll(false);
        this.changeDetector.markForCheck();
    }

    registerOnChange(fn: any): void {
        this.cvaChange = fn;
    }

    registerOnTouched(fn: any): void {
        this.cvaTouch = fn;
    }

    setDisabledState(disabled: boolean): void {
        this.disabled = disabled;

        if (!this.form) {
            return;
        }

        if (disabled) {
            this.form.disable({ emitEvent: false });
        } else {
            this.form.enable({ emitEvent: false });
        }
    }

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    triggerChange(value: any): void {
        if (typeof this.cvaChange === 'function') {
            this.cvaChange(value);
        }
    }

    triggerTouch(): void {
        if (typeof this.cvaTouch === 'function') {
            this.cvaTouch();
        }
    }

    updateInitialValueFlag(value: boolean): void {
        this.initialValue = value;
        this.initialValueChange.emit(value);
    }

    /**
     * Tracking function for ngFor for better performance.
     */
    identify(index: number, part: DisplayItem): string {
        return `${part?.item?.keyword}:${index}`;
    }

    async createNewPart(): Promise<void> {
        const usedOrders = this.internalParts.map(part => part.partOrder);
        const highestOrder = Math.max(...usedOrders);

        const modalInput: any = {
            supportedLanguages: this.supportedLanguages,
            keywordBlacklist: this.internalParts.map(part => part.keyword),
            orderBlacklist: usedOrders,
        };

        if (highestOrder > 0) {
            modalInput.defaultOrder = highestOrder + 1;
        }

        const dialog = await this.modals.fromComponent(CreateConstructPartModalComponent, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
            width: '80%',
        }, modalInput);

        const created: TagPart<Raw> = await dialog.open();

        if (!created) {
            return;
        }

        const ctl: FormControl = new UntypedFormControl(created, Validators.required);
        const index = this.internalParts.push(created) - 1;

        this.observeSingleTag(ctl, index);
        this.form.push(ctl, { emitEvent: false });
        this.displayItems.push({
            item: created,
            state: {
                keyword: created.keyword,
                collapsed: true,
                hidden: false,
            },
        });

        this.triggerChange(this.form.value);
        this.changeDetector.markForCheck();

        setTimeout(() => {
            ctl.markAsDirty();
            ctl.markAsTouched();
            ctl.updateValueAndValidity();
            this.form.markAsDirty();
            this.form.markAsTouched();
            this.form.updateValueAndValidity();
        }, 10);
    }

    private observeSingleTag(control: AbstractControl, index: number): void {
        this.partSubscriptions.push(control.valueChanges.subscribe(newValue => {
            if (newValue !== CONTROL_INVALID_VALUE) {
                this.internalParts[index] = newValue || this.internalParts[index];
                this.changeDetector.markForCheck();
            }
        }));
    }

    deletePart(index: number, event?: MouseEvent): void {
        if (event) {
            event.preventDefault();
            event.stopImmediatePropagation();
            event.stopPropagation();
        }

        this.partSubscriptions.forEach(sub => sub.unsubscribe());
        this.partSubscriptions = [];

        // Instead of splice, using slice and creating a new array,
        // so angular can properly detect that it's a new object.
        this.internalParts = [
            ...this.internalParts.slice(0, index),
            ...this.internalParts.slice(index + 1),
        ];
        this.displayItems = [
            ...this.displayItems.slice(0, index),
            ...this.displayItems.slice(index + 1),
        ];
        this.form.removeAt(index, { emitEvent: false });

        for (let i = 0; i < this.form.length; i++) {
            const ctl = this.form.controls[i];
            this.observeSingleTag(ctl, i);
        }

        this.form.markAsDirty();
        this.form.updateValueAndValidity();
        this.triggerChange(this.form.value);
        this.changeDetector.markForCheck();
    }

    /**
     * Collapses or expands node data row.
     *
     * @param index the index of the item
     */
    toggleRow(index: number): void {
        this.displayItems[index].state.collapsed = !this.displayItems[index].state.collapsed;
    }

    toggleRowAll(allCollapsed?: boolean): void {
        if (typeof allCollapsed === 'boolean') {
            this.allCollapsed = allCollapsed;
        } else {
            this.allCollapsed = !this.allCollapsed;
        }

        this.displayItems.forEach(item => {
            item.state.collapsed = allCollapsed;
        });

        this.changeDetector.markForCheck();
    }
}
