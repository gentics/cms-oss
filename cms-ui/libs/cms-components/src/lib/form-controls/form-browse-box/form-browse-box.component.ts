import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, forwardRef, Inject, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { AbstractControl, ControlValueAccessor, UntypedFormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, ValidationErrors, Validator } from '@angular/forms';
import { ItemInNode, RepositoryBrowserOptions } from '@gentics/cms-models';
import { merge, Observable, Subscription } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { GcmsUiServices, GCMS_UI_SERVICES_PROVIDER, I18nService } from '../../core';

export type ItemWithNode = { id: number; nodeId: number; } | null;
@Component({
    selector: 'gtx-form-browse-box',
    templateUrl: './form-browse-box.component.html',
    styleUrls: ['./form-browse-box.component.scss'],
    providers: [{
        provide: NG_VALUE_ACCESSOR, // Is an InjectionToken required by the ControlValueAccessor interface to provide a form value
        useExisting: forwardRef(() => FormBrowseBoxComponent), // tells Angular to use the existing instance
        multi: true,
    }, {
        provide: NG_VALIDATORS, // Is an InjectionToken required by this class to be able to be used as an Validator
        useExisting: forwardRef(() => FormBrowseBoxComponent), // for now validation will be put into the component, but can be separated
        multi: true,
    }],
    changeDetection: ChangeDetectionStrategy.OnPush,
    encapsulation: ViewEncapsulation.None,
})
export class FormBrowseBoxComponent implements ControlValueAccessor, Validator, OnInit, OnChanges, OnDestroy {

    @Input() label: string;
    @Input() required: boolean;
    @Input() activeContentLanguage: string;
    @Input() options: RepositoryBrowserOptions;

    @Output() blur: EventEmitter<FocusEvent> = new EventEmitter<FocusEvent>();

    itemControl = new UntypedFormControl();

    private _onChange: (_: any) => void;
    _onTouched: any;
    private _onValidatorChange: () => void;

    private item: ItemWithNode;
    private valueChangesSubscription: Subscription;

    itemDisplayValue$: Observable<any>;
    selectedItemBreadCrumbs: string;

    /** The helper for managing and loading the selected item. */
    private selectedItemHelper;

    constructor(
        private i18n: I18nService,
        private changeDetector: ChangeDetectorRef,
        @Inject(GCMS_UI_SERVICES_PROVIDER) private gcmsUiServices: GcmsUiServices,
    ) {}

