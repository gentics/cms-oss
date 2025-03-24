import {
    coerceToBoolean,
    DeactivationConfig,
    DisableableComponent,
    ObservableStopper,
    PermissionsCheckResult,
    UserActionPermissions,
    RequiredInstancePermissions,
    RequiredPermissions,
} from '@admin-ui/common';
import { I18nService, PermissionsService } from '@admin-ui/core';
import {
    ChangeDetectorRef,
    Directive,
    ElementRef,
    Input,
    OnDestroy,
    OnInit,
    Optional,
    Self,
} from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { IndexByKey } from '@gentics/cms-models';
import {
    ButtonComponent,
    CheckboxComponent,
    DateTimePickerComponent,
    DropdownItemComponent,
    InputComponent,
    RadioButtonComponent,
    RangeComponent,
    SelectComponent,
    SplitButtonComponent,
    TabComponent,
    TabPaneComponent,
    TextareaComponent,
} from '@gentics/ui-core';
import { BehaviorSubject, combineLatest, Observable, of as observableOf, of, Subscription } from 'rxjs';
import { map, switchMap, takeUntil } from 'rxjs/operators';
import { DashboardItemComponent } from '../../../dashboard/components/dashboard-item/dashboard-item.component';

/**
 * A component that should be used with the `ActionAllowedDirective` has to implement either
 * the `DisableableComponent` or the `DisableableControlValueAccessor` interface,
 * i.e., the component must provide either a `setDisabledState()` method or a `disabled` property.
 */
export type DisableableControlValueAccessor = Required<Pick<ControlValueAccessor, 'setDisabledState'>>;

export const DEFAULT_DISABLED_TOOLTIP = 'common.not_enough_permissions_tooltip';
export const ACTION_HIDDEN_CSS_CLASS = 'gtx-action-no-perms-hidden';

export const GTX_ACTION_ALLOWED_SELECTOR = `
    gtx-button[gtxActionAllowed],
    gtx-checkbox[gtxActionAllowed],
    gtx-dashboard-item[gtxActionAllowed],
    gtx-date-time-picker[gtxActionAllowed],
    gtx-dropdown-item[gtxActionAllowed],
    gtx-input[gtxActionAllowed],
    gtx-radio-button[gtxActionAllowed],
    gtx-range[gtxActionAllowed],
    gtx-select[gtxActionAllowed],
    gtx-split-button[gtxActionAllowed],
    gtx-tab-pane[gtxActionAllowed],
    gtx-tab[gtxActionAllowed],
    gtx-textarea[gtxActionAllowed],
    gtx-browse-box[gtxActionAllowed],
`;

/**
 * Checks if the current user is allowed to execute a specific action and disables (or hides)
 * the element, to which the directive is applied, if the user does not have sufficient permissions.
 *
 * User actions are defined in `user-action-permissions.ts` and are referenced
 * in strings as `'<module>.<actionId>'`, e.g., `'user.createUser'`.
 * If no action ID is set or if it is set to a falsy value, the permissions are treated as granted.
 *
 * This directive can be used to check type and instance permissions, depending on whether the `aaInstanceId`
 * input property is set or not.
 * **Important:** If the user action requires permissions on multiple AccessControlledTypes, the instance ID
 * will only apply to the first object in the `UserActionPermissions.permissions` array.
 * This means that `UserActionPermissions.permissions[0]` is treated as `RequiredInstancePermissions` if
 * `aaInstanceId` is set and otherwise as `RequiredTypePermissions`. All other members of
 * `UserActionPermissions.permissions[0]` are always treated as `RequiredTypePermissions`.
 *
 * @note For hiding elements the CSS class `gtx-action-no-perms-hidden` defined in `_global-styles.scss` is used.
 *
 * @example
 * // Disable the button if the user doesn't have permissions to execute 'user.createUser'.
 * <gtx-button (click)="onCreateUserClick()" gtxActionAllowed="user.createUser"></gtx-button>
 * // Hide the button if the user doesn't have permissions to execute 'user.createUser'.
 * <gtx-button (click)="onCreateUserClick()" gtxActionAllowed="user.createUser" aaHideElement="true"></gtx-button>
 */
