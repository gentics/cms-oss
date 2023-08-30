import { CopyValueComponent } from './copy-value.component';

describe('CopyValueComponent', () => {
    it('should display the value', () => {
        const exampleValue = 'hello world 123';
        cy.mount(CopyValueComponent, {
            componentProperties: {
                value: exampleValue,
            },
        }).get('.content').contains(exampleValue);
    })
});
