import { InterfaceOf, MultiModuleUserActionPermissions, USER_ACTION_PERMISSIONS, UserActionPermissions } from '@admin-ui/common';
import { I18nService, PermissionsService, RequiredInstancePermissions, RequiredPermissions } from '@admin-ui/core';
import { componentTest } from '@admin-ui/testing';
import { AfterViewInit, Component, ElementRef, NO_ERRORS_SCHEMA, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { GcmsUiLanguage } from '@gentics/cms-integration-api-models';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { ButtonComponent, GenticsUICoreModule, InputComponent } from '@gentics/ui-core';
import { cloneDeep } from 'lodash-es';
import { BehaviorSubject, Observable, combineLatest, of as observableOf } from 'rxjs';
import { delay, map } from 'rxjs/operators';
import { MockI18nServiceWithSpies } from '../../../core/providers/i18n/i18n.service.mock';
import { ACTION_HIDDEN_CSS_CLASS, ActionAllowedDirective, DEFAULT_DISABLED_TOOLTIP } from './action-allowed.directive';

function assembleTestTemplate(additionalAttributes: string): string {
    return `<gtx-button #actionButton [gtxActionAllowed]="userAction" ${additionalAttributes}>Action Button</gtx-button>`;
}

@Component({
    template: assembleTestTemplate(''),
    standalone: false,
})
class TestComponent {
    userAction = 'typeTests.testAction';

    @ViewChild('actionButton')
    actionButton: ButtonComponent;

    @ViewChild('actionButton', { static: true, read: ElementRef })
    actionButtonElement: ElementRef<HTMLElement>;

    // The following properties are not referenced in all tests.
    hideElement = false;
    disabled: boolean;
    instanceId: number;
    nodeId: number;
}

@Component({
    template: '<gtx-input #inputField [gtxActionAllowed]="userAction"></gtx-input>',
    standalone: false,
})
class InputTestComponent implements AfterViewInit {

    userAction = 'typeTests.testAction';

    @ViewChild('inputField')
    inputField: InputComponent;

    @ViewChild('inputField', { static: true, read: ElementRef })
    inputFieldElement: ElementRef<HTMLElement>;

    setDisabledStateSpy: jasmine.Spy;

    ngAfterViewInit(): void {
        this.setDisabledStateSpy = spyOn(this.inputField, 'setDisabledState').and.callThrough();
    }

}

class MockPermissionsService implements Partial<InterfaceOf<PermissionsService>> {
    private currPermissions = new BehaviorSubject<boolean>(true);

    checkPermissions = jasmine.createSpy('checkPermissions').and
        .returnValue(this.currPermissions.pipe(delay(0)));

    mockPermissionsGranted(value: boolean): void {
        this.currPermissions.next(value);
    }

    public getUserActionPermsForId(actionId: string): UserActionPermissions {
        if (!actionId) {
            return null;
        }

        const parts = actionId.split('.');
        if (parts.length !== 2) {
            throw new Error(`Malformed user action ID provided to gtxActionAllowed directive: '${actionId}'.` +
                'Make sure that you use the format \'<module>.actionId');
        }

        const module = MOCK_USER_ACTIONS[parts[0]];
        if (!module) {
            throw new Error(`User Action module '${parts[0]}' does not exist.`);
        }
        const reqPerms = module[parts[1]];
        if (!reqPerms) {
            throw new Error(`User Action '${parts[1]}' does not exist within module '${parts[0]}'.`);
        }
        return reqPerms;
    }
}

class MockI18nWithLangChange implements Partial<InterfaceOf<I18nService>> {
    private currLang$ = new BehaviorSubject<GcmsUiLanguage>('en');

    get(key: string): Observable<string> {
        return combineLatest([
            observableOf(key),
            this.currLang$,
        ]).pipe(
            map(([translation, lang]) => `${lang}-${translation}`),
        );
    }

    setLanguage(lang: GcmsUiLanguage): void {
        this.currLang$.next(lang);
    }
}

const TEST_TOOLTIP = 'testTooltip';

const MOCK_USER_ACTIONS = {
    typeTests: {
        testAction: {
            typePermissions: [
                {
                    type: AccessControlledType.ADMIN,
                    permissions: [
                        GcmsPermission.READ,
                        GcmsPermission.DELETE_ITEMS,
                    ],
                },
            ],
            disabledTooltip: TEST_TOOLTIP,
        },
        testAction2: {
            typePermissions: [
                {
                    type: AccessControlledType.ADMIN,
                    permissions: [
                        GcmsPermission.READ,
                        GcmsPermission.DELETE_ITEMS,
                    ],
                },
            ],
            disabledTooltip: TEST_TOOLTIP,
        },
    },
    instanceTests: {
        instancePermsOnly: {
            instancePermissions: {
                type: AccessControlledType.FOLDER,
                permissions: [ GcmsPermission.READ ],
            },
            disabledTooltip: TEST_TOOLTIP,
        },
        typeAndInstancePerms: {
            typePermissions: [
                {
                    type: AccessControlledType.ADMIN,
                    permissions: [
                        GcmsPermission.READ,
                        GcmsPermission.EDIT_IMPORT,
                    ],
                },
            ],
            instancePermissions: {
                type: AccessControlledType.FOLDER,
                permissions: [
                    GcmsPermission.READ,
                ],
            },
            disabledTooltip: TEST_TOOLTIP,
        },
    },
    tooltipTests: {
        noTooltip: {
            typePermissions: [
                {
                    type: AccessControlledType.USER_ADMIN,
                    permissions: [
                        GcmsPermission.READ,
                        GcmsPermission.CREATE_USER,
                    ],
                },
            ],
        },
        tooltipParams: {
            typePermissions: [
                {
                    type: AccessControlledType.ADMIN,
                    permissions: [ GcmsPermission.READ ],
                },
            ],
            disabledTooltip: {
                key: TEST_TOOLTIP,
                params: {
                    a: 'test',
                },
            },
        },
    },
};

const INSTANCE_ID_A = 4711;
const INSTANCE_ID_B = 1234;
const NODE_ID_A = 1;
const NODE_ID_B = 2;

describe('ActionAllowedDirective', () => {

    let i18n: MockI18nServiceWithSpies;
    let permissions: MockPermissionsService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                GenticsUICoreModule,
            ],
            declarations: [
                ActionAllowedDirective,
                TestComponent,
                InputTestComponent,
            ],
            providers: [
                { provide: I18nService, useClass: MockI18nServiceWithSpies },
                { provide: PermissionsService, useClass: MockPermissionsService },
                { provide: USER_ACTION_PERMISSIONS, useValue: cloneDeep(MOCK_USER_ACTIONS as MultiModuleUserActionPermissions) },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();
    });

    /** Some tests need to override the TestComponent's template, which must happen before injecting anything. */
    function initServices(): void {
        i18n = TestBed.get(I18nService);
        i18n.setLanguage('en');
        permissions = TestBed.get(PermissionsService);
    }

    function assertActionState(instance: TestComponent, expected: { disabled: boolean, tooltip?: string, hidden: boolean }): void {
        expect(instance.actionButton.disabled).toBe(expected.disabled, 'control.disabled state did not match expected value');
        if (typeof expected.tooltip === 'string') {
            expect(instance.actionButtonElement.nativeElement.title).toBeTruthy('No tooltip was set.');
            expect(instance.actionButtonElement.nativeElement.title).toEqual(expected.tooltip, 'The tooltip was incorrect.');
        } else {
            expect(instance.actionButtonElement.nativeElement.title).toEqual('', 'Did not expect any tooltip now.');
        }
        expect(instance.actionButtonElement.nativeElement.classList.contains(ACTION_HIDDEN_CSS_CLASS)).toBe(expected.hidden);
    }

    function assertInputFieldState(
        instance: InputTestComponent,
        expected: { disabled: boolean, tooltip?: string, hidden: boolean },
    ): void {
        expect(instance.setDisabledStateSpy.calls.all().length).toBe(1, 'setDisabledState() was not called');
        expect(instance.setDisabledStateSpy).toHaveBeenCalledWith(expected.disabled);
        expect(instance.inputField.disabled).toBe(expected.disabled, 'control.disabled state did not match expected value');

        if (typeof expected.tooltip === 'string') {
            expect(instance.inputFieldElement.nativeElement.title).toBeTruthy('No tooltip was set.');
            expect(instance.inputFieldElement.nativeElement.title).toEqual(expected.tooltip, 'The tooltip was incorrect.');
        } else {
            expect(instance.inputFieldElement.nativeElement.title).toEqual('', 'Did not expect any tooltip now.');
        }
        expect(instance.inputFieldElement.nativeElement.classList.contains(ACTION_HIDDEN_CSS_CLASS)).toBe(expected.hidden);

        instance.setDisabledStateSpy.calls.reset();
    }

    function runDoubleChangeDetection(fixture: ComponentFixture<any>): void {
        fixture.detectChanges();
        tick();
        fixture.detectChanges();
    }

    describe('TypePermissions checking', () => {

        beforeEach(() => initServices());

        it('checks permissions and leaves the control enabled if permissions are initially granted',
            componentTest(() => TestComponent, (fixture, instance) => {
                runDoubleChangeDetection(fixture);

                expect(permissions.checkPermissions).toHaveBeenCalledTimes(1);
                expect(permissions.checkPermissions).toHaveBeenCalledWith(MOCK_USER_ACTIONS.typeTests.testAction.typePermissions);
                expect(i18n.get).not.toHaveBeenCalled();

                assertActionState(instance, { disabled: false, hidden: false });
            }),
        );

        it('checks permissions, disables the control, and sets the translated tooltip if permissions are initially denied',
            componentTest(() => TestComponent, (fixture, instance) => {
                permissions.mockPermissionsGranted(false);
                runDoubleChangeDetection(fixture);

                expect(permissions.checkPermissions).toHaveBeenCalledTimes(1);
                expect(permissions.checkPermissions).toHaveBeenCalledWith(MOCK_USER_ACTIONS.typeTests.testAction.typePermissions);
                expect(i18n.get).toHaveBeenCalledTimes(1);
                expect(i18n.get).toHaveBeenCalledWith(TEST_TOOLTIP, undefined);

                assertActionState(instance, { disabled: true, tooltip: TEST_TOOLTIP,  hidden: false });
            }),
        );

        it('reacts to permission changes',
            componentTest(() => TestComponent, (fixture, instance) => {
                permissions.mockPermissionsGranted(false);
                runDoubleChangeDetection(fixture);

                assertActionState(instance, { disabled: true, tooltip: TEST_TOOLTIP,  hidden: false });

                permissions.mockPermissionsGranted(true);
                tick();
                fixture.detectChanges();
                assertActionState(instance, { disabled: false, hidden: false });

                expect(permissions.checkPermissions.calls.count()).toBe(1, 'checkPermissions() should be executed only once');
            }),
        );

        it('reacts to actionId changes',
            componentTest(() => TestComponent, (fixture, instance) => {
                runDoubleChangeDetection(fixture);

                expect(permissions.checkPermissions).toHaveBeenCalledTimes(1);
                expect(permissions.checkPermissions).toHaveBeenCalledWith(MOCK_USER_ACTIONS.typeTests.testAction.typePermissions);
                assertActionState(instance, { disabled: false, hidden: false });

                instance.userAction = 'typeTests.testAction2';
                runDoubleChangeDetection(fixture);
                expect(permissions.checkPermissions).toHaveBeenCalledTimes(2);
                expect(permissions.checkPermissions).toHaveBeenCalledWith(MOCK_USER_ACTIONS.typeTests.testAction2.typePermissions);
                assertActionState(instance, { disabled: false, hidden: false });
            }),
        );

        it('leaves the control enabled if actionId is null',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.userAction = null;
                runDoubleChangeDetection(fixture);

                expect(permissions.checkPermissions).not.toHaveBeenCalled();
                assertActionState(instance, { disabled: false, hidden: false });
            }),
        );

        it('leaves the control enabled if actionId is an empty string',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.userAction = '';
                runDoubleChangeDetection(fixture);

                expect(permissions.checkPermissions).not.toHaveBeenCalled();
                assertActionState(instance, { disabled: false, hidden: false });
            }),
        );

    });

    describe('InstancePermissions checking', () => {

        it('checks instance permissions only', componentTest(
            () => TestComponent,
            assembleTestTemplate('[aaInstanceId]="instanceId"'),
            (fixture, instance) => {
                initServices();
                instance.userAction = 'instanceTests.instancePermsOnly';
                runDoubleChangeDetection(fixture);

                // Since we haven't provided an instanceId yet, the component should be disabled.
                expect(permissions.checkPermissions).not.toHaveBeenCalled();
                assertActionState(instance, { disabled: true, tooltip: TEST_TOOLTIP,  hidden: false });

                instance.instanceId = INSTANCE_ID_A;
                runDoubleChangeDetection(fixture);

                const expectedReqPermissions: RequiredInstancePermissions = {
                    ...MOCK_USER_ACTIONS.instanceTests.instancePermsOnly.instancePermissions,
                    instanceId: INSTANCE_ID_A,
                    nodeId: undefined,
                };
                expect(permissions.checkPermissions).toHaveBeenCalledTimes(1);
                expect(permissions.checkPermissions).toHaveBeenCalledWith([expectedReqPermissions]);
                assertActionState(instance, { disabled: false, hidden: false });
            }),
        );

        it('checks instance and type permissions', componentTest(
            () => TestComponent,
            assembleTestTemplate('[aaInstanceId]="instanceId" [aaNodeId]="nodeId"'),
            (fixture, instance) => {
                initServices();
                instance.userAction = 'instanceTests.typeAndInstancePerms';
                instance.instanceId = INSTANCE_ID_A;
                instance.nodeId = NODE_ID_A;
                runDoubleChangeDetection(fixture);

                const expectedReqPermissions: RequiredPermissions[] = [
                    ...MOCK_USER_ACTIONS.instanceTests.typeAndInstancePerms.typePermissions,
                    {
                        ...MOCK_USER_ACTIONS.instanceTests.typeAndInstancePerms.instancePermissions,
                        instanceId: INSTANCE_ID_A,
                        nodeId: NODE_ID_A,
                    },
                ];
                expect(permissions.checkPermissions).toHaveBeenCalledWith(expectedReqPermissions);
                assertActionState(instance, { disabled: false, hidden: false });
            }),
        );

        it('reacts to aaInstanceId changes', componentTest(
            () => TestComponent,
            assembleTestTemplate('[aaInstanceId]="instanceId" [aaNodeId]="nodeId"'),
            (fixture, instance) => {
                initServices();
                instance.userAction = 'instanceTests.instancePermsOnly';
                instance.instanceId = INSTANCE_ID_A;
                instance.nodeId = NODE_ID_A;
                runDoubleChangeDetection(fixture);

                const expectedReqPermissions: RequiredInstancePermissions = {
                    ...MOCK_USER_ACTIONS.instanceTests.instancePermsOnly.instancePermissions,
                    instanceId: INSTANCE_ID_A,
                    nodeId: NODE_ID_A,
                };
                expect(permissions.checkPermissions).toHaveBeenCalledWith([expectedReqPermissions]);
                assertActionState(instance, { disabled: false, hidden: false });

                instance.instanceId = INSTANCE_ID_B;
                permissions.mockPermissionsGranted(false);
                runDoubleChangeDetection(fixture);

                expectedReqPermissions.instanceId = INSTANCE_ID_B;
                expect(permissions.checkPermissions).toHaveBeenCalledWith([expectedReqPermissions]);
                assertActionState(instance, { disabled: true, tooltip: TEST_TOOLTIP, hidden: false });
            }),
        );

        it('reacts to aaNodeId changes', componentTest(
            () => TestComponent,
            assembleTestTemplate('[aaInstanceId]="instanceId" [aaNodeId]="nodeId"'),
            (fixture, instance) => {
                initServices();
                instance.userAction = 'instanceTests.instancePermsOnly';
                instance.instanceId = INSTANCE_ID_A;
                instance.nodeId = NODE_ID_A;
                runDoubleChangeDetection(fixture);

                const expectedReqPermissions: RequiredInstancePermissions = {
                    ...MOCK_USER_ACTIONS.instanceTests.instancePermsOnly.instancePermissions,
                    instanceId: INSTANCE_ID_A,
                    nodeId: NODE_ID_A,
                };
                expect(permissions.checkPermissions).toHaveBeenCalledWith([expectedReqPermissions]);
                assertActionState(instance, { disabled: false, hidden: false });

                instance.nodeId = NODE_ID_B;
                permissions.mockPermissionsGranted(false);
                runDoubleChangeDetection(fixture);

                expectedReqPermissions.nodeId = NODE_ID_B;
                expect(permissions.checkPermissions).toHaveBeenCalledWith([expectedReqPermissions]);
                assertActionState(instance, { disabled: true, tooltip: TEST_TOOLTIP, hidden: false });
            }),
        );

    });

    describe('hideElement', () => {

        it('hides the element if aaHideElement is true and permissions are denied and shows it again if permissions are granted', componentTest(
            () => TestComponent,
            assembleTestTemplate('[aaHideElement]="hideElement"'),
            (fixture, instance) => {
                initServices();
                permissions.mockPermissionsGranted(false);
                instance.hideElement = true;

                runDoubleChangeDetection(fixture);
                assertActionState(instance, { disabled: false, hidden: true });

                permissions.mockPermissionsGranted(true);
                tick();
                fixture.detectChanges();
                assertActionState(instance, { disabled: false, hidden: false });
            }),
        );

        it('hides the element if aaHideElement is set to the string "true"', componentTest(
            () => TestComponent,
            assembleTestTemplate('aaHideElement="true"'),
            (fixture, instance) => {
                initServices();
                permissions.mockPermissionsGranted(false);

                runDoubleChangeDetection(fixture);
                assertActionState(instance, { disabled: false, hidden: true });
            }),
        );

        it('does not hide the element if aaHideElement is set to the string "false"', componentTest(
            () => TestComponent,
            assembleTestTemplate('aaHideElement="false"'),
            (fixture, instance) => {
                initServices();
                permissions.mockPermissionsGranted(false);

                runDoubleChangeDetection(fixture);
                assertActionState(instance, { disabled: true, tooltip: TEST_TOOLTIP, hidden: false });
            }),
        );

        it('reacts to aaHideElement changes', componentTest(
            () => TestComponent,
            assembleTestTemplate('[aaHideElement]="hideElement"'),
            (fixture, instance) => {
                initServices();
                permissions.mockPermissionsGranted(false);
                instance.hideElement = true;

                // Element should be hidden
                runDoubleChangeDetection(fixture);
                assertActionState(instance, { disabled: false, hidden: true });

                // Element should be shown, but disabled.
                instance.hideElement = false;
                fixture.detectChanges();
                assertActionState(instance, { disabled: true, tooltip: TEST_TOOLTIP, hidden: false });

                // Element should be hidden and disabled (we don't re-enable the element if we transition from hidden -> disabled).
                instance.hideElement = true;
                fixture.detectChanges();
                assertActionState(instance, { disabled: true, tooltip: TEST_TOOLTIP, hidden: true });

                // Now the permissions are granted, so the element should be shown and enabled.
                permissions.mockPermissionsGranted(true);
                tick();
                fixture.detectChanges();
                assertActionState(instance, { disabled: false, hidden: false });
            }),
        );

    });

    describe('disabled', () => {

        it('disables the element if [disabled] is true and permissions are granted and enables it again when [disabled] becomes false', componentTest(
            () => TestComponent,
            assembleTestTemplate('[disabled]="disabled"'),
            (fixture, instance) => {
                initServices();
                permissions.mockPermissionsGranted(true);
                instance.disabled = true;

                runDoubleChangeDetection(fixture);
                assertActionState(instance, { disabled: true, hidden: false });
            }),
        );

        it('reacts to [disabled] changes', componentTest(
            () => TestComponent,
            assembleTestTemplate('[disabled]="disabled"'),
            (fixture, instance) => {
                initServices();
                permissions.mockPermissionsGranted(true);
                instance.disabled = false;

                // Element should be enabled
                runDoubleChangeDetection(fixture);
                assertActionState(instance, { disabled: false, hidden: false });

                // Element should be disabled.
                instance.disabled = true;
                fixture.detectChanges();
                assertActionState(instance, { disabled: true, hidden: false });

                // Element should be enabled again.
                instance.disabled = false;
                fixture.detectChanges();
                assertActionState(instance, { disabled: false, hidden: false });
            }),
        );

        it('a permission change to granted does not override [disabled]', componentTest(
            () => TestComponent,
            assembleTestTemplate('[disabled]="disabled"'),
            (fixture, instance) => {
                initServices();
                permissions.mockPermissionsGranted(false);
                instance.disabled = true;

                // Element should be disabled
                runDoubleChangeDetection(fixture);
                assertActionState(instance, { disabled: true, tooltip: TEST_TOOLTIP, hidden: false });

                // Now the permissions are granted, but the element should still be disabled.
                permissions.mockPermissionsGranted(true);
                tick();
                fixture.detectChanges();
                assertActionState(instance, { disabled: true, hidden: false });

                // Element should be enabled again.
                instance.disabled = false;
                fixture.detectChanges();
                assertActionState(instance, { disabled: false, hidden: false });
            }),
        );

    });

    describe('Tooltips', () => {

        beforeEach(() => {
            initServices();
            permissions.mockPermissionsGranted(false);
        });

        it('uses the default tooltip if none is provided',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.userAction = 'tooltipTests.noTooltip';
                runDoubleChangeDetection(fixture);

                expect(i18n.get).toHaveBeenCalledWith(DEFAULT_DISABLED_TOOLTIP, undefined);
                assertActionState(instance, { disabled: true, tooltip: DEFAULT_DISABLED_TOOLTIP, hidden: false });
            }),
        );

        it('tooltip params work, if provided',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.userAction = 'tooltipTests.tooltipParams';
                runDoubleChangeDetection(fixture);

                expect(i18n.get).toHaveBeenCalledWith(TEST_TOOLTIP, MOCK_USER_ACTIONS.tooltipTests.tooltipParams.disabledTooltip.params);
                assertActionState(instance, { disabled: true, tooltip: TEST_TOOLTIP, hidden: false });
            }),
        );

        it('restores the original tooltip',
            componentTest(() => TestComponent, (fixture, instance) => {
                const origTooltip = 'original Tooltip';
                instance.actionButtonElement.nativeElement.title = origTooltip;
                runDoubleChangeDetection(fixture);

                assertActionState(instance, { disabled: true, tooltip: TEST_TOOLTIP, hidden: false });

                permissions.mockPermissionsGranted(true);
                tick();
                fixture.detectChanges();

                assertActionState(instance, { disabled: false, tooltip: origTooltip, hidden: false });
            }),
        );

    });

    it('tooltips react to language changes', fakeAsync(() => {
        const i18nWithLangChange = new MockI18nWithLangChange();
        TestBed.overrideProvider(I18nService, { useValue: i18nWithLangChange });
        initServices();
        permissions.mockPermissionsGranted(false);

        const fixture = TestBed.createComponent(TestComponent);
        runDoubleChangeDetection(fixture);
        assertActionState(fixture.componentInstance, { disabled: true, tooltip: `en-${TEST_TOOLTIP}`, hidden: false });

        i18nWithLangChange.setLanguage('de');
        tick();
        fixture.detectChanges();
        assertActionState(fixture.componentInstance, { disabled: true, tooltip: `de-${TEST_TOOLTIP}`, hidden: false });
    }),
    );

    describe('ControlValueAccessor components', () => {

        // Unfortunately I was not able to find a way to spy on the input field's disabled property setter.
        // Angular seems to mess with them, so we cannot use spyOnProperty().
        // Thus we can only check if setDisabledState() was called, but not if the disabled property was not set.

        beforeEach(() => initServices());

        it('are enabled/disabled using ControlValueAccessor.setDisabledState()',
            componentTest(() => InputTestComponent, (fixture, instance) => {
                permissions.mockPermissionsGranted(false);
                runDoubleChangeDetection(fixture);

                assertInputFieldState(instance, { disabled: true, tooltip: TEST_TOOLTIP,  hidden: false });

                permissions.mockPermissionsGranted(true);
                tick();
                fixture.detectChanges();
                assertInputFieldState(instance, { disabled: false, hidden: false });
            }),
        );

    });

});