    ngOnInit(): void {
        this.selectedItemHelper = this.gcmsUiServices.createSelectedItemsHelper(this.options.allowedSelection as "page" | "folder" | "file" | "image" | "form");
        this.itemDisplayValue$ = this.trackDisplayValue(this.itemControl);

        this.valueChangesSubscription = this.itemControl.valueChanges.subscribe((value: ItemWithNode) => {
            if (this.item !== undefined) {
                this.item = value;

                if (this._onChange) {
                    this._onChange(this.item);
                }
            }
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (this.item) {
            this.itemControl.setValue(
                this.item,
                {
                    onlySelf: true,
                    emitEvent: true,
                },
            );
        }
    }

    ngOnDestroy(): void {
        if (this.valueChangesSubscription) {
            this.valueChangesSubscription.unsubscribe();
        }
    }

    writeValue(item: ItemWithNode): void {
        this.selectedItemHelper.setSelectedItem(item.id, item.nodeId);
        this.item = item.id && typeof item.id === 'number' ? ({ id: item.id, nodeId: item?.nodeId }) : null;
        this.itemControl.setValue(this.item, {
            onlySelf: true,
            emitEvent: true,
        });
    }

    setValue(item: ItemInNode<any>): void {
        this.selectedItemHelper.setSelectedItem(item);
        this.item = item && typeof item.id === 'number' ? ({ id: item.id, nodeId: item?.nodeId }) : null;
        this.itemControl.setValue(this.item, {
            onlySelf: true,
            emitEvent: true,
        });
    }

    registerOnChange(fn: (_: any) => void): void {
        this._onChange = fn;
    }

    registerOnTouched(fn: any): void {
        this._onTouched = (event: FocusEvent) => {
            this.blur.emit(event);
            fn();
        }
    }

    setDisabledState?(isDisabled: boolean): void {
        if (isDisabled) {
            this.itemControl.disable({
                onlySelf: true,
                emitEvent: true,
            });
        } else {
            this.itemControl.enable({
                onlySelf: true,
                emitEvent: true,
            });
        }
    }

    validate(control: AbstractControl): ValidationErrors {
        const requiredError: ValidationErrors = this.validateRequired(control);
        if (!requiredError) {
            return null;
        }
        return Object.assign({}, requiredError);
    }

    registerOnValidatorChange?(fn: () => void): void {
        this._onValidatorChange = fn;
    }

    private validateRequired(control: AbstractControl): ValidationErrors {
        /**
         * if there is no i18n data, then
         *  - there is a required error, if the value is required in the current language
         */
        if (!this.item) {
            if (this.required) {
                return { required: true };
            }
        }

        /**
         * if there is no value in the current language, then
         *  - there is a required error, iff the value is required in the current language
         */
        if (!this.valuePresent()) {
            if (this.required) {
                return { required: true };
            }
        }

        return null;
    }

    /**
     * checks whether a value is present in the given language
     */
    private valuePresent(): boolean {
        if (this.item) {
            const value = this.item;
            if (typeof value === 'number') {
                return true;
            }
        }
        return false;
    }

    openRepositoryBrowser(options: RepositoryBrowserOptions): Promise<any> {
        if (this?.item?.nodeId) {
            options = { ...options, startNode: this.item.nodeId };

            if (this?.item['folderId']) {
                options = { ...options, startFolder: this.item['folderId'] };
            }
        }

        return this.gcmsUiServices.openRepositoryBrowser({ contentLanguage: this.activeContentLanguage, ...options })
        .then((selectedItem: ItemInNode) => {
            this.setValue(selectedItem);
        });
    }

    /**
     * @returns A string with the breadcrumbs path of the specified Page.
     */
     private generateBreadcrumbsPath(item: ItemInNode<any>): string {
        let breadcrumbsPath = '';
        if (item?.path) {
            breadcrumbsPath = item.path.replace('/', '');
            if (breadcrumbsPath.length > 0 && breadcrumbsPath.charAt(breadcrumbsPath.length - 1) === '/') {
                breadcrumbsPath = breadcrumbsPath.substring(0, breadcrumbsPath.length - 1);
            }
            breadcrumbsPath = breadcrumbsPath.split('/').join(' > ');
        }
        return breadcrumbsPath;
     }

     private trackDisplayValue(control: UntypedFormControl): Observable<any> {
        return merge(
            this.selectedItemHelper.selectedItem$.pipe(
                tap((item: ItemInNode) => this.selectedItemBreadCrumbs = this.generateBreadcrumbsPath(item)),
                map((selectedItem: ItemInNode) => {
                    if (selectedItem) {
                        this.item = selectedItem;
                        return selectedItem.name;
                    } else {
                        /**
                         * null is emitted, when nothing is selected.
                         * Also, null is emitted in case a referenced page got deleted and the tag property data was refetched.
                         * (Since the pageId in tagProperty gets removed).
                         */
                        let noItemSelectedText = '';
                        switch (this.options.allowedSelection) {
                            case 'page':
                                noItemSelectedText = 'editor.page_no_selection';
                                break;
                        }
                        return this.i18n.instant(noItemSelectedText);
                    }
                }),
            ),
            this.selectedItemHelper.loadingError$.pipe(
                map((error: { error: any, item: { item: number, nodeId?: number } }) => {
                    /**
                     * When a page that is referenced gets deleted, the pageId is kept in tagProperty.
                     * When we try to fetch the page information we get an error message.
                     * In that case we want to inform the user that the page got deleted
                     * (and thus avoid suggesting that a valid page is still selected).
                     */
                    if (control) {
                        /** additional check, in case the loadingError$ Subject is changed to a BehaviorSubject in the future.
                         * This could trigger an emission before this.tagProperty is set in updateTagProperty
                         */
                        return this.i18n.instant('editor.page_not_found', { id: control.value });
                    } else {
                        return '';
                    }
                    
                }),
            )
        ).pipe(
            tap(() => this.changeDetector.markForCheck()),
        );
     }

}