@Directive({
    selector: GTX_ACTION_ALLOWED_SELECTOR,
})
export class ActionAllowedDirective implements OnInit, OnDestroy {

    /**
     * ID of the user action to be checked.
     *
     * User actions are defined in `user-action-permissions.ts` and are referenced
     * in strings as `'<module>.<actionId>'`, e.g., `'user.createUser'`.
     *
     * If this is not set or set to a falsy value, the permissions are treated as granted.
     */
    @Input('gtxActionAllowed')
    get actionId(): string {
        return this.actionId$.value;
    }
    set actionId(value: string) {
        this.actionId$.next(value);
    }

    /**
     * If `true`, the element is hidden instead of being disabled (default: false).
     */
    @Input()
    get aaHideElement(): boolean {
        return this.hideElement$.value;
    }
    set aaHideElement(value: boolean) {
        this.hideElement$.next(coerceToBoolean(value));
    }

    /**
     * If permissions on an instance of a type need to be checked this must be set to the Id of the instance.
     */
    @Input()
    get aaInstanceId(): number | string {
        return this.instanceId$.value;
    }
    set aaInstanceId(value: number | string) {
        this.instanceId$.next(value);
    }

    /**
     * When checking an instance permission, this may be set to the ID of the node, where the instance is located.
     */
    @Input()
    get aaNodeId(): number {
        return this.nodeId$.value;
    }
    set aaNodeId(value: number) {
        this.nodeId$.next(value);
    }

    @Input()
    get aaOverrideCheck(): PermissionsCheckResult | Observable<PermissionsCheckResult> {
        return this.overrideCheck$.value;
    }
    set aaOverrideCheck(value: PermissionsCheckResult | Observable<PermissionsCheckResult>) {
        // If it has a subscribe, it's an observable
        if (value != null && typeof value === 'object' && value.hasOwnProperty('subscribe') && typeof (value as any).subscribe === 'function') {
            this.unsubscribeOverrideCheck(false);
            this.overrideCheckSub = (value as Observable<PermissionsCheckResult>).subscribe(subVal => {
                this.overrideCheck$.next(subVal);
            });
        } else {
            this.overrideCheck$.next(value as PermissionsCheckResult);
        }
    }

    /**
     * If `true`, the element is always disabled, regardless of the permission status (default: false).
     *
     * This can be useful, e.g., if a button be disabled when a form is invalid.
     *
     * This property replaces the `disabled` property of the host component to avoid the `gtxActionAllowed` directive
     * overriding that property.
     *
     * @note For info about the replacement of the host component's `disabled` property
     * see https://github.com/angular/angular/pull/3419
     */
    @Input()
    get disabled(): boolean {
        return this.alwaysDisabled$.value;
    }
    set disabled(value: boolean) {
        this.alwaysDisabled$.next(coerceToBoolean(value));
    }

    private actionId$ = new BehaviorSubject<string>(undefined);
    private hideElement$ = new BehaviorSubject<boolean>(false);
    private instanceId$ = new BehaviorSubject<number | string>(undefined);
    private nodeId$ = new BehaviorSubject<number>(undefined);
    private overrideCheck$ = new BehaviorSubject<PermissionsCheckResult>(null);
    private alwaysDisabled$ = new BehaviorSubject<boolean>(false);

    private stopper = new ObservableStopper();
    private i18nSub: Subscription;
    private overrideCheckSub: Subscription;

    /** The original tooltip (if any) on the elment before changing it to the disabledTooltip. */
    private originalTooltip: string;

    private hostComponent: DisableableControlValueAccessor | DisableableComponent;

    /** Independantly from this directive a disabled property could be set, which shall override disabled state set by this directive. */
    private elementIsDisabledExternal: boolean;

