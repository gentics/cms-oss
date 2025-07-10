import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { TagEditorContext } from '@gentics/cms-integration-api-models';
import { EditableTag, StringTagPartProperty, TagPart, TagPartType, TagPropertyMap, TagPropertyType } from '@gentics/cms-models';
import { GenticsUICoreModule, InputComponent, TextareaComponent } from '@gentics/ui-core';
import { cloneDeep } from 'lodash-es';
import { componentTest, configureComponentTest } from '../../../../../testing';
import {
    getExampleEditableTag,
    getExampleNaturalNumberValidationInfo,
    getExampleValidationFailed,
    getExampleValidationSuccess,
    getMockedTagEditorContext,
    getMultiValidationResult,
} from '../../../../../testing/test-tag-editor-data.mock';
import { TagPropertyLabelPipe } from '../../../pipes/tag-property-label/tag-property-label.pipe';
import { TagPropertyEditorResolverService } from '../../../providers/tag-property-editor-resolver/tag-property-editor-resolver.service';
import { ValidationErrorInfoComponent } from '../../shared/validation-error-info/validation-error-info.component';
import { TagPropertyEditorHostComponent } from '../../tag-property-editor-host/tag-property-editor-host.component';
import { TextTagPropertyEditor } from './text-tag-property-editor.component';

/** The number of ms to wait after the input event. */
const WAIT_AFTER_INPUT = 500;

