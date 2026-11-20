import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '@gentics/cms-components';
import { FormElement, FormSchemaProperties, FormTypeConfiguration } from '@gentics/cms-models';
import { ISortableEvent, ModalService } from '@gentics/ui-core';
import {
    ATTR_CONTAINER_ID,
    ATTR_CONTEXT_ID,
    ATTR_ELEMENT_ID,
    ElementContainerMoveEvent,
    FormGridEditMode,
} from '../../models';
import { FormGridElementsContainerComponent } from './form-grid-elements-container.component';

const ROOT_ID = 'root-container';

const CONFIG = {
    controls: {
        text: { labelI18n: { en: 'Text' } },
    },
    blocks: {
        group: { labelI18n: { en: 'Group' }, container: true },
    },
} as unknown as FormTypeConfiguration;

function controlElement(id: string): FormElement {
    return { id, type: 'property', label: { en: id }, formGridOptions: { type: 'text' } } as unknown as FormElement;
}

describe('FormGridElementsContainerComponent', () => {
    let fixture: ComponentFixture<FormGridElementsContainerComponent>;
    let component: FormGridElementsContainerComponent;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [FormGridElementsContainerComponent],
            providers: [
                { provide: I18nService, useValue: {} },
                { provide: ModalService, useValue: {} },
            ],
        });
        TestBed.overrideTemplate(FormGridElementsContainerComponent, '');
        fixture = TestBed.createComponent(FormGridElementsContainerComponent);
        component = fixture.componentInstance;
    });

    function setup(options: {
        id: string;
        contextId: string;
        isAggregate: boolean;
        elements: FormElement[];
        schemaPropertiesMap: FormSchemaProperties;
    }): void {
        const ref = fixture.componentRef;
        ref.setInput('rootId', ROOT_ID);
        ref.setInput('id', options.id);
        ref.setInput('contextId', options.contextId);
        ref.setInput('parentId', ROOT_ID);
        ref.setInput('isAggregate', options.isAggregate);
        ref.setInput('config', CONFIG);
        ref.setInput('controlTypeNames', new Set(['text']));
        ref.setInput('level', 1);
        ref.setInput('mode', FormGridEditMode.FULL);
        ref.setInput('pageIndex', 0);
        ref.setInput('languages', ['en']);
        ref.setInput('gridSurface', document.createElement('div'));
        ref.setInput('schema', { key: '', version: '', properties: {} });
        ref.setInput('elements', options.elements);
        ref.setInput('elementMap', {});
        ref.setInput('schemaPropertiesMap', options.schemaPropertiesMap);
        fixture.detectChanges();
    }

    it('displays a control inside a non-aggregate container', () => {
        // Non-aggregate containers keep the root context, and their children are not prefixed in the maps
        setup({
            id: 'group1',
            contextId: ROOT_ID,
            isAggregate: false,
            elements: [controlElement('email')],
            schemaPropertiesMap: {
                email: { type: 'text' },
            },
        });

        const items = component.displayItems();
        expect(items.map((item) => item.id)).toEqual(['email']);
        expect(items[0].isControl).toBe(true);
    });

    it('displays a control inside an aggregate', () => {
        setup({
            id: 'agg',
            contextId: ROOT_ID,
            isAggregate: true,
            elements: [controlElement('child')],
            schemaPropertiesMap: {
                'agg@child': { type: 'text' },
            },
        });

        const items = component.displayItems();
        expect(items.map((item) => item.id)).toEqual(['child']);
        expect(items[0].isControl).toBe(true);
    });

    it('puts a dropped element back into its source list and emits the move', () => {
        setup({
            id: 'group1',
            contextId: ROOT_ID,
            isAggregate: false,
            elements: [],
            schemaPropertiesMap: {},
        });

        const from = document.createElement('div');
        from.setAttribute(ATTR_CONTAINER_ID, 'group1');
        from.setAttribute(ATTR_CONTEXT_ID, ROOT_ID);
        const to = document.createElement('div');
        to.setAttribute(ATTR_CONTAINER_ID, ROOT_ID);
        to.setAttribute(ATTR_CONTEXT_ID, ROOT_ID);

        const first = document.createElement('div');
        const item = document.createElement('div');
        item.setAttribute(ATTR_ELEMENT_ID, 'email');
        const last = document.createElement('div');
        from.append(first, last);
        // SortableJS has already moved the item from index 1 of `from` into `to`
        to.append(item);

        const emitted: ElementContainerMoveEvent[] = [];
        component.moveToContainer.subscribe((event) => emitted.push(event));

        component.onSortableAdd({
            item,
            from,
            to,
            oldIndex: 1,
            newIndex: 0,
            newDraggableIndex: 0,
        } as unknown as ISortableEvent);

        expect(to.children.length).toBe(0);
        expect(Array.from(from.children)).toEqual([first, item, last]);
        expect(emitted).toEqual([{
            pageIndex: 0,
            elementId: 'email',
            fromContainerId: 'group1',
            fromContextId: ROOT_ID,
            toContainerId: ROOT_ID,
            toContextId: ROOT_ID,
            targetIndex: 0,
        }]);
    });
});