    // Unfortunately there is no generic way to have the host component injected,
    // so we need to list all supported component types explicitly.
    // When adding a component type to this list, make sure that it is added to
    // the selector in the @Directive decorator as well.
    constructor(
        private changeDetector: ChangeDetectorRef,
        private elementRef: ElementRef<HTMLElement>,
        private i18n: I18nService,
        private permissionsService: PermissionsService,
        @Self() @Optional() gtxButton: ButtonComponent,
        @Self() @Optional() gtxCheckbox: CheckboxComponent,
        @Self() @Optional() gtxDashboardItem: DashboardItemComponent,
        @Self() @Optional() gtxDateTimePicker: DateTimePickerComponent,
        @Self() @Optional() gtxDropdownItem: DropdownItemComponent,
        @Self() @Optional() gtxInput: InputComponent,
        @Self() @Optional() gtxRadio: RadioButtonComponent,
        @Self() @Optional() gtxRange: RangeComponent,
        @Self() @Optional() gtxSelect: SelectComponent,
        @Self() @Optional() gtxSplitButton: SplitButtonComponent,
        @Self() @Optional() gtxTab: TabComponent,
        @Self() @Optional() gtxTabPane: TabPaneComponent,
        @Self() @Optional() gtxTextarea: TextareaComponent,
    ) {
        this.hostComponent =
            gtxButton ||
            gtxCheckbox ||
            gtxDashboardItem ||
            gtxDateTimePicker ||
            gtxDropdownItem ||
            gtxInput ||
            gtxRadio ||
            gtxRange ||
            gtxSelect ||
            gtxSplitButton ||
            gtxTab ||
            gtxTabPane ||
            gtxTextarea;
    }

    ngOnInit(): void {
        this.elementIsDisabledExternal = (this.hostComponent as DisableableComponent).disabled;
        this.setUpPermissionChecking();
    }

    ngOnDestroy(): void {
        this.stopper.stop();
        this.unsubscribeOverrideCheck();
    }

    /**
     * Unsubscribes the i18n subscription that is created when translating the
     * `disabledTooltip`. This subscription needs to be terminated whenever we enable
     * the element again, thus we cannot simply use `this.stopper`.
     */
    private unsubscribeI18n(): void {
        if (this.i18nSub) {
            this.i18nSub.unsubscribe();
            this.i18nSub = null;
        }
    }

    private unsubscribeOverrideCheck(clear: boolean = true): void {
        if (this.overrideCheckSub) {
            this.overrideCheckSub.unsubscribe();
            if (clear) {
                this.overrideCheckSub = null;
            }
        }
    }

    private setUpPermissionChecking(): void {
        const reqActionPerms$ = this.actionId$.pipe(
            map(actionId => this.permissionsService.getUserActionPermsForId(actionId)),
        );

        const permissionsResult$ = combineLatest([
            reqActionPerms$,
            this.instanceId$,
            this.nodeId$,
        ]).pipe(
            switchMap(([actionPerms, instanceId, nodeId]) => this.checkPermissions(actionPerms, instanceId, nodeId)),
        );

        combineLatest([
            permissionsResult$,
            this.hideElement$,
            this.alwaysDisabled$,
        ]).pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(([permissionsResult, hideElement, alwaysDisabled]) => this.changeElementStatus(permissionsResult, { hideElement, alwaysDisabled }));
    }

    /**
     * Executes the permissions check.
     *
     * If `actionPerms` is null, the permission is granted by default.
     * If instance permissions need to be checked, but there is no `instanceId`, the permission is denied.
     */
    private checkPermissions(actionPerms: UserActionPermissions, instanceId?: number | string, nodeId?: number): Observable<PermissionsCheckResult> {
        return this.overrideCheck$.pipe(
            switchMap(result => {
                if (result != null) {
                    if (typeof result === 'object' && result.hasOwnProperty('_isScalar') && typeof (result as any).subscribe === 'function') {
                        return result as any as Observable<PermissionsCheckResult>;
                    } else {
                        return of(result);
                    }
                }

                let permissionsGranted$: Observable<boolean>;

                if (actionPerms) {
                    const requiredPerms = this.assembleRequiredPermissions(actionPerms, instanceId, nodeId);
                    if (requiredPerms) {
                        permissionsGranted$ = this.permissionsService.checkPermissions(requiredPerms);
                    } else {
                        permissionsGranted$ = observableOf(false);
                    }
                } else {
                    // If no action was specified, the permissions are granted by default.
                    permissionsGranted$ = observableOf(true);
                }

                return permissionsGranted$.pipe(
                    map(granted => ({ actionPerms, granted })),
                );
            }),
        );
    }