describe('TextTagPropertyEditorComponent', () => {

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
                TextTagPropertyEditor,
                ValidationErrorInfoComponent,
            ],
        });
    });

    describe('initialization', () => {

        function validateTextInputInit(
            fixture: ComponentFixture<TestComponent>,
            instance: TestComponent,
            tag: EditableTag,
            contextInfo?: Partial<TagEditorContext>,
        ): void {
            const context = getMockedTagEditorContext(tag, contextInfo);
            const tagPart = tag.tagType.parts[0];
            const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
            const origTagProperty = cloneDeep(tagProperty);

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(TextTagPropertyEditor));
            expect(editorElement).toBeTruthy();

            const editor: TextTagPropertyEditor = editorElement.componentInstance;
            editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
            editor.registerOnChange(() => null);
            fixture.detectChanges();
            tick();

            // Make sure that an InputField is used for Text(short) and that there is no Textarea
            const inputElement = editorElement.query(By.directive(InputComponent));
            expect(inputElement).toBeTruthy();
            // eslint-disable-next-line @typescript-eslint/no-unused-expressions
            expect(editorElement.query(By.directive(TextareaComponent))).toBeFalsy;

            // Make sure that the initial values are correct.
            const inputComponent = inputElement.componentInstance as InputComponent;
            expect(inputComponent.label).toEqual(tagPart.name);
            expect(inputComponent.readonly).toBe(context.readOnly);
            expect(inputElement.query(By.css('input')).nativeElement.value).toEqual(origTagProperty.stringValue);
        }

        it('initializes properly for Text(short) without an initial value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                const tagPart = tag.tagType.parts[0];
                tagPart.typeId = TagPartType.TextShort;
                const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
                tagProperty.stringValue = '';

                validateTextInputInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for Text(short) with an initial value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                const tagPart = tag.tagType.parts[0];
                tagPart.typeId = TagPartType.TextShort;
                const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
                tagProperty.stringValue = 'test';

                validateTextInputInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for Text(short) and context.readOnly=true',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                const tagPart = tag.tagType.parts[0];
                tagPart.typeId = TagPartType.TextShort;
                const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
                tagProperty.stringValue = 'test';

                validateTextInputInit(fixture, instance, tag, { readOnly: true });
            }),
        );

        function validateTextAreaInit(
            fixture: ComponentFixture<TestComponent>,
            instance: TestComponent,
            tag: EditableTag,
            contextInfo?: Partial<TagEditorContext>,
        ): void {
            const context = getMockedTagEditorContext(tag, contextInfo);
            const tagPart = tag.tagType.parts[0];
            const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
            const origTagProperty = cloneDeep(tagProperty);

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(TextTagPropertyEditor));
            expect(editorElement).toBeTruthy();

            const editor: TextTagPropertyEditor = editorElement.componentInstance;
            editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
            editor.registerOnChange(() => getMultiValidationResult(tagPart, getExampleValidationSuccess()));
            fixture.detectChanges();
            tick(100);

            // Make sure that a Textarea is used for Text and that there is no InputField.
            const textAreaElement = editorElement.query(By.directive(TextareaComponent));
            expect(textAreaElement).toBeTruthy();
            // eslint-disable-next-line @typescript-eslint/no-unused-expressions
            expect(editorElement.query(By.directive(InputComponent))).toBeFalsy;

            // Make sure that the initial values are correct.
            const textAreaComponent = textAreaElement.componentInstance as TextareaComponent;
            expect(textAreaComponent.label).toEqual(tagPart.name);
            expect(textAreaComponent.readonly).toBe(context.readOnly);
            const textAreaEl: HTMLTextAreaElement = textAreaElement.query(By.css('textarea')).nativeElement;
            expect(textAreaEl.value).toEqual(origTagProperty.stringValue);
        }

        it('initializes properly for Text without an initial value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                const tagPart = tag.tagType.parts[0];
                tagPart.typeId = TagPartType.Text;
                const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
                tagProperty.stringValue = '';

                validateTextAreaInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for Text with an initial value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                const tagPart = tag.tagType.parts[0];
                tagPart.typeId = TagPartType.Text;
                const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
                tagProperty.stringValue = 'Line 1\nLine 2';

                validateTextAreaInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for Text with an initial value and context.readOnly=true',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                const tagPart = tag.tagType.parts[0];
                tagPart.typeId = TagPartType.Text;
                const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
                tagProperty.stringValue = 'Line 1\nLine 2';

                validateTextAreaInit(fixture, instance, tag, { readOnly: true });
            }),
        );

        it('initializes properly for Text/HTML without an initial value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                const tagPart = tag.tagType.parts[0];
                tagPart.typeId = TagPartType.TextHtml;
                const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
                tagProperty.stringValue = '';

                validateTextAreaInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for Text/HTML with an initial value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                const tagPart = tag.tagType.parts[0];
                tagPart.typeId = TagPartType.TextHtml;
                const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
                tagProperty.stringValue = 'Line 1\nLine 2';

                validateTextAreaInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for Text/HTML (long) without an initial value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                const tagPart = tag.tagType.parts[0];
                tagPart.typeId = TagPartType.TextHtmlLong;
                const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
                tagProperty.stringValue = '';

                validateTextAreaInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for Text/HTML (long) with an initial value',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                const tagPart = tag.tagType.parts[0];
                tagPart.typeId = TagPartType.TextHtmlLong;
                const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
                tagProperty.stringValue = 'Line 1\nLine 2';

                validateTextAreaInit(fixture, instance, tag);
            }),
        );

    });

    describe('user input handling without validation', () => {

        function testInputCommunication(fixture: ComponentFixture<TestComponent>, instance: TestComponent, tag: EditableTag): void {
            const context = getMockedTagEditorContext(tag);
            const tagPart = tag.tagType.parts[0];
            tagPart.typeId = TagPartType.TextShort;
            const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
            const onChangeSpy = jasmine.createSpy('onChangeFn').and.returnValue(
                getMultiValidationResult(tagPart, getExampleValidationSuccess()),
            );

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(TextTagPropertyEditor));
            const editor: TextTagPropertyEditor = editorElement.componentInstance;
            editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
            editor.registerOnChange(onChangeSpy);
            fixture.detectChanges();
            tick();

            // Get the actual textBox element (input or textarea, depending on TagPartType).
            const textBoxElement = editorElement.query(By.css('input, textarea')).nativeElement;

            // Simulate the entering of some text.
            const changedProperty = cloneDeep(tagProperty);
            changedProperty.stringValue = 'Some text';
            const expectedChanges: Partial<TagPropertyMap> = { };
            expectedChanges[tagPart.keyword] = changedProperty;
            textBoxElement.value = changedProperty.stringValue;
            triggerInputEvent(textBoxElement);
            fixture.detectChanges();
            tick(WAIT_AFTER_INPUT);
            expect(onChangeSpy.calls.count()).toBe(1);
            expect(onChangeSpy).toHaveBeenCalledWith(expectedChanges);
            onChangeSpy.calls.reset();

            // Enter some more text.
            changedProperty.stringValue += ' and even more text';
            textBoxElement.value = changedProperty.stringValue;
            triggerInputEvent(textBoxElement);
            fixture.detectChanges();
            tick(WAIT_AFTER_INPUT);
            expect(onChangeSpy.calls.count()).toBe(1);
            expect(onChangeSpy).toHaveBeenCalledWith(expectedChanges);
            onChangeSpy.calls.reset();

            // Delete the text.
            changedProperty.stringValue = '';
            textBoxElement.value = changedProperty.stringValue;
            triggerInputEvent(textBoxElement);
            fixture.detectChanges();
            tick(WAIT_AFTER_INPUT);
            expect(onChangeSpy.calls.count()).toBe(1);
            expect(onChangeSpy).toHaveBeenCalledWith(expectedChanges);
        }

        it('communicates user input for Text(short)',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                tag.tagType.parts[0].typeId = TagPartType.TextShort;
                testInputCommunication(fixture, instance, tag);
            }),
        );

        it('communicates user input for Text',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                tag.tagType.parts[0].typeId = TagPartType.Text;
                testInputCommunication(fixture, instance, tag);
            }),
        );

        it('communicates user input for Text/HTML',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                tag.tagType.parts[0].typeId = TagPartType.TextHtml;
                testInputCommunication(fixture, instance, tag);
            }),
        );

        it('communicates user input for Text/HTML (long)',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                tag.tagType.parts[0].typeId = TagPartType.TextHtmlLong;
                testInputCommunication(fixture, instance, tag);
            }),
        );

    });

    describe('user input handling with validation', () => {

        function testInputValidation(fixture: ComponentFixture<TestComponent>, instance: TestComponent, tag: EditableTag): void {
            const context = getMockedTagEditorContext(tag);
            const tagPart = tag.tagType.parts[0];
            tagPart.typeId = TagPartType.TextShort;
            tagPart.mandatory = true;
            tagPart.regex = getExampleNaturalNumberValidationInfo();
            const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
            const onChangeSpy = jasmine.createSpy('onChangeFn');

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(TextTagPropertyEditor));
            const editor: TextTagPropertyEditor = editorElement.componentInstance;
            editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
            editor.registerOnChange(onChangeSpy);
            fixture.detectChanges();
            tick();

            // Get the actual textBox element (input or textarea, depending on TagPartType) and the ValidationErrorInfo element.
            const textBoxElement = editorElement.query(By.css('input, textarea')).nativeElement as HTMLInputElement;
            const validationInfoElement = editorElement.query(By.directive(ValidationErrorInfoComponent));

            // Simulate the entering of some valid text.
            onChangeSpy.and.returnValue(getMultiValidationResult(tagPart, getExampleValidationSuccess()));
            const changedProperty = cloneDeep(tagProperty);
            changedProperty.stringValue = '100';
            const expectedChanges: Partial<TagPropertyMap> = { };
            expectedChanges[tagPart.keyword] = changedProperty;
            textBoxElement.value = changedProperty.stringValue;
            triggerInputEvent(textBoxElement);
            fixture.detectChanges();
            tick(WAIT_AFTER_INPUT);
            expect(onChangeSpy.calls.count()).toBe(1);
            expect(onChangeSpy).toHaveBeenCalledWith(expectedChanges);
            onChangeSpy.calls.reset();
            triggerBlurEvent(textBoxElement);
            fixture.detectChanges();
            tick();
            expect(validationInfoElement.children.length).toBe(0);

            // Make the entered text invalid (onChange should still be called).
            onChangeSpy.and.returnValue(getMultiValidationResult(tagPart, getExampleValidationFailed()));
            changedProperty.stringValue += 'invalid';
            textBoxElement.value = changedProperty.stringValue;
            triggerInputEvent(textBoxElement);
            fixture.detectChanges();
            tick(WAIT_AFTER_INPUT);
            expect(onChangeSpy.calls.count()).toBe(1);
            expect(onChangeSpy).toHaveBeenCalledWith(expectedChanges);
            onChangeSpy.calls.reset();
            triggerBlurEvent(textBoxElement);
            fixture.detectChanges();
            tick();
            expect(validationInfoElement.children.length).not.toBe(0);

            // Make the entered text valid again.
            onChangeSpy.and.returnValue(getMultiValidationResult(tagPart, getExampleValidationSuccess()));
            changedProperty.stringValue = '1';
            textBoxElement.value = changedProperty.stringValue;
            triggerInputEvent(textBoxElement);
            fixture.detectChanges();
            tick(WAIT_AFTER_INPUT);
            expect(onChangeSpy.calls.count()).toBe(1);
            expect(onChangeSpy).toHaveBeenCalledWith(expectedChanges);
            onChangeSpy.calls.reset();
            triggerBlurEvent(textBoxElement);
            fixture.detectChanges();
            tick();
            expect(validationInfoElement.children.length).toBe(0);

            // Delete the text (onChange should be called even though TagPart is mandatory).
            onChangeSpy.and.returnValue(getMultiValidationResult(tagPart, getExampleValidationFailed()));
            changedProperty.stringValue = '';
            textBoxElement.value = changedProperty.stringValue;
            triggerInputEvent(textBoxElement);
            fixture.detectChanges();
            tick(WAIT_AFTER_INPUT);
            expect(onChangeSpy.calls.count()).toBe(1);
            expect(onChangeSpy).toHaveBeenCalledWith(expectedChanges);
            triggerBlurEvent(textBoxElement);
            fixture.detectChanges();
            tick();
            expect(validationInfoElement.children.length).not.toBe(0);
        }

        it('validates user input for Text(short)',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                tag.tagType.parts[0].typeId = TagPartType.TextShort;
                testInputValidation(fixture, instance, tag);
            }),
        );

        it('validates user input for Text',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                tag.tagType.parts[0].typeId = TagPartType.Text;
                testInputValidation(fixture, instance, tag);
            }),
        );

        it('validates user input for Text/HTML',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                tag.tagType.parts[0].typeId = TagPartType.TextHtml;
                testInputValidation(fixture, instance, tag);
            }),
        );

        it('validates user input for Text/HTML (long)',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                tag.tagType.parts[0].typeId = TagPartType.TextHtmlLong;
                testInputValidation(fixture, instance, tag);
            }),
        );

    });

    describe('writeChangedValues()', () => {

        function testWriteChangedValues(fixture: ComponentFixture<TestComponent>, instance: TestComponent, tag: EditableTag): void {
            const context = getMockedTagEditorContext(tag);
            const tagPart = tag.tagType.parts[0];
            const tagProperty: StringTagPartProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
            tagProperty.stringValue = '';
            const origTagProperty = cloneDeep(tagProperty);
            const onChangeSpy = jasmine.createSpy('onChangeFn').and.stub();

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(TextTagPropertyEditor));
            const editor: TextTagPropertyEditor = editorElement.componentInstance;
            editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
            editor.registerOnChange(onChangeSpy);
            fixture.detectChanges();
            tick();

            // Get the actual textBox element (input or textarea, depending on TagPartType).
            const textBoxElement = editorElement.query(By.css('input, textarea')).nativeElement as HTMLInputElement;

            // Update the TagPropertyEditor's value using writeChangedValues().
            let expectedValue = 'New Value';
            const changedProperty = cloneDeep(origTagProperty);
            changedProperty.stringValue = expectedValue;
            let changes: Partial<TagPropertyMap> = { };
            changes[tagPart.keyword] = changedProperty;
            editor.writeChangedValues(cloneDeep(changes));
            fixture.detectChanges();
            tick(WAIT_AFTER_INPUT);
            expect(onChangeSpy).not.toHaveBeenCalled();
            expect(textBoxElement.value).toEqual(expectedValue);

            // Call writeChangedValues() with another TagProperty's value (which should be ignored)
            changedProperty.stringValue = 'This change should be ignored.';
            changes = {
                ignoredProperty: changedProperty,
            };
            editor.writeChangedValues(cloneDeep(changes));
            fixture.detectChanges();
            tick(WAIT_AFTER_INPUT);
            expect(onChangeSpy).not.toHaveBeenCalled();
            expect(textBoxElement.value).toEqual(expectedValue);

            // Add another change for our TagProperty
            expectedValue = 'This change needs to be applied.';
            const anotherChange = cloneDeep(origTagProperty);
            anotherChange.stringValue = expectedValue;
            changes[tagPart.keyword] = anotherChange;
            editor.writeChangedValues(cloneDeep(changes));
            fixture.detectChanges();
            tick(WAIT_AFTER_INPUT);
            expect(onChangeSpy).not.toHaveBeenCalled();
            expect(textBoxElement.value).toEqual(expectedValue);
        }

        it('handles writeChangedValues() correctly for Text(short)',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                tag.tagType.parts[0].typeId = TagPartType.TextShort;
                testWriteChangedValues(fixture, instance, tag);
            }),
        );

        it('handles writeChangedValues() correctly for Text',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                tag.tagType.parts[0].typeId = TagPartType.Text;
                testWriteChangedValues(fixture, instance, tag);
            }),
        );

        it('handles writeChangedValues() correctly for Text/HTML',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                tag.tagType.parts[0].typeId = TagPartType.TextHtml;
                testWriteChangedValues(fixture, instance, tag);
            }),
        );

        it('handles writeChangedValues() correctly for Text/HTML (long)',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getMockedTag();
                tag.tagType.parts[0].typeId = TagPartType.TextHtmlLong;
                testWriteChangedValues(fixture, instance, tag);
            }),
        );

    });

});

