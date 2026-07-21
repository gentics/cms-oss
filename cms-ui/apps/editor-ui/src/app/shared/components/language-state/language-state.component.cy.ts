import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { UIMode } from '../../../common/models';
import { LanguageStateComponent } from './language-state.component';

const LANG_DE = {
    code: 'de',
    id: 1,
    name: 'Deutsch',
    globalId: '123',
};

describe('LanguageStateComponent', () => {

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                FormsModule,
                ReactiveFormsModule,
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });
    });

    it('should render', () => {
        cy.mount(LanguageStateComponent, {
            componentProperties: {
                language: LANG_DE,
                activeLanguage: LANG_DE,
                mode: UIMode.EDIT,
                state: {
                    available: true,
                    deleted: false,
                    inherited: false,
                    localized: false,
                    modified: true,
                    planned: true,
                    published: false,
                    queued: true,
                    staged: true,
                },
            },
        }).detectChanges();

        cy.get('.language-button')
            .should('be.visible');
    });
});
