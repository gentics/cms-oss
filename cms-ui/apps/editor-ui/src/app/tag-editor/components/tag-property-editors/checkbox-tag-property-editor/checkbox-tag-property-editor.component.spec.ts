import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import {
    BooleanTagPartProperty,
    EditableTag,
    TagEditorContext,
    TagPart,
    TagPartType,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';
import { CheckboxComponent, GenticsUICoreModule } from '@gentics/ui-core';
import { cloneDeep } from 'lodash-es';
import { componentTest, configureComponentTest } from '../../../../../testing';
import {
    MockTagPropertyInfo,
    getExampleValidationSuccess,
    getMockedTagEditorContext,
    getMultiValidationResult,
    mockEditableTag,
} from '../../../../../testing/test-tag-editor-data.mock';
import { TagPropertyLabelPipe } from '../../../pipes/tag-property-label/tag-property-label.pipe';
import { TagPropertyEditorResolverService } from '../../../providers/tag-property-editor-resolver/tag-property-editor-resolver.service';
import { ValidationErrorInfo } from '../../shared/validation-error-info/validation-error-info.component';
import { TagPropertyEditorHostComponent } from '../../tag-property-editor-host/tag-property-editor-host.component';
import { CheckboxTagPropertyEditor } from './checkbox-tag-property-editor.component';

describe('CheckboxTagPropertyEditorComponent', () => {

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
                CheckboxTagPropertyEditor,
                ValidationErrorInfo,
            ],
        });
    });

    describe('initialization', () => {

        function validateInit(fixture: ComponentFixture<TestComponent>,
            instance: TestComponent, tag: EditableTag, initialValue?: boolean, contextInfo?: Partial<TagEditorContext>): void {
            const context = getMockedTagEditorContext(tag, contextInfo);
            const tagPart = tag.tagType.parts[0];
            const tagProperty = tag.properties[tagPart.keyword] as BooleanTagPartProperty;
            tagProperty.booleanValue = initialValue;
            let expectedValue = initialValue;
            if (initialValue === undefined) {
                delete tagProperty.booleanValue;
                expectedValue = false;
            }

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(CheckboxTagPropertyEditor));
            expect(editorElement).toBeTruthy();

            const editor: CheckboxTagPropertyEditor = editorElement.componentInstance;
            editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
            editor.registerOnChange(() => null);
            fixture.detectChanges();
            tick();
            fixture.detectChanges();
            tick();

            // Make sure that a Checkbox is used
            const checkboxElement = editorElement.query(By.directive(CheckboxComponent));
            expect(checkboxElement).toBeTruthy();

            // Make sure that the initial values are correct.
            const checkbox = checkboxElement.componentInstance as CheckboxComponent;
            expect(checkbox.label).toEqual(tagPart.name);
            expect(checkbox.disabled).toBe(context.readOnly);
            expect(checkboxElement.query(By.css('input')).nativeElement.checked).toEqual(expectedValue);
        }

        it('initializes properly for the initial value true',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                validateInit(fixture, instance, tag, true);
            }),
        );

        it('initializes properly for the initial value false',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                validateInit(fixture, instance, tag, false);
            }),
        );

        it('initializes properly without an initial value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                validateInit(fixture, instance, tag, undefined);
            }),
        );

        it('is disabled if context.readOnly=true',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                validateInit(fixture, instance, tag, true, { readOnly: true });
            }),
        );

    });

    describe('user input handling', () => {

        function testInputCommunication(fixture: ComponentFixture<TestComponent>,
            instance: TestComponent, tag: EditableTag, initialValue?: boolean): void {
            const context = getMockedTagEditorContext(tag);

            const tagPart = tag.tagType.parts[0];
            const tagProperty = tag.properties[tagPart.keyword] as BooleanTagPartProperty;
            tagProperty.booleanValue = initialValue;
            if (initialValue === undefined) {
                delete tagProperty.booleanValue;
            }
            const onChangeSpy = jasmine.createSpy('onChangeFn').and.returnValue(
                getMultiValidationResult(tagPart, getExampleValidationSuccess()),
            );

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(CheckboxTagPropertyEditor));
            const editor: CheckboxTagPropertyEditor = editorElement.componentInstance;
            editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
            editor.registerOnChange(onChangeSpy);
            fixture.detectChanges();
            tick();
            fixture.detectChanges();
            tick();

            // Get the actual input element.
            const checkboxElement = editorElement.query(By.css('input')).nativeElement;

            // Simulate a user click.
            const changedProperty = cloneDeep(tagProperty);
            changedProperty.booleanValue = !initialValue;
            const expectedChanges: Partial<TagPropertyMap> = { };
            expectedChanges[tagPart.keyword] = changedProperty;
            checkboxElement.click();
            fixture.detectChanges();
            tick();
            expect(onChangeSpy.calls.count()).toBe(1);
            expect(onChangeSpy).toHaveBeenCalledWith(expectedChanges);
            onChangeSpy.calls.reset();

            // Simulate another user click.
            changedProperty.booleanValue = !!initialValue;
            checkboxElement.click();
            fixture.detectChanges();
            tick();
            expect(onChangeSpy.calls.count()).toBe(1);
            expect(onChangeSpy).toHaveBeenCalledWith(expectedChanges);
        }

        it('communicates user input for an initial value of true',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                testInputCommunication(fixture, instance, tag, true);
            }),
        );

        it('communicates user input for an initial value of false',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                testInputCommunication(fixture, instance, tag, false);
            }),
        );

        it('communicates user input for no initial value',
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
                const tagProperty = tag.properties[tagPart.keyword] as BooleanTagPartProperty;
                const origTagProperty = cloneDeep(tagProperty);
                const onChangeSpy = jasmine.createSpy('onChangeFn').and.stub();

                instance.tagPart = tagPart;
                fixture.detectChanges();
                tick();

                const editorElement = fixture.debugElement.query(By.directive(CheckboxTagPropertyEditor));
                const editor: CheckboxTagPropertyEditor = editorElement.componentInstance;
                editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
                editor.registerOnChange(onChangeSpy);
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                tick();

                // Get the actual input element.
                const checkboxElement = editorElement.query(By.css('input')).nativeElement;

                // Update the TagPropertyEditor's value using writeChangedValues().
                let expectedValue = true;
                const changedProperty = cloneDeep(origTagProperty);
                changedProperty.booleanValue = expectedValue;
                let changes: Partial<TagPropertyMap> = { };
                changes[tagPart.keyword] = changedProperty;
                editor.writeChangedValues(cloneDeep(changes));
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                tick();
                expect(onChangeSpy).not.toHaveBeenCalled();
                expect(checkboxElement.checked).toEqual(expectedValue);

                // Call writeChangedValues() with another TagProperty's value (which should be ignored)
                changedProperty.booleanValue = false;
                changes = {
                    ignoredProperty: changedProperty,
                };
                editor.writeChangedValues(cloneDeep(changes));
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                tick();
                expect(onChangeSpy).not.toHaveBeenCalled();
                expect(checkboxElement.checked).toEqual(expectedValue);

                // Add another change for our TagProperty
                expectedValue = false;
                const anotherChange = cloneDeep(origTagProperty);
                anotherChange.booleanValue = expectedValue;
                changes[tagPart.keyword] = anotherChange;
                editor.writeChangedValues(cloneDeep(changes));
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                tick();
                expect(onChangeSpy).not.toHaveBeenCalled();
                expect(checkboxElement.checked).toEqual(expectedValue);
            }),
        );

    });

});

/**
 * Creates an EditableTag, where tag.tagType.parts[0] can be used for
 * testing the CheckboxTagPropertyEditorComponent.
 */
function getMockedTag(): EditableTag {
    const tagPropInfos: MockTagPropertyInfo<BooleanTagPartProperty>[] = [
        {
            booleanValue: false,
            type: TagPropertyType.BOOLEAN,
            typeId: TagPartType.Checkbox,
        },
    ];
    return mockEditableTag(tagPropInfos);
}

/**
 * We don't add the CheckboxTagPropertyEditor directly to the template, but instead have it
 * created dynamically just like in the real use cases.
 *
 * This also tests if the mappings in the TagPropertyEditorResolverService are correct.
 */
@Component({
    template: `
        <tag-property-editor-host #tagPropEditorHost [tagPart]="tagPart"></tag-property-editor-host>
    `,
})
class TestComponent {
    @ViewChild('tagPropEditorHost', { static: true })
    tagPropEditorHost: TagPropertyEditorHostComponent;

    tagPart: TagPart;
}