/**
 * Creates an EditableTag, where tag.tagType.parts[0] can be used for
 * testing the TextTagPropertyEditorComponent.
 */
function getMockedTag(): EditableTag {
    const tag = getExampleEditableTag();
    const tagPart = tag.tagType.parts[0];
    tagPart.editable = true;
    tagPart.mandatory = false;
    tagPart.type = TagPropertyType.STRING;
    tagPart.typeId = TagPartType.TextShort;
    (tagPart.defaultProperty as StringTagPartProperty).stringValue = '';
    delete tagPart.regex;
    tag.properties[tagPart.keyword].type = tagPart.type;
    (tag.properties[tagPart.keyword] as StringTagPartProperty).stringValue = '';
    return tag;
}

function triggerInputEvent(element: HTMLElement): void {
    const event: Event = document.createEvent('Event');
    event.initEvent('input', true, true);
    element.dispatchEvent(event);
}

function triggerBlurEvent(element: HTMLElement): void {
    const event: Event = document.createEvent('Event');
    event.initEvent('blur', true, true);
    element.dispatchEvent(event);
}

/**
 * We don't add the TextTagPropertyEditor directly to the template, but instead have it
 * created dynamically just like in the real use cases.
 *
 * This also tests if the mappings in the TagPropertyEditorResolverService are correct.
 */
@Component({
    template: `
        <tag-property-editor-host #tagPropEditorHost [tagPart]="tagPart"></tag-property-editor-host>
    `,
    standalone: false,
})
class TestComponent {
    @ViewChild('tagPropEditorHost', { static: true })
    tagPropEditorHost: TagPropertyEditorHostComponent;

    tagPart: TagPart;
}
