import { NO_ERRORS_SCHEMA, provideZoneChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AccordionComponent } from './accordion.component';

describe(AccordionComponent.name, () => {
    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideZoneChangeDetection(),
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });
    });

    it('renders', () => {
        cy.mount(AccordionComponent, {
            componentProperties: {
                text: '',
                open: false,
                triggerToggle: false,
            },
        });
    });
});
