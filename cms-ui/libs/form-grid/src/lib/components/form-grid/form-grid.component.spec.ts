import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nNotificationService, I18nService } from '@gentics/cms-components';
import { FormElement, FormSchema, FormTypeConfiguration, FormUISchema } from '@gentics/cms-models';
import { TranslateStore } from '@ngx-translate/core';
import { FormGridEditMode, FormGridViewMode } from '../../models';
import { FormGridComponent } from './form-grid.component';

const CONFIG = {
    type: 'generic',
    pluginName: 'forms',
    controls: {
        text: { labelI18n: { en: 'Text' } },
    },
    blocks: {
        group: { labelI18n: { en: 'Group' }, container: true },
    },
    flows: [],
} as unknown as FormTypeConfiguration;

function element(id: string, type: string, elements?: FormElement[]): FormElement {
    const el: any = { id, type: elements ? 'container' : 'property', label: { en: id }, formGridOptions: { type } };
    if (elements) {
        el.elements = elements;
    }
    return el;
}

describe('FormGridComponent', () => {
    let fixture: ComponentFixture<FormGridComponent>;
    let component: FormGridComponent;
    let rootId: string;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [FormGridComponent],
            providers: [
                { provide: I18nService, useValue: { getCurrentLanguage: () => 'en' } },
                { provide: TranslateStore, useValue: {} },
                { provide: I18nNotificationService, useValue: {} },
            ],
        });
        TestBed.overrideTemplate(FormGridComponent, '');
        fixture = TestBed.createComponent(FormGridComponent);
        component = fixture.componentInstance;
        rootId = component.ELEMENT_ROOT_CONTAINER_ID;
    });

    function setup(elements: FormElement[], schema: FormSchema): void {
        const ref = fixture.componentRef;
        ref.setInput('config', CONFIG);
        ref.setInput('mode', FormGridEditMode.FULL);
        ref.setInput('languages', ['en']);
        ref.setInput('viewMode', FormGridViewMode.EDITOR);
        ref.setInput('flowId', 'default');
        ref.setInput('pageIndex', 0);
        ref.setInput('schema', schema);
        ref.setInput('uiSchema', {
            key: '',
            version: '',
            pages: [{ pagename: { en: 'page' }, elements }],
        });
        fixture.detectChanges();
    }

    it('moves an element out of a non-aggregate container', () => {
        setup(
            [element('group1', 'group', [element('email', 'text')])],
            { key: '', version: '', properties: { email: { type: 'text' } } },
        );

        component.moveElementToContainer({
            pageIndex: 0,
            elementId: 'email',
            fromContainerId: 'group1',
            fromContextId: rootId,
            toContainerId: rootId,
            toContextId: rootId,
            targetIndex: 0,
        });

        const rootElements = component.uiSchema().pages[0].elements;
        expect(rootElements.map((el) => el.id)).toEqual(['email', 'group1']);
        expect(rootElements[1].elements).toEqual([]);
        expect(Object.keys(component.schema().properties)).toEqual(['email']);
    });

    it('keeps the element in place when the target container cannot be found', () => {
        const schema: FormSchema = { key: '', version: '', properties: {} };
        setup(
            [element('outer', 'group', [element('inner', 'group', [])])],
            schema,
        );
        const uiSchemaBefore = structuredClone(component.uiSchema());

        // Moving a container into its own child: the target is only reachable through the moved element
        component.moveElementToContainer({
            pageIndex: 0,
            elementId: 'outer',
            fromContainerId: rootId,
            fromContextId: rootId,
            toContainerId: 'inner',
            toContextId: rootId,
            targetIndex: 0,
        });

        expect(component.uiSchema()).toEqual(uiSchemaBefore);
        expect(component.schema()).toEqual(schema);
    });
});
