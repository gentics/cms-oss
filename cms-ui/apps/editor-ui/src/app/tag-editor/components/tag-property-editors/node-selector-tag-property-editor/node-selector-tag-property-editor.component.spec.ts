import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { Node, NodeTagPartProperty, TagPart, TagPartType, TagPropertyMap, TagPropertyType } from '@gentics/cms-models';
import { DropdownContentWrapperComponent, GenticsUICoreModule, SelectComponent } from '@gentics/ui-core';
import { cloneDeep } from 'lodash-es';
import { componentTest, configureComponentTest } from '../../../../../testing';
import {
    MockTagPropertyInfo,
    getExampleValidationSuccess,
    getMockedTagEditorContext,
    getMultiValidationResult,
    mockEditableTag,
} from '../../../../../testing/test-tag-editor-data.mock';
import { EntityResolver } from '../../../../core/providers/entity-resolver/entity-resolver';
import { I18nService } from '../../../../core/providers/i18n/i18n.service';
import { ApplicationStateService } from '../../../../state';
import { TestApplicationState } from '../../../../state/test-application-state.mock';
import { EditableTag, TagEditorContext } from '../../../common';
import { TagPropertyLabelPipe } from '../../../pipes/tag-property-label/tag-property-label.pipe';
import { TagPropertyEditorResolverService } from '../../../providers/tag-property-editor-resolver/tag-property-editor-resolver.service';
import { ValidationErrorInfo } from '../../shared/validation-error-info/validation-error-info.component';
import { TagPropertyEditorHostComponent } from '../../tag-property-editor-host/tag-property-editor-host.component';
import { NodeSelectorTagPropertyEditor } from './node-selector-tag-property-editor.component';

