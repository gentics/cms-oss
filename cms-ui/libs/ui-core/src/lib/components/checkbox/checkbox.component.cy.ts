import { NO_ERRORS_SCHEMA } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CHECKBOX_STATE_INDETERMINATE, CheckboxState } from '@gentics/ui-core';
import { createOutputSpy, mount, MountResponse } from 'cypress/angular';
import { CheckboxComponent } from './checkbox.component';

describe('CheckboxComponent', () => {
    function checkInputValue(instance: CheckboxComponent, value: CheckboxState): void {
        cy.get('input', { log: false }).then($el => {
            const compVal = instance.value;
            Cypress.log({
                type: 'parent',
                name: 'checkValue',
                message: value,
                $el: $el,
                consoleProps: () => ({
                    'Component Value': compVal,
                }),
            });

            const input = $el.get(0);
            // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
            expect(input.getAttribute('state')).to.equal(`${value}`);
            if (typeof value === 'boolean') {
                expect(input.checked).to.equal(value);
                expect(input.indeterminate).to.equal(false);
            } else {
                expect(input.indeterminate).to.equal(true);
            }
        });
    }

    function detectChangesAndWait(ref: MountResponse<any>) {
        return cy.wrap(null, { log: false }).then(() => {
            ref.fixture.detectChanges();

            return ref.fixture.whenRenderingDone();
        });
    }

    function setInputValue(ref: MountResponse<any>, value: CheckboxState) {
        cy.wrap(null, { log: false }).then(() => {
            Cypress.log({
                type: 'parent',
                name: 'setValue',
                message: value,
                $el: Cypress.$(ref.fixture.nativeElement),
            });

            ref.component.value = value;

            return detectChangesAndWait(ref);
        });
    }

    function writeControlValue(ref: MountResponse<any>, control: FormControl<CheckboxState>, value: CheckboxState) {
        cy.wrap(null, { log: false }).then(() => {
            Cypress.log({
                type: 'parent',
                name: 'writeValue',
                message: value,
                $el: Cypress.$(ref.fixture.nativeElement),
            });

            control.setValue(value);

            return detectChangesAndWait(ref);
        });
    }

    function resetSpy(spyName: string) {
        cy.get<Cypress.Agent<sinon.SinonSpy>>(`@${spyName}`)
            .then(spy => {
                spy.resetHistory();
            });
    }

    function calledOnceWithReset(spyName: string, value: any) {
        cy.get<Cypress.Agent<sinon.SinonSpy>>(`@${spyName}`)
            .then(spy => {
                expect(spy).to.have.been.calledOnceWith(value);
                spy.resetHistory();
            });
    }

    function toggleStateWithClick(ref: MountResponse<any>) {
        cy.get('label')
            .click();
        return detectChangesAndWait(ref);
    }

    const ALIAS_CHANGE_SPY = 'valueChangeSpy';

    it('should toggle between the boolean states correctly', () => {
        mount(CheckboxComponent, {
            componentProperties: {
                valueChange: createOutputSpy(ALIAS_CHANGE_SPY),
            },
            autoDetectChanges: true,
            schemas: [NO_ERRORS_SCHEMA],
        }).then(ref => {
            return ref.fixture.whenStable()
                .then(() => ref);
        }).then(ref => {
            const cb = ref.fixture.debugElement.childNodes[0].componentInstance;

            checkInputValue(cb, false);

            toggleStateWithClick(ref);
            calledOnceWithReset(ALIAS_CHANGE_SPY, true);
            checkInputValue(cb, true);

            toggleStateWithClick(ref);
            calledOnceWithReset(ALIAS_CHANGE_SPY, false);
            checkInputValue(cb, false);

            toggleStateWithClick(ref);
            calledOnceWithReset(ALIAS_CHANGE_SPY, true);
            checkInputValue(cb, true);
        });
    });

    it('should switch to "true" as next value when in "indeterminate" state', () => {
        const spy = createOutputSpy(ALIAS_CHANGE_SPY);
        mount('<gtx-checkbox [value]="value" />', {
            componentProperties: {
                value: CHECKBOX_STATE_INDETERMINATE,
            },
            schemas: [NO_ERRORS_SCHEMA],
            declarations: [CheckboxComponent],
        }).then(ref => {
            return ref.fixture.whenStable()
                .then(() => ref);
        }).then(ref => {
            const cb: CheckboxComponent = ref.fixture.debugElement.childNodes[0].componentInstance;
            cb.valueChange = spy;

            // Should be initially indeterminate
            checkInputValue(cb, CHECKBOX_STATE_INDETERMINATE);
            toggleStateWithClick(ref);
            checkInputValue(cb, true);
            calledOnceWithReset(ALIAS_CHANGE_SPY, true);
        });
    });

    it('should have the correct state with the [value] binding', () => {
        mount('<gtx-checkbox [value]="value" />', {
            componentProperties: {
                value: false,
            },
            schemas: [NO_ERRORS_SCHEMA],
            declarations: [CheckboxComponent],
        }).then(ref => {
            return ref.fixture.whenStable()
                .then(() => ref);
        }).then(ref => {
            const cb = ref.fixture.debugElement.childNodes[0].componentInstance;

            // Should be initially false
            checkInputValue(cb, false);

            // Set it to true
            setInputValue(ref, true);
            checkInputValue(cb, true);

            // Set it to false
            setInputValue(ref, false);
            checkInputValue(cb, false);

            // Set it to indeterminate
            setInputValue(ref, CHECKBOX_STATE_INDETERMINATE);
            checkInputValue(cb, CHECKBOX_STATE_INDETERMINATE);
        });
    });

    it('should have the correct state with the [(ngModel)] binding', () => {
        mount('<gtx-checkbox [(ngModel)]="value" />', {
            componentProperties: {
                value: false,
            },
            schemas: [NO_ERRORS_SCHEMA],
            declarations: [CheckboxComponent],
            imports: [
                ReactiveFormsModule,
                FormsModule,
            ],
        }).then(ref => {
            return ref.fixture.whenStable()
                .then(() => ref);
        }).then(ref => {
            const cb = ref.fixture.debugElement.childNodes[0].componentInstance;

            // Should be initially false
            checkInputValue(cb, false);

            // Set it to true
            setInputValue(ref, true);
            checkInputValue(cb, true);

            // Set it to false
            setInputValue(ref, false);
            checkInputValue(cb, false);

            // Set it to indeterminate
            setInputValue(ref, CHECKBOX_STATE_INDETERMINATE);
            checkInputValue(cb, CHECKBOX_STATE_INDETERMINATE);
        });
    });

    it('should have the correct state with the formControl binding', () => {
        const control = new FormControl<CheckboxState>(false);

        mount('<gtx-checkbox [formControl]="control" />', {
            componentProperties: {
                control: control,
            },
            schemas: [NO_ERRORS_SCHEMA],
            declarations: [CheckboxComponent],
            imports: [
                ReactiveFormsModule,
                FormsModule,
            ],
        }).then(ref => {
            return ref.fixture.whenStable()
                .then(() => ref);
        }).then(ref => {
            const cb = ref.fixture.debugElement.childNodes[0].componentInstance;

            // Should be initially false
            checkInputValue(cb, false);

            // Set it to true
            writeControlValue(ref, control, true);
            checkInputValue(cb, true);

            // Set it to false
            writeControlValue(ref, control, false);
            checkInputValue(cb, false);

            // Set it to indeterminate
            writeControlValue(ref, control, CHECKBOX_STATE_INDETERMINATE);
            checkInputValue(cb, CHECKBOX_STATE_INDETERMINATE);
        });
    });

    it('should not update the state when marked as "pure"', () => {
        mount(CheckboxComponent, {
            componentProperties: {
                valueChange: createOutputSpy(ALIAS_CHANGE_SPY),
                pure: true,
            },
            autoDetectChanges: true,
            schemas: [NO_ERRORS_SCHEMA],
        }).then(ref => {
            return ref.fixture.whenStable()
                .then(() => ref);
        }).then(ref => {
            const cb = ref.fixture.debugElement.childNodes[0].componentInstance;

            checkInputValue(cb, false);

            toggleStateWithClick(ref);
            calledOnceWithReset(ALIAS_CHANGE_SPY, true);
            checkInputValue(cb, false);
        });
    });

    it('should not update the state or trigger a valueChange event when disabled', () => {
        mount(CheckboxComponent, {
            componentProperties: {
                valueChange: createOutputSpy(ALIAS_CHANGE_SPY),
                disabled: true,
            },
            autoDetectChanges: true,
            schemas: [NO_ERRORS_SCHEMA],
        }).then(ref => {
            return ref.fixture.whenStable()
                .then(() => ref);
        }).then(ref => {
            const cb = ref.fixture.debugElement.childNodes[0].componentInstance;

            checkInputValue(cb, false);

            toggleStateWithClick(ref);
            cy.get(`@${ALIAS_CHANGE_SPY}`)
                .should('not.have.been.called');
            checkInputValue(cb, false);
        });
    });
});
