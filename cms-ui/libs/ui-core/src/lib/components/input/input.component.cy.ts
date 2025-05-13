import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { InputComponent } from './input.component';

// NOTE: The `cy.wrap(null)` calls are there, to make assertions/executions happen in the correct
// order, as `cy` calls are actually queued and with direct `expect` calls, these would be out of
// order and simply not work correctly.

describe('InputComponent', () => {

    const ATTR_ID = 'id';
    const ATTR_FOR_ID = 'for';

    const QUERY_INPUT = 'input.input-element';
    const QUERY_LABEL = 'label';

    const OUTPUT_CHANGE = '@changeSpy';
    const OUTPUT_VALUE_CHANGE = '@valueChangeSpy';

    const INITIAL_VALUE = 'testValue';

    it('should trigger the change accordingly', () => {
        const WRITE_VALUE = 'Hello World';

        cy.mount(InputComponent, {
            autoSpyOutputs: true,
            imports: [FormsModule, ReactiveFormsModule],
            schemas: [NO_ERRORS_SCHEMA],
        });

        cy.get(QUERY_INPUT)
            .type(WRITE_VALUE);

        cy.get(OUTPUT_CHANGE)
            .should('have.been.calledWith', WRITE_VALUE);
        cy.get(OUTPUT_VALUE_CHANGE)
            .should('have.been.calledWith', WRITE_VALUE);
    });

    it('should not display the label element when no label has been provided', () => {
        cy.mount(InputComponent, {
            componentProperties: {},
            imports: [FormsModule, ReactiveFormsModule],
            schemas: [NO_ERRORS_SCHEMA],
        });

        cy.get(QUERY_LABEL)
            .should('not.exist');
    });

    it('should display the label', () => {
        const LABEL_VALUE = 'test label';

        cy.mount(InputComponent, {
            componentProperties: {
                label: LABEL_VALUE,
            },
            imports: [FormsModule, ReactiveFormsModule],
            schemas: [NO_ERRORS_SCHEMA],
        });

        cy.get(QUERY_LABEL)
            .should('exist')
            .should('have.text', LABEL_VALUE);
    });

    it('should bind the id on input and label', () => {
        const ID_VALUE = 'myInput';

        cy.mount(InputComponent, {
            componentProperties: {
                id: ID_VALUE,
                label: 'something',
            },
            imports: [FormsModule, ReactiveFormsModule],
            schemas: [NO_ERRORS_SCHEMA],
        }).then(async mounted => {
            mounted.fixture.detectChanges();
            await mounted.fixture.whenRenderingDone();

            cy.get(QUERY_INPUT)
                .then($input => expect($input[0].getAttribute(ATTR_ID)).to.equal(ID_VALUE));

            cy.get(QUERY_LABEL)
                .then($label => expect($label[0].getAttribute(ATTR_FOR_ID)).to.equal(ID_VALUE));
        });

    });

    it('should apply default attributes', () => {
        cy.mount(InputComponent, {
            componentProperties: {},
            imports: [FormsModule, ReactiveFormsModule],
            schemas: [NO_ERRORS_SCHEMA],
        }).then(async mounted => {
            mounted.fixture.detectChanges();
            await mounted.fixture.whenRenderingDone();

            cy.get<HTMLInputElement>(QUERY_INPUT).then($input => {
                expect($input[0].autocomplete).to.equal('');
                expect($input[0].autofocus).to.equal(false);
                expect($input[0].disabled).to.equal(false);
                expect($input[0].readOnly).to.equal(false);
                expect($input[0].required).to.equal(false);
                expect($input[0].type).to.equal('text');
                expect($input[0].value).to.equal('');

                const notDefinedAttributes = [
                    'id',
                    'max',
                    'min',
                    'maxLength',
                    'name',
                    'pattern',
                    'placeholder',
                    'step',
                ];

                for (const attr of notDefinedAttributes) {
                    expect($input[0].hasAttribute(attr)).to.equal(false);
                }
            });
        });
    });

    it('should apply native attributes correctly', () => {
        const AUTO_COMPLETE_VALUE = 'on';
        const DISABLED_VALUE = true;
        const MAX_VALUE = 100;
        const MIN_VALUE = 5;
        const MAX_LENGTH_VALUE = 25;
        const NAME_VALUE = 'testName';
        const PATTERN_VALUE = '\\w*';
        const PLACEHOLDER_VALUE = 'testPlaceholder';
        const READONLY_VALUE = true;
        const REQUIRED_VALUE = true;
        const STEP_VALUE = 5;
        const TYPE_VALUE = 'text';
        const VALUE = 'testValue';

        cy.mount(InputComponent, {
            componentProperties: {
                autocomplete: AUTO_COMPLETE_VALUE,
                disabled: DISABLED_VALUE,
                max: MAX_VALUE,
                min: MIN_VALUE,
                maxlength: MAX_LENGTH_VALUE,
                name: NAME_VALUE,
                pattern: PATTERN_VALUE,
                placeholder: PLACEHOLDER_VALUE,
                readonly: READONLY_VALUE,
                required: REQUIRED_VALUE,
                step: STEP_VALUE,
                type: TYPE_VALUE,
                value: VALUE,
            },
            imports: [FormsModule, ReactiveFormsModule],
            schemas: [NO_ERRORS_SCHEMA],
        }).then(async mounted => {
            mounted.fixture.detectChanges();
            await mounted.fixture.whenRenderingDone();

            cy.get<HTMLInputElement>(QUERY_INPUT).then($input => {
                expect($input[0].autocomplete).to.equal(AUTO_COMPLETE_VALUE);
                expect($input[0].disabled).to.equal(DISABLED_VALUE);
                expect(Number($input[0].max)).to.equal(MAX_VALUE);
                expect(Number($input[0].min)).to.equal(MIN_VALUE);
                expect($input[0].maxLength).to.equal(MAX_LENGTH_VALUE);
                expect($input[0].name).to.equal(NAME_VALUE);
                expect($input[0].pattern).to.equal(PATTERN_VALUE);
                expect($input[0].placeholder).to.equal(PLACEHOLDER_VALUE);
                expect($input[0].readOnly).to.equal(READONLY_VALUE);
                expect($input[0].required).to.equal(REQUIRED_VALUE);
                expect(Number($input[0].step)).to.equal(STEP_VALUE);
                expect($input[0].type).to.equal(TYPE_VALUE);
                expect($input[0].value).to.equal(VALUE);
            });
        });
    });

    it('should emit a number value when the type is number', () => {
        const VALUE_START_VALUE = 10;
        const VALUE_ADD = '5';
        const VALUE_CHANGE_VALUE = 105;

        cy.mount(InputComponent, {
            componentProperties: {
                value: VALUE_START_VALUE,
                type: 'number',
            },
            autoSpyOutputs: true,
            imports: [FormsModule, ReactiveFormsModule],
            schemas: [NO_ERRORS_SCHEMA],
        }).then(async mounted => {
            mounted.fixture.detectChanges();
            await mounted.fixture.whenRenderingDone();

            cy.get(QUERY_INPUT)
                .should('have.value', VALUE_START_VALUE)
                .type(VALUE_ADD);

            cy.get(OUTPUT_VALUE_CHANGE)
                .should('have.been.calledWith', VALUE_CHANGE_VALUE);
        });
    });

    it('should work with regular two way bindings', () => {
        @Component({
            template: '<gtx-input [(ngModel)]="value" />',
            standalone: false,
        })
        class Test2WayBindingComponent {
            public value = INITIAL_VALUE;
        }

        const INBETWEEN_VALUE = 'value from parent';
        const NEW_VALUE = 'hello world!';

        cy.mount(Test2WayBindingComponent, {
            declarations: [InputComponent],
            imports: [FormsModule, ReactiveFormsModule],
            schemas: [NO_ERRORS_SCHEMA],
        }).then(async mounted => {
            mounted.fixture.detectChanges();
            await mounted.fixture.whenRenderingDone();

            cy.get(QUERY_INPUT)
                .should('have.value', INITIAL_VALUE);

            cy.wrap(null).then(async () => {
                mounted.component.value = INBETWEEN_VALUE;
                mounted.fixture.detectChanges();
                await mounted.fixture.whenRenderingDone();
            });

            cy.get(QUERY_INPUT)
                .should('have.value', INBETWEEN_VALUE);

            cy.get(QUERY_INPUT)
                .clear()
                .type(NEW_VALUE);

            cy.wrap(null).then(() => {
                expect(mounted.component.value).to.equal(NEW_VALUE);
            });
        });
    });

    describe('Form Bindings', () => {
        @Component({
            template: '<gtx-input [formControl]="control" />',
            standalone: false,
        })
        class TestFormComponent {
            public control = new FormControl(INITIAL_VALUE);
        }

        it('should propagate the value correctly via form bindings', () => {
            cy.mount(TestFormComponent, {
                declarations: [InputComponent],
                imports: [FormsModule, ReactiveFormsModule],
                schemas: [NO_ERRORS_SCHEMA],
            }).then(async mounted => {
                mounted.fixture.detectChanges();
                await mounted.fixture.whenRenderingDone();

                const TEMPORARY_VALUE = 'something inbetween?';
                const NEW_VALUE = 'hello world!';

                expect(mounted.component.control.value).to.equal(INITIAL_VALUE);
                expect(mounted.component.control.touched).to.equal(false);
                expect(mounted.component.control.dirty).to.equal(false);

                mounted.component.control.setValue(TEMPORARY_VALUE);

                mounted.fixture.detectChanges();
                await mounted.fixture.whenRenderingDone();

                cy.get(QUERY_INPUT)
                    .should('have.value', TEMPORARY_VALUE);

                cy.get(QUERY_INPUT)
                    .clear()
                    .type(NEW_VALUE);

                cy.wrap(null).then(async () => {
                    mounted.fixture.detectChanges();
                    await mounted.fixture.whenRenderingDone();

                    expect(mounted.component.control.value).to.equal(NEW_VALUE);
                    expect(mounted.component.control.touched).to.equal(true);
                    expect(mounted.component.control.dirty).to.equal(true);
                });
            });
        });

        it('should be possible to dis-/enable the input via form bindings', () => {
            cy.mount(TestFormComponent, {
                declarations: [InputComponent],
                imports: [FormsModule, ReactiveFormsModule],
                schemas: [NO_ERRORS_SCHEMA],
            }).then(async mounted => {
                mounted.fixture.detectChanges();
                await mounted.fixture.whenRenderingDone();

                expect(mounted.component.control.disabled).to.equal(false);
                cy.get<HTMLInputElement>(QUERY_INPUT)
                    .should('not.be.disabled');

                cy.wrap(null).then(async () => {
                    mounted.component.control.disable();
                    mounted.fixture.detectChanges();
                    await mounted.fixture.whenRenderingDone();

                    expect(mounted.component.control.disabled).to.equal(true);
                });

                cy.get(QUERY_INPUT)
                    .should('be.disabled');

                cy.wrap(null).then(async () => {
                    mounted.component.control.enable();
                    mounted.fixture.detectChanges();
                    await mounted.fixture.whenRenderingDone();

                    expect(mounted.component.control.disabled).to.equal(false);
                });

                cy.get(QUERY_INPUT)
                    .should('not.be.disabled');
            });
        });
    });
});
