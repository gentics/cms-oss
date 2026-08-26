import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Language } from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import '@gentics/ui-core/cypress';
import { LanguagePropertiesComponent } from './language-properties.component';

const NAME_INPUT = 'gtx-input[formControlName="name"] input';
const CODE_INPUT = 'gtx-input[formControlName="code"] input';

@Pipe({
    name: 'gtxI18n',
    standalone: false,
})
class MockI18nPipe implements PipeTransform {
    transform(query: string, ...args: any[]): string {
        return query;
    }
}

describe('LanguagePropertiesComponent', () => {

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                FormsModule,
                ReactiveFormsModule,
                GenticsUICoreModule.forRoot(),
            ],
            declarations: [MockI18nPipe],
            schemas: [NO_ERRORS_SCHEMA],
        });
    });

    it('should bind the initial `name`/`code` value to the form inputs', () => {
        const INITIAL_VALUE: Language = {
            name: 'German',
            code: 'de',
        } as Language;

        cy.mount(LanguagePropertiesComponent, {
            componentProperties: {
                value: INITIAL_VALUE,
            },
        });

        cy.get(NAME_INPUT).should('have.value', 'German');
        cy.get(CODE_INPUT).should('have.value', 'de');
    });

    it('should update the form value when the `name`/`code` inputs change', () => {
        cy.mount(LanguagePropertiesComponent, {
            componentProperties: {
                value: { name: 'German', code: 'de' } as Language,
            },
            autoSpyOutputs: true,
        }).then(() => {
            cy.get(NAME_INPUT).clear();
            cy.get(NAME_INPUT).type('English');
            cy.get(CODE_INPUT).clear();
            cy.get(CODE_INPUT).type('en');
        });

        cy.get('@valueChangeSpy').should('have.been.calledWith', Cypress.sinon.match({
            name: 'English',
            code: 'en',
        }));
    });

});