    /**
     * @returns the `RequiredPermissions` array needed by the PermissionsService.
     * If there are instancePermissions to be checked, but no instanceId, `null` is returned.
     */
    private assembleRequiredPermissions(actionPerms: UserActionPermissions, instanceId?: number | string, nodeId?: number): RequiredPermissions[] {
        const allPerms: RequiredPermissions[] = actionPerms.typePermissions ? [ ...actionPerms.typePermissions ] : [];

        if (actionPerms.instancePermissions) {
            if (typeof instanceId === 'number' || typeof instanceId === 'string') {
                const instancePerms: RequiredInstancePermissions = {
                    ...actionPerms.instancePermissions,
                    instanceId,
                    nodeId,
                };
                allPerms.push(instancePerms);
            } else {
                // If there are instancePermissions to be checked, but not instanceId,
                // the permissions are treated as not granted.
                return null;
            }
        }

        return allPerms;
    }

    private changeElementStatus(permissionsResult: PermissionsCheckResult, deactivationConfig: DeactivationConfig): void {
        if (permissionsResult.granted) {
            this.showElement();
            this.enableElement();
        } else {
            if (deactivationConfig.hideElement) {
                this.hideElement();
            } else {
                // We need to show the element here, because aaHideElement could have been true the first
                // time the permissions were checked (and thus the element was hidden) and then aaHideElement
                // could have been changed to false (so the element needs to be disabled, but not hidden).
                this.showElement();
                this.disableElement(permissionsResult.actionPerms);
            }
        }

        // If the component should always be disabled we disable it now, regardless of the permissions or hide status.
        if (deactivationConfig.alwaysDisabled) {
            // When disabling because alwaysDisabled is true, we don't need to set a tooltip, so we can change the state directly.
            this.setElementDisabledState(true);
        }

        this.changeDetector.markForCheck();
    }

    private disableElement(actionPerms: UserActionPermissions): void {
        this.setElementDisabledState(true);
        this.originalTooltip = this.elementRef.nativeElement.title;

        const disabledTooltipI18n = actionPerms && actionPerms.disabledTooltip || DEFAULT_DISABLED_TOOLTIP;
        let i18nKey: string;
        let i18nParams: IndexByKey<any>;
        if (typeof disabledTooltipI18n === 'string') {
            i18nKey = disabledTooltipI18n;
        } else {
            i18nKey = disabledTooltipI18n.key;
            i18nParams = disabledTooltipI18n.params;
        }

        this.unsubscribeI18n();
        this.i18nSub = this.i18n.get(i18nKey, i18nParams).pipe(
            takeUntil(this.stopper.stopper$),
        )
            .subscribe(translation => this.elementRef.nativeElement.title = translation);
    }

    private enableElement(): void {
        this.setElementDisabledState(false);

        this.unsubscribeI18n();
        if (this.originalTooltip) {
            this.elementRef.nativeElement.title = this.originalTooltip;
        } else {
            this.elementRef.nativeElement.removeAttribute('title');
        }
        this.originalTooltip = null;
    }

    private hideElement(): void {
        this.elementRef.nativeElement.classList.add(ACTION_HIDDEN_CSS_CLASS);
    }

    private showElement(): void {
        this.elementRef.nativeElement.classList.remove(ACTION_HIDDEN_CSS_CLASS);
    }

    private setElementDisabledState(isDisabled: boolean): void {
        if (typeof (this.hostComponent as DisableableControlValueAccessor).setDisabledState === 'function') {
            (this.hostComponent as DisableableControlValueAccessor).setDisabledState(isDisabled);
        } else {
            (this.hostComponent as DisableableComponent).disabled = isDisabled;
        }
    }

}