describe('NodeSelectorTagPropertyEditorComponent', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
                FormsModule,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState},
                { provide: EntityResolver, useClass: MockEntityResolver },
                { provide: I18nService, useClass: MockI18nService },
                TagPropertyEditorResolverService,
            ],
            declarations: [
                TagPropertyEditorHostComponent,
                TagPropertyLabelPipe,
                TestComponent,
                NodeSelectorTagPropertyEditor,
                ValidationErrorInfo,
            ],
        });
    });

    describe('initialization', () => {

        const AVAILABLE_NODES = [1];
        let appState: TestApplicationState;

        beforeEach(() => {
            appState = TestBed.get(ApplicationStateService);
            appState.mockState({
                folder: {
                    nodes: {
                        list: AVAILABLE_NODES,
                    },
                },
            });
        });

        function validateInit(fixture: ComponentFixture<TestComponent>,
            instance: TestComponent, tag: EditableTag, initialValue?: number, contextInfo?: Partial<TagEditorContext>): void {
            const context = getMockedTagEditorContext(tag, contextInfo);
            const tagPart = tag.tagType.parts[0];

            const tagProperty = tag.properties[tagPart.keyword] as NodeTagPartProperty;
            tagProperty.nodeId = initialValue;

            if (initialValue === undefined) {
                delete tagProperty.nodeId;
            }

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(NodeSelectorTagPropertyEditor));
            expect(editorElement).toBeTruthy();

            const editor: NodeSelectorTagPropertyEditor = editorElement.componentInstance;
            editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
            editor.registerOnChange(() => null);
            fixture.detectChanges();
            tick();
            fixture.detectChanges();
            tick();

            // Make sure that a Node is used
            const nodeElement = editorElement.query(By.directive(SelectComponent));
            expect(nodeElement).toBeTruthy();

            // Make sure that the initial values are correct
            const nodeSelector = nodeElement.componentInstance as SelectComponent;
            expect(nodeSelector.label).toEqual(tagPart.name);
            expect(nodeSelector.value).toEqual(tagProperty.nodeId);
            expect(nodeSelector.disabled).toBe(context.readOnly);
            if (tagProperty.nodeId && !AVAILABLE_NODES.includes(tagProperty.nodeId)) {
                expect(nodeSelector.placeholder).toBe('editor.node_not_found');
            }
            if (!tagProperty.nodeId) {
                expect(nodeSelector.placeholder).toBe('editor.node_no_selection');
            }

        }

        it('works properly with node unset property',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                validateInit(fixture, instance, tag, undefined);
            }),
        );

        it('works properly with node set property',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                validateInit(fixture, instance, tag, 1);
            }),
        );

        it('works properly with node set property that is no longer available',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                validateInit(fixture, instance, tag, -1);
            }),
        );

        it('is disabled for context.readOnly=true',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                validateInit(fixture, instance, tag, 1, { readOnly: true });
            }),
        );
    });

    xdescribe('user input handling', () => {

        function testInputCommunication(fixture: ComponentFixture<TestComponent>,
            instance: TestComponent, tag: EditableTag, initialValue?: number): void {
            const context = getMockedTagEditorContext(tag);

            const state: TestApplicationState = fixture.debugElement.injector.get(ApplicationStateService) as any;

            state.mockState({
                folder: {
                    nodes: {
                        list: [
                            1, 2, 3,
                        ],
                    },
                },
            });

            const tagPart = tag.tagType.parts[0];
            const tagProperty = tag.properties[tagPart.keyword] as NodeTagPartProperty;
            tagProperty.nodeId = initialValue;
            if (initialValue === undefined) {
                delete tagProperty.nodeId;
            }

            const onChangeSpy = jasmine.createSpy('onChangeFn').and.returnValue(
                getMultiValidationResult(tagPart, getExampleValidationSuccess()),
            );

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(NodeSelectorTagPropertyEditor));
            const editor: NodeSelectorTagPropertyEditor = editorElement.componentInstance;
            editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
            editor.registerOnChange(onChangeSpy);
            fixture.detectChanges();
            tick();
            fixture.detectChanges();
            tick();

            // Get the actual input element.
            const nodeElement = editorElement.query(By.css('.select-input')).nativeElement;

            // Simulate a user click.
            const changedProperty = cloneDeep(tagProperty);
            changedProperty.nodeId = 2;

            const expectedChanges: Partial<TagPropertyMap> = { };
            expectedChanges[tagPart.keyword] = changedProperty;

            nodeElement.click();
            fixture.detectChanges();
            tick();

            const dropdownContent = fixture.nativeElement.query(By.directive(DropdownContentWrapperComponent));

            const selectOption = dropdownContent.queryAll(By.css('.select-option'))[1].nativeElement;

            selectOption.click();
            fixture.detectChanges();
            tick(1000);

            expect(onChangeSpy.calls.count()).toBe(1);
            expect(onChangeSpy).toHaveBeenCalledWith(expectedChanges);
        }

        it('communicates user input with node set value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                testInputCommunication(fixture, instance, tag, 1);
            }),
        );

        it('communicates user input with node unset value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                testInputCommunication(fixture, instance, tag, undefined);
            }),
        );
    });

    describe('writeChangedValues()', () => {
        it('handles writeChangedValues() correctly',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                const context = getMockedTagEditorContext(tag);
                const tagPart = tag.tagType.parts[0];
                const tagProperty = tag.properties[tagPart.keyword] as NodeTagPartProperty;
                const origTagProperty = cloneDeep(tagProperty);
                const onChangeSpy = jasmine.createSpy('onChangeFn').and.stub();

                instance.tagPart = tagPart;
                fixture.detectChanges();
                tick();

                const editorElement = fixture.debugElement.query(By.directive(NodeSelectorTagPropertyEditor));
                const editor: NodeSelectorTagPropertyEditor = editorElement.componentInstance;
                editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
                editor.registerOnChange(onChangeSpy);
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                tick();

                // Get the actual input element
                const nodeElement = editorElement.query(By.directive(SelectComponent));

                // Update the TagPropertyEditor's value using writeChangedValues().
                let expectedValue = 2;
                const changedProperty = cloneDeep(origTagProperty);
                changedProperty.nodeId = expectedValue;
                let changes: Partial<TagPropertyMap> = { };
                changes[tagPart.keyword] = changedProperty;
                editor.writeChangedValues(cloneDeep(changes));
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                tick();
                expect(onChangeSpy).not.toHaveBeenCalled();
                expect((<SelectComponent> nodeElement.componentInstance).value).toEqual(expectedValue);

                // Call writeChangedValues() with another TagProperty's value (which should be ignored)
                changedProperty.nodeId = 3;
                changes = {
                    ignoredProperty: changedProperty,
                };
                editor.writeChangedValues(cloneDeep(changes));
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                tick();
                expect(onChangeSpy).not.toHaveBeenCalled();
                expect((<SelectComponent> nodeElement.componentInstance).value).toEqual(expectedValue);

                // Add another change for our TagProperty
                expectedValue = 1;
                const anotherChange = cloneDeep(origTagProperty);
                anotherChange.nodeId = expectedValue;
                changes[tagPart.keyword] = anotherChange;
                editor.writeChangedValues(cloneDeep(changes));
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                tick();
                expect(onChangeSpy).not.toHaveBeenCalled();
                expect((<SelectComponent> nodeElement.componentInstance).value).toEqual(expectedValue);
            }),
        );
    });
});

/**
 * Creates an EditableTag, where tag.tagType.parts[0] can be used for
 * testing the NodeSelectorTagPropertyEditorComponent.
 */
function getMockedTag(): EditableTag {
    const tagPropInfos: MockTagPropertyInfo<NodeTagPartProperty>[] = [
        {
            type: TagPropertyType.NODE,
            typeId: TagPartType.Node,
            nodeId: 1,
        },
    ];
    return mockEditableTag(tagPropInfos);
}


/**
 * We don't add the NodeSelectorTagPropertyEditor directly to the template, but instead have it
 * created dynamically just like in the real use cases.
 *
 * This also tests if the mappings in the TagPropertyEditorResolverService are correct.
 */
@Component({
    template: `
        <gtx-overlay-host></gtx-overlay-host>
        <tag-property-editor-host #tagPropEditorHost [tagPart]="tagPart"></tag-property-editor-host>
    `,
})
class TestComponent {
    @ViewChild('tagPropEditorHost', { static: true })
    tagPropEditorHost: TagPropertyEditorHostComponent;

    tagPart: TagPart;
}

class MockEntityResolver {
    getNode(id: number): Node {
        return {
            id: id,
            name: `Node ${id}`,
        } as any;
    }
}

class MockI18nService {
    translate(key: string | string, params?: any): string {
        return key;
    }
}
