import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserDynamicTestingModule } from '@angular/platform-browser-dynamic/testing';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { SelectOption, SelectTagPartProperty, TagPart, TagPartType, TagPropertyMap, TagPropertyType } from '@gentics/cms-models';
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
import { EditableTag, TagEditorContext } from '../../../common';
import { TagPropertyLabelPipe } from '../../../pipes/tag-property-label/tag-property-label.pipe';
import { TagPropertyEditorResolverService } from '../../../providers/tag-property-editor-resolver/tag-property-editor-resolver.service';
import { ValidationErrorInfo } from '../../shared/validation-error-info/validation-error-info.component';
import { TagPropertyEditorHostComponent } from '../../tag-property-editor-host/tag-property-editor-host.component';
import { SelectTagPropertyEditor } from './select-tag-property-editor.component';

describe('SelectTagPropertyEditorComponent', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
                FormsModule,
            ],
            providers: [
                TagPropertyEditorResolverService,
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
            declarations: [
                TagPropertyEditorHostComponent,
                TagPropertyLabelPipe,
                TestComponent,
                SelectTagPropertyEditor,
                ValidationErrorInfo,
            ],
        });
        TestBed.overrideModule(BrowserDynamicTestingModule, {
            set: {
                entryComponents: [
                    SelectTagPropertyEditor,
                ],
            },
        });
    });

    describe('initialization', () => {

        function validateInit(fixture: ComponentFixture<TestComponent>,
            instance: TestComponent, tag: EditableTag, initialValue?: SelectOption[], contextInfo?: Partial<TagEditorContext>): void {
            const context = getMockedTagEditorContext(tag, contextInfo);
            const tagPart = tag.tagType.parts[0];
            const tagProperty = tag.properties[tagPart.keyword] as SelectTagPartProperty;
            tagProperty.selectedOptions = initialValue;
            if (initialValue === undefined) {
                delete tagProperty.selectedOptions;
            }

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(SelectTagPropertyEditor));
            expect(editorElement).toBeTruthy();

            const editor: SelectTagPropertyEditor = editorElement.componentInstance;
            editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
            editor.registerOnChange(() => null);
            fixture.detectChanges();
            tick();
            fixture.detectChanges();
            tick();

            // Make sure that a Select is used
            const selectElement = editorElement.query(By.directive(SelectComponent));
            expect(selectElement).toBeTruthy();

            // Make sure that the initial values are correct
            const selectComponent = selectElement.componentInstance as SelectComponent;
            expect(selectComponent.label).toEqual(tagPart.name);
            expect(selectComponent.disabled).toBe(context.readOnly);
            expect(selectComponent.optionGroups?.length).toEqual((tagProperty.options || []).length > 0 ? 1 : 0);
            const selectValue = (Array.isArray(selectComponent.value) ? selectComponent.value : [selectComponent.value]).filter(v => !!v);
            expect(selectValue.length).toEqual(tagProperty.selectedOptions.length);
            tagProperty.selectedOptions.forEach(value => {
                expect(selectValue.find(option => option === value.id)).toBeDefined();
            });
        }

        it('works properly with single select empty value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedSingleTag();
                validateInit(fixture, instance, tag, []);
            }),
        );

        it('works properly with single select no value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedSingleTag();
                validateInit(fixture, instance, tag);
            }),
        );

        it('works properly with single select not empty value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedSingleTag();
                validateInit(fixture, instance, tag, [
                    {
                        id: 1,
                        key: '1',
                        value: '1',
                    },
                ]);
            }),
        );

        it('is disabled with single select and context.readOnly=true',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedSingleTag();
                validateInit(fixture, instance, tag, [
                    {
                        id: 1,
                        key: '1',
                        value: '1',
                    },
                ], {
                    readOnly: true,
                });
            }),
        );

        it('works properly with multiple select empty value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedMultipleTag();
                validateInit(fixture, instance, tag, []);
            }),
        );

        it('works properly with multiple select no value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedMultipleTag();
                validateInit(fixture, instance, tag);
            }),
        );

        it('works properly with multiple select not empty value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedMultipleTag();
                validateInit(fixture, instance, tag, [
                    {
                        id: 1,
                        key: '1',
                        value: '1',
                    },
                    {
                        id: 2,
                        key: '2',
                        value: '2',
                    },
                ]);
            }),
        );

        it('is disabled with multiple select and context.readOnly=true',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedMultipleTag();
                validateInit(fixture, instance, tag, [
                    {
                        id: 1,
                        key: '1',
                        value: '1',
                    },
                    {
                        id: 2,
                        key: '2',
                        value: '2',
                    },
                ], {
                    readOnly: true,
                });
            }),
        );
    });

    xdescribe('user input handling', () => {

        function testInputCommunication(fixture: ComponentFixture<TestComponent>,
            instance: TestComponent, tag: EditableTag, initialValue?: SelectOption[], multiple?: boolean): void {
            const context = getMockedTagEditorContext(tag);

            const tagPart = tag.tagType.parts[0];
            const tagProperty = tag.properties[tagPart.keyword] as SelectTagPartProperty;
            tagProperty.selectedOptions = initialValue;
            if (initialValue === undefined) {
                delete tagProperty.selectedOptions;
            }

            const onChangeSpy = jasmine.createSpy('onChangeFn').and.returnValue(
                getMultiValidationResult(tagPart, getExampleValidationSuccess()),
            );

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(SelectTagPropertyEditor));
            const editor: SelectTagPropertyEditor = editorElement.componentInstance;
            editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
            editor.registerOnChange(onChangeSpy);
            fixture.detectChanges();
            tick();
            fixture.detectChanges();
            tick();

            // Get the actual input element.
            const selectElement = editorElement.query(By.css('.select-input')).nativeElement;

            // Simulate a user click.
            const changedProperty = cloneDeep(tagProperty);
            const changedValue: SelectOption[] = [
                {
                    id: 3,
                    key: '3',
                    value: '3',
                },
            ];

            const changedMultipleValue: SelectOption[] = [
                {
                    id: 1,
                    key: '1',
                    value: '1',
                },
                {
                    id: 2,
                    key: '2',
                    value: '2',
                },
                {
                    id: 3,
                    key: '3',
                    value: '3',
                },
            ];

            if (multiple) {
                changedProperty.selectedOptions = changedMultipleValue;
            } else {
                changedProperty.selectedOptions = changedValue;
            }

            const expectedChanges: Partial<TagPropertyMap> = { };
            expectedChanges[tagPart.keyword] = changedProperty;

            selectElement.click();
            fixture.detectChanges();
            tick();

            const dropdownContent = fixture.debugElement.query(By.directive(DropdownContentWrapperComponent));

            let selectOption = dropdownContent.queryAll(By.css('.select-option'))[2].nativeElement;

            selectOption.click();
            fixture.detectChanges();
            tick(1000);

            expect(onChangeSpy.calls.count()).toBe(1);
            expect(onChangeSpy).toHaveBeenCalledWith(expectedChanges);
        }

        it('communicates user input with single select empty value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedSingleTag();
                testInputCommunication(fixture, instance, tag, []);
            }),
        );

        it('communicates user input with single select not empty value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedSingleTag();
                testInputCommunication(fixture, instance, tag, [
                    {
                        id: 1,
                        key: '1',
                        value: '1',
                    },
                ]);
            }),
        );

        it('communicates user input with multiple select empty value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedMultipleTag();
                testInputCommunication(fixture, instance, tag, []);
            }),
        );

        it('communicates user input with multiple select not empty value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedMultipleTag();
                testInputCommunication(fixture, instance, tag, [
                    {
                        id: 1,
                        key: '1',
                        value: '1',
                    },
                    {
                        id: 2,
                        key: '2',
                        value: '2',
                    },
                ], true);
            }),
        );
    });

    describe('writeChangedValues()', () => {
        it('handles writeChangedValues() correctly',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedSingleTag();
                const context = getMockedTagEditorContext(tag);
                const tagPart = tag.tagType.parts[0];
                const tagProperty = tag.properties[tagPart.keyword] as SelectTagPartProperty;
                const origTagProperty = cloneDeep(tagProperty);
                const onChangeSpy = jasmine.createSpy('onChangeFn').and.stub();

                instance.tagPart = tagPart;
                fixture.detectChanges();
                tick();

                const editorElement = fixture.debugElement.query(By.directive(SelectTagPropertyEditor));
                const editor: SelectTagPropertyEditor = editorElement.componentInstance;
                editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
                editor.registerOnChange(onChangeSpy);
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                tick();

                // Get the actual input element
                const selectElement = editorElement.query(By.css('.select-input div')).nativeElement;

                // Update the TagPropertyEditor's value using writeChangedValues().
                let expectedValue: SelectOption[] = [{
                    id: 1,
                    key: '1',
                    value: '1',
                }];
                const changedProperty = cloneDeep(origTagProperty);
                changedProperty.selectedOptions = expectedValue;
                let changes: Partial<TagPropertyMap> = { };
                changes[tagPart.keyword] = changedProperty;
                editor.writeChangedValues(cloneDeep(changes));
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                tick();
                expect(onChangeSpy).not.toHaveBeenCalled();
                tagProperty.selectedOptions.forEach((value: { key: any; }) => {
                    expect(selectElement.innerHTML).toEqual(value.key);
                });

                // Call writeChangedValues() with another TagProperty's value (which should be ignored)
                changedProperty.selectedOptions = [];
                changes = {
                    ignoredProperty: changedProperty,
                };
                editor.writeChangedValues(cloneDeep(changes));
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                tick();
                expect(onChangeSpy).not.toHaveBeenCalled();
                tagProperty.selectedOptions.forEach((value: { key: any; }) => {
                    expect(selectElement.innerHTML).toEqual(value.key);
                });

                // Add another change for our TagProperty
                expectedValue = [];
                const anotherChange = cloneDeep(origTagProperty);
                anotherChange.selectedOptions = expectedValue;
                changes[tagPart.keyword] = anotherChange;
                editor.writeChangedValues(cloneDeep(changes));
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                tick();
                expect(onChangeSpy).not.toHaveBeenCalled();
                tagProperty.selectedOptions.forEach((value: { key: any; }) => {
                    expect(selectElement.innerHTML).toEqual(value.key);
                });
            }),
        );
    });
});

