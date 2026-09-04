import { NO_ERRORS_SCHEMA, provideZoneChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CmsComponentsModule } from '@gentics/cms-components';
import { FormGridModule } from '../../form-grid.module';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { FormGridComponent } from './form-grid.component';
import { provideTranslateService } from '@ngx-translate/core';
import { FormGridEditMode, FormGridViewMode } from '../../models';

describe('FormGridComponent', () => {
    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                FormsModule,
                ReactiveFormsModule,
                GenticsUICoreModule.forRoot(),
                CmsComponentsModule,
                FormGridModule,
            ],
            providers: [
                provideTranslateService(),
                provideZoneChangeDetection(),
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });
    });

    it('should render', () => {
        cy.mount(FormGridComponent, {
            componentProperties: {
                config: {
                    type: 'generic',
                    pluginName: 'forms',
                    controls: {},
                    flows: [
                        {
                            id: 'default',
                            labelI18n: {
                                en: 'default',
                            },
                            nameTranslationKey: 'foobar',
                            steps: [],
                        },
                    ],
                },
                mode: FormGridEditMode.FULL,
                languages: ['en'],
                schema: {
                    key: '',
                    version: '',
                    properties: {},
                },
                uiSchema: {
                    key: '',
                    version: '',
                    pages: [
                        {
                            pagename: {
                                en: 'foo bar',
                            },
                            elements: [],
                        },
                    ],
                },
                pageIndex: 0,
                viewMode: FormGridViewMode.EDITOR,
                flowId: 'default',
            },
        }).detectChanges();

        cy.get('.editor-shell')
            .should('be.visible');
    });
});
