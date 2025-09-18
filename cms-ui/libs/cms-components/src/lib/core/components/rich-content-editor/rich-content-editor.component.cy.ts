import { Component, EventEmitter, Input, NO_ERRORS_SCHEMA, Output, ViewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { GCMSRestClientModule, GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { mockPipe } from '@gentics/ui-core/testing/mock-pipe';
import { mount } from 'cypress/angular';
import { ATTR_CONTENT_TYPE, ATTR_LINK_TYPE, ATTR_TARGET, ATTR_URL, RichContentLinkType, RichContentType } from '../../../common/models';
import { normalizeWhitespaces } from '../../../common/utils/rich-content';
import { ValuesPipe } from '../../pipes';
import { GCMS_UI_SERVICES_PROVIDER } from '../../providers/gcms-ui-services/gcms-ui-services';
import { RichContentLinkPropertiesComponent } from '../rich-content-link-properties/rich-content-link-properties.component';
import { RichContentModal } from '../rich-content-modal/rich-content-modal.component';
import { RichContentEditorComponent } from './rich-content-editor.component';

@Component({
    template: `
        <gtx-rich-content-editor
            [value]="value"
            (valueChange)="valueChange.emit($event)"
        />
        <gtx-overlay-host />
    `,
    standalone: false,
})
class TestComponent {

    @ViewChild(RichContentEditorComponent, { static: true })
    public editor: RichContentEditorComponent;

    @Input()
    public value;

    @Output()
    public valueChange = new EventEmitter<any>();
}

describe('RichContentEditorComponent', () => {

    it('should display rich-content links correctly', () => {
        const VALUE = 'Hello World! {{LINK|URL:https&col;//www.gentics.com|Click me|_blank}} for more info!';

        mount(RichContentEditorComponent, {
            componentProperties: {
                value: VALUE,
            },
            schemas: [NO_ERRORS_SCHEMA],
            imports: [GenticsUICoreModule.forRoot()],
        }).then(async mounted => {
            mounted.fixture.detectChanges();
            await mounted.fixture.whenRenderingDone();

            cy.get('.content-wrapper .text-container')
                .then($container => {
                    expect(normalizeWhitespaces($container.text())).to.equal('Hello World! Click me for more info!');
                    return $container;
                })
                .find(`[${ATTR_CONTENT_TYPE}="${RichContentType.LINK}"]`)
                .should('exist')
                .then($link => {
                    expect($link.attr(ATTR_LINK_TYPE)).to.equal(RichContentLinkType.URL);
                    expect($link.attr(ATTR_URL)).to.equal('https://www.gentics.com');
                    expect($link.attr(ATTR_TARGET)).to.equal('_blank');
                    expect($link.text()).to.equal('Click me');
                });
        });
    });

    it('should trigger changes for simple text correctly', () => {
        const VALUE = 'Hello World!';

        mount(RichContentEditorComponent, {
            componentProperties: {
                value: '',
            },
            autoSpyOutputs: true,
            schemas: [NO_ERRORS_SCHEMA],
            imports: [GenticsUICoreModule.forRoot()],
        }).then(async mounted => {
            mounted.fixture.detectChanges();
            await mounted.fixture.whenRenderingDone();

            cy.get('.content-wrapper .text-container')
                .as('container')
                .type(VALUE)
                .blur();

            cy.get('@valueChangeSpy')
                .should('have.been.calledOnceWith', VALUE);

            cy.get('.content-wrapper .text-container')
                .then($container => {
                    expect(normalizeWhitespaces($container.text())).to.equal('Hello World!');
                    return $container;
                });
        });
    });

    it('should be able to create a new link and emit a properly encoded valueChange', () => {
        const FINAL_VALUE = 'Hello World {{LINK|URL:https&col;//www.example.com|Example|_top}}';

        mount(TestComponent, {
            componentProperties: {
                value: 'Hello World',
            },
            autoSpyOutputs: true,
            schemas: [NO_ERRORS_SCHEMA],
            declarations: [
                RichContentEditorComponent,
                RichContentModal,
                RichContentLinkPropertiesComponent,
                ValuesPipe,
                mockPipe('i18n'),
            ],
            providers: [
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                { provide: GCMS_UI_SERVICES_PROVIDER, useValue: null },
            ],
            imports: [
                GenticsUICoreModule.forRoot(),
                GCMSRestClientModule,
                FormsModule,
                ReactiveFormsModule,
            ],
        }).then(async mounted => {
            mounted.fixture.detectChanges();
            await mounted.fixture.whenRenderingDone();

            cy.get('.content-wrapper .text-container')
                .as('container')
                .type('{end} ');
            cy.get('.content-wrapper .content-toolbar [action="add"] button')
                .click();

            cy.get('gtx-rich-content-modal')
                .should('exist')
                .as('modal');

            // Enter link properties

            cy.get('@modal')
                .find('[property-name="displayText"] input')
                .type('{selectAll}Example');

            cy.get('@modal')
                .find('[property-name="linkType"] .select-input')
                .click({ force: true });
            cy.get('gtx-dropdown-content-wrapper .select-options .select-option')
                .then($options => Cypress.$($options.get(2)))
                .click({ force: true });

            cy.get('@modal')
                .find('[property-name="url"] input')
                .type('https://www.example.com');

            // Create the link which triggers a change

            cy.get('@modal')
                .find('.modal-footer [action="confirm"]')
                .click({ force: true });

            // Validate changes

            cy.get('@container')
                .then($container => {
                    expect(normalizeWhitespaces($container.text())).to.equal('Hello World Example');
                    return $container;
                })
                .find(`[${ATTR_CONTENT_TYPE}="${RichContentType.LINK}"]`)
                .should('exist')
                .then($link => {
                    expect($link.attr(ATTR_LINK_TYPE)).to.equal(RichContentLinkType.URL);
                    expect($link.attr(ATTR_URL)).to.equal('https://www.example.com');
                    expect($link.attr(ATTR_TARGET)).to.equal('_top');
                    expect($link.text()).to.equal('Example');
                });
            cy.get('@valueChangeSpy')
                .should('have.been.calledWith', FINAL_VALUE)
        });
    });

    it('should be possible to update the link url correctly', () => {
        const INITIAL_VALUE = 'Hello World {{LINK|URL:https&col;//www.example.com|Example|_top}}';
        const FINAL_VALUE = 'Hello World {{LINK|URL:https&col;//www.gentics.com|Example|_top}}';

        mount(TestComponent, {
            componentProperties: {
                value: INITIAL_VALUE,
            },
            autoSpyOutputs: true,
            schemas: [NO_ERRORS_SCHEMA],
            declarations: [
                RichContentEditorComponent,
                RichContentModal,
                RichContentLinkPropertiesComponent,
                ValuesPipe,
                mockPipe('i18n'),
            ],
            providers: [
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                { provide: GCMS_UI_SERVICES_PROVIDER, useValue: null },
            ],
            imports: [
                GenticsUICoreModule.forRoot(),
                GCMSRestClientModule,
                FormsModule,
                ReactiveFormsModule,
            ],
        }).then(async mounted => {
            mounted.fixture.detectChanges();
            await mounted.fixture.whenRenderingDone();

            // Select the first link
            cy.get('.content-wrapper .text-container')
                .as('container')
                .then($container => {
                    expect(normalizeWhitespaces($container.text())).to.equal('Hello World Example');
                    $container[0].focus();

                    // Validate initial link
                    const $link = Cypress.$($container.children()[0]);
                    expect($link.attr(ATTR_LINK_TYPE)).to.equal(RichContentLinkType.URL);
                    expect($link.attr(ATTR_URL)).to.equal('https://www.example.com');
                    expect($link.attr(ATTR_TARGET)).to.equal('_top');
                    expect($link.text()).to.equal('Example');

                    // Set range to be inside of the link, so the edit button is enabled
                    const range = document.createRange();
                    range.setStart($container.children()[0], 1);
                    range.collapse(true);
                    document.getSelection().removeAllRanges();
                    document.getSelection().addRange(range);
                });
            cy.get('.content-wrapper .content-toolbar [action="edit"] button')
                .click();

            cy.get('gtx-rich-content-modal')
                .should('exist')
                .as('modal');

            // Update the URL

            cy.get('@modal')
                .find('[property-name="url"] input')
                .type('{selectAll}https://www.gentics.com');

            // Create the link which triggers a change

            cy.get('@modal')
                .find('.modal-footer [action="confirm"]')
                .click({ force: true });

            // Validate changes

            cy.get('@container')
                .then($container => {
                    expect(normalizeWhitespaces($container.text())).to.equal('Hello World Example');
                    return $container;
                })
                .find(`[${ATTR_CONTENT_TYPE}="${RichContentType.LINK}"]`)
                .should('exist')
                .then($link => {
                    expect($link.attr(ATTR_LINK_TYPE)).to.equal(RichContentLinkType.URL);
                    expect($link.attr(ATTR_URL)).to.equal('https://www.gentics.com');
                    expect($link.attr(ATTR_TARGET)).to.equal('_top');
                    expect($link.text()).to.equal('Example');
                });
            cy.get('@valueChangeSpy')
                .should('have.been.calledWith', FINAL_VALUE)
        });
    });

    it('should be possible to remove the link from a text', () => {
        const INITIAL_VALUE = 'Hello World {{LINK|URL:https&col;//www.example.com|Example|_top}}';

        mount(TestComponent, {
            componentProperties: {
                value: INITIAL_VALUE,
            },
            autoSpyOutputs: true,
            schemas: [NO_ERRORS_SCHEMA],
            declarations: [
                RichContentEditorComponent,
                RichContentModal,
                RichContentLinkPropertiesComponent,
                ValuesPipe,
                mockPipe('i18n'),
            ],
            providers: [
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                { provide: GCMS_UI_SERVICES_PROVIDER, useValue: null },
            ],
            imports: [
                GenticsUICoreModule.forRoot(),
                GCMSRestClientModule,
                FormsModule,
                ReactiveFormsModule,
            ],
        }).then(async mounted => {
            mounted.fixture.detectChanges();
            await mounted.fixture.whenRenderingDone();

            cy.get('.content-wrapper .text-container')
                .as('container')
                .then($container => {
                    expect(normalizeWhitespaces($container.text())).to.equal('Hello World Example');
                    $container[0].focus();

                    // Validate initial link
                    const $link = Cypress.$($container.children()[0]);
                    expect($link.attr(ATTR_LINK_TYPE)).to.equal(RichContentLinkType.URL);
                    expect($link.attr(ATTR_URL)).to.equal('https://www.example.com');
                    expect($link.attr(ATTR_TARGET)).to.equal('_top');
                    expect($link.text()).to.equal('Example');

                    // Set range to be inside of the link, so the edit button is enabled
                    const range = document.createRange();
                    range.setStart($container.children()[0], 1);
                    range.collapse(true);
                    document.getSelection().removeAllRanges();
                    document.getSelection().addRange(range);
                });

            cy.get('.content-wrapper .content-toolbar [action="delete"] button')
                .click();

            // Validate changes

            cy.get('@container')
                .then($container => {
                    expect(normalizeWhitespaces($container.text())).to.equal('Hello World Example');
                    return $container;
                })
                .find(`[${ATTR_CONTENT_TYPE}="${RichContentType.LINK}"]`)
                .should('not.exist');

            cy.get('@valueChangeSpy')
                .should('have.been.calledWith', 'Hello World Example');
        });
    });
});