/**
 * Creates an EditableTag, where tag.tagType.parts[0] can be used for
 * testing the SelectTagPropertyEditorComponent (Single).
 */
function getMockedSingleTag(): EditableTag {
    const tagPropInfos: MockTagPropertyInfo<SelectTagPartProperty>[] = [
        {
            type: TagPropertyType.SELECT,
            typeId: TagPartType.SelectSingle,
            options: [
                {
                    id: 1,
                    key: '1',
                    value: '1',
                },
                {
                    id: 2,
                    key: '2',
                    value: '2',
                },
                {
                    id: 3,
                    key: '3',
                    value: '3',
                },
            ],
            selectedOptions: [],
        },
    ];
    return mockEditableTag(tagPropInfos);
}

/**
 * Creates an EditableTag, where tag.tagType.parts[0] can be used for
 * testing the SelectTagPropertyEditorComponent (Multiple).
 */
function getMockedMultipleTag(): EditableTag {
    const tagPropInfos: MockTagPropertyInfo<SelectTagPartProperty>[] = [
        {
            type: TagPropertyType.MULTISELECT,
            typeId: TagPartType.SelectMultiple,
            options: [
                {
                    id: 1,
                    key: '1',
                    value: '1',
                },
                {
                    id: 2,
                    key: '2',
                    value: '2',
                },
                {
                    id: 3,
                    key: '3',
                    value: '3',
                },
            ],
            selectedOptions: [],
        },
    ];
    return mockEditableTag(tagPropInfos);
}

/**
 * We don't add the SelectTagPropertyEditor directly to the template, but instead have it
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
