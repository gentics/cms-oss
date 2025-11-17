import { Component, ComponentRef, ViewChild } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import {
    MultiValidationResult,
    TagChangedFn,
    TagEditorContext,
    TagPropertiesChangedFn,
    TagPropertyEditor,
    ValidationResult,
} from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    StringTagPartProperty,
    TagPartProperty,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { mockPipes } from '@gentics/ui-core/testing';
import { I18nService } from '@gentics/cms-components';
import { cloneDeep } from 'lodash-es';
import { TagEditorHostComponent } from '../..';
import { componentTest, configureComponentTest } from '../../../../testing';
import { spyOnDynamicallyCreatedComponent } from '../../../../testing/dynamic-components';
import { getExampleEditableTag, getMockedTagEditorContext } from '../../../../testing/test-tag-editor-data.mock';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { ApplicationStateService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { assertTagEditorContextsEqual } from '../../common/impl/tag-editor-context.spec';
import { TagPropertyLabelPipe } from '../../pipes/tag-property-label/tag-property-label.pipe';
import { TagEditorService } from '../../providers/tag-editor/tag-editor.service';
import { TagPropertyEditorResolverService } from '../../providers/tag-property-editor-resolver/tag-property-editor-resolver.service';
import { ValidationErrorInfoComponent } from '../shared/validation-error-info/validation-error-info.component';
import { TagPropertyEditorHostComponent } from '../tag-property-editor-host/tag-property-editor-host.component';
import { TextTagPropertyEditor } from '../tag-property-editors/text-tag-property-editor/text-tag-property-editor.component';
import { GenticsTagEditorComponent } from './gentics-tag-editor.component';

describe('GenticsTagEditorComponent', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
                FormsModule,
            ],
            providers: [
                TagPropertyEditorResolverService,
                { provide: ErrorHandler, useClass: MockErrorHandlerService },
                { provide: TagEditorService, useClass: MockTagEditorService },
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
            declarations: [
                GenticsTagEditorComponent,
                TagEditorHostComponent,
                TagPropertyEditorHostComponent,
                TagPropertyLabelPipe,
                TestComponent,
                TextTagPropertyEditor,
                ValidationErrorInfoComponent,
                mockPipes('objTagName'),
            ],
        });
    });

    it('creates the correct number of tag property editors and initializes them correctly',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();

            // Create the TagPropertyEditor spies and make sure that the spied methods are called in the right order.
            const initTagPropEditorSpies: jasmine.Spy[] = [];
            const registerOnChangeSpies: jasmine.Spy[] = [];
            const writeChangedValuesSpies: jasmine.Spy[] = [];
            let validatorSpy: jasmine.Spy;
            let currTagPropertyEditorIndex = 0;
            spyOnDynamicallyCreatedComponent([GenticsTagEditorComponent, TextTagPropertyEditor],
                (componentType, componentInstance: ComponentRef<any>) => {
                    if (componentType === TextTagPropertyEditor) {
                        const index = currTagPropertyEditorIndex;
                        const origInit = componentInstance.instance.initTagPropertyEditor.bind(componentInstance.instance);
                        initTagPropEditorSpies.push(
                            spyOn(componentInstance.instance, 'initTagPropertyEditor').and.callFake((...args: any[]) => {
                                expect(registerOnChangeSpies[index].calls.count()).toBe(0);
                                expect(writeChangedValuesSpies[index].calls.count()).toBe(0);
                                origInit(...args);
                            }),
                        );
                        const origRegister = componentInstance.instance.registerOnChange.bind(componentInstance.instance);
                        registerOnChangeSpies.push(
                            spyOn(componentInstance.instance, 'registerOnChange').and.callFake((...args: any[]) => {
                                expect(initTagPropEditorSpies[index].calls.count()).toBe(1);
                                expect(writeChangedValuesSpies[index].calls.count()).toBe(0);
                                origRegister(...args);
                            }),
                        );
                        writeChangedValuesSpies.push(
                            spyOn(componentInstance.instance, 'writeChangedValues').and.stub(),
                        );
                        ++currTagPropertyEditorIndex;
                    }
                    if (componentType === GenticsTagEditorComponent) {
                        const tagEditor = (<GenticsTagEditorComponent> componentInstance.instance);
                        const origEditTag = tagEditor.editTag.bind(tagEditor);
                        spyOn(tagEditor, 'editTag').and.callFake((tag: EditableTag, context: TagEditorContext) => {
                            validatorSpy = spyOn(context.validator, 'validateTagProperty').and.callThrough();
                            return origEditTag(tag, context);
                        });
                    }
                });

            const tag = getMockedTag();
            const tagPart0Key = tag.tagType.parts[0].keyword;
            const tagPart1Key = tag.tagType.parts[1].keyword;
            const tagPart2Key = tag.tagType.parts[2].keyword;
            const tagPart5Key = tag.tagType.parts[5].keyword;
            const context = getMockedTagEditorContext(tag);

            instance.tagEditorHost.editTag(tag, context);
            fixture.detectChanges();
            tick();

            // Make sure that the validator has been called for all editable properties.
            expect(validatorSpy.calls.count()).toBe(4);
            expect(validatorSpy.calls.argsFor(0)[0]).toEqual(tag.properties[tagPart0Key]);
            expect(validatorSpy.calls.argsFor(1)[0]).toEqual(tag.properties[tagPart1Key]);
            expect(validatorSpy.calls.argsFor(2)[0]).toEqual(tag.properties[tagPart2Key]);
            expect(validatorSpy.calls.argsFor(3)[0]).toEqual(tag.properties[tagPart5Key]);

            // Verify that the correct number of TagPropertyEditor hosts has been created.
            // 4 TagParts are editable and not hidden in the editor.
            expect(fixture.debugElement.queryAll(By.directive(TagPropertyEditorHostComponent)).length).toBe(4);
            expect(fixture.debugElement.queryAll(By.directive(TextTagPropertyEditor)).length).toBe(4);
            expect(initTagPropEditorSpies.length).toBe(4);
            expect(registerOnChangeSpies.length).toBe(4);
            expect(writeChangedValuesSpies.length).toBe(4);

            // Verify that the TagPropertyEditor initialization methods have been called and
            // that the parameters to them are always clones of the original objects.
            const expectedTagParts = [ tag.tagType.parts[0], tag.tagType.parts[1], tag.tagType.parts[2], tag.tagType.parts[5] ];
            for (let i = 0; i < expectedTagParts.length; ++i) {
                const expectedTagPart = expectedTagParts[i];
                const expectedTagProperty = tag.properties[expectedTagPart.name];

                expect(initTagPropEditorSpies[i].calls.count()).toBe(1);
                const initArgs = initTagPropEditorSpies[i].calls.argsFor(0);
                expect(initArgs[0]).toEqual(expectedTagPart);
                expect(initArgs[0]).not.toBe(expectedTagPart);
                expect(initArgs[1]).toEqual(tag);
                expect(initArgs[1]).not.toBe(tag);
                expect(initArgs[2]).toEqual(expectedTagProperty);
                expect(initArgs[2]).not.toBe(expectedTagProperty);
                assertTagEditorContextsEqual(context, initArgs[3]);
                expect(initArgs[3]).not.toBe(context);

                expect(registerOnChangeSpies[i].calls.count()).toBe(1);
                expect(typeof registerOnChangeSpies[i].calls.argsFor(0)[0]).toBe('function');

                expect(writeChangedValuesSpies[i]).not.toHaveBeenCalled();
            }
        }),
    );

    it('editTag() disables the OK button at the beginning if a mandatory TagProperty is empty',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();
            const tag = getMockedTag();
            const context = getMockedTagEditorContext(tag);
            instance.tagEditorHost.editTag(tag, context);
            fixture.detectChanges();
            tick();

            const okButton = (<HTMLElement> fixture.nativeElement).querySelector('.ok-button button');
            const disabled = okButton.attributes.getNamedItem('disabled');
            expect(disabled).toBeTruthy();
        }),
    );

    it('editTag() enables the OK button at the beginning if no TagProperty is mandatory',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();
            const tag = getMockedTag();
            tag.tagType.parts[2].mandatory = false;
            const context = getMockedTagEditorContext(tag);
            instance.tagEditorHost.editTag(tag, context);
            fixture.detectChanges();
            tick();

            const okButton = (<HTMLElement> fixture.nativeElement).querySelector('.ok-button button');
            const disabled = okButton.attributes.getNamedItem('disabled');
            expect(disabled).toBeFalsy();
        }),
    );

    it('editTag() enables the OK button at the beginning if all mandatory TagProperties are already filled and resolves the promise correctly',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();
            const tag = getMockedTag();
            (tag.properties[tag.tagType.parts[2].keyword] as StringTagPartProperty).stringValue = 'Already filled';
            const context = getMockedTagEditorContext(tag);
            const result = instance.tagEditorHost.editTag(tag, context);
            fixture.detectChanges();
            tick();

            const okButton: HTMLElement = fixture.nativeElement.querySelector('.ok-button button');
            const disabled = okButton.attributes.getNamedItem('disabled');
            expect(disabled).toBeFalsy();

            // Click the OK button and make sure that the promise resolves with the expected edits.
            let promiseResolved = false;
            result.then(editorResult => {
                expect(promiseResolved).toBe(false);
                expect(editorResult.doDelete).toEqual(false);
                expect(editorResult.tag).toEqual(tag);
                expect(editorResult.tag).not.toBe(tag);
                promiseResolved = true;
            }).catch(() => fail('The openTagEditor() promise should not be rejected'));
            tick();
            expect(promiseResolved).toBe(false);
            okButton.click();
            fixture.detectChanges();
            tick();
            expect(promiseResolved).toBe(true);
        }),
    );

    it('editTag() enables the OK button when all mandatory TagProperties have been filled and disables it again when that property is emptied',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();

            const registerOnChangeSpies: jasmine.Spy[] = [];
            spyOnDynamicallyCreatedComponent([TextTagPropertyEditor],
                (componentType, componentInstance: ComponentRef<TagPropertyEditor>) => {
                    registerOnChangeSpies.push(
                        spyOn(componentInstance.instance, 'registerOnChange').and.callThrough(),
                    );
                });

            const tag = getMockedTag();
            const context = getMockedTagEditorContext(tag);
            const mandatoryTagPart = tag.tagType.parts[2];
            const mandatoryTagProperty = tag.properties[mandatoryTagPart.keyword];
            instance.tagEditorHost.editTag(tag, context);
            fixture.detectChanges();
            tick();

            // OK button should be disabled at the beginning.
            const okButton = (<HTMLElement> fixture.nativeElement).querySelector('.ok-button button');
            let disabled = okButton.attributes.getNamedItem('disabled');
            expect(disabled).toBeTruthy();

            // Fill the mandatory TagProperty by firing the TagPropertiesChanged event.
            const onTagPropChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[2].calls.argsFor(0)[0];
            let changes: Partial<TagPropertyMap> = { };
            changes[mandatoryTagPart.keyword] = {
                ...mandatoryTagProperty,
                stringValue: 'Property filled',
            } as StringTagPartProperty;
            onTagPropChangeFn(changes);

            // OK button should be enabled now.
            fixture.detectChanges();
            tick();
            disabled = okButton.attributes.getNamedItem('disabled');
            expect(disabled).toBeFalsy();

            // Empty the mandatory TagProperty again.
            changes = { };
            changes[mandatoryTagPart.keyword] = {
                ...mandatoryTagProperty,
                stringValue: '',
            } as StringTagPartProperty;
            onTagPropChangeFn(changes);

            // OK button should again be disabled.
            fixture.detectChanges();
            tick();
            disabled = okButton.attributes.getNamedItem('disabled');
            expect(disabled).toBeTruthy();
        }),
    );

    it('throws an error when a TagPropertyEditor tries to edit a non-editable TagProperty',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();

            const registerOnChangeSpies: jasmine.Spy[] = [];
            spyOnDynamicallyCreatedComponent([TextTagPropertyEditor],
                (componentType, componentInstance: ComponentRef<TagPropertyEditor>) => {
                    registerOnChangeSpies.push(
                        spyOn(componentInstance.instance, 'registerOnChange').and.callThrough(),
                    );
                });

            const tag = getMockedTag();
            const context = getMockedTagEditorContext(tag);
            const nonEditableTagPart = tag.tagType.parts[3];
            const nonEditableTagProperty = tag.properties[nonEditableTagPart.keyword];
            instance.tagEditorHost.editTag(tag, context);
            fixture.detectChanges();
            tick();

            // Try to change the non-editable TagProperty by firing the TagPropertiesChanged event from another property.
            const onTagPropChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[0].calls.argsFor(0)[0];
            const changes: Partial<TagPropertyMap> = { };
            changes[nonEditableTagPart.keyword] = {
                ...nonEditableTagProperty,
                stringValue: 'Property changed',
            } as StringTagPartProperty;
            expect(() => onTagPropChangeFn(changes)).toThrow();
        }),
    );

    it('correctly validates and communicates changes by one TagPropertyEditor to all TagPropertyEditors ' +
        'and editTag() resolves the promise correctly on OK click',
    componentTest(() => TestComponent, (fixture, instance) => {
        fixture.detectChanges();

        const registerOnChangeSpies: jasmine.Spy[] = [];
        const writeChangedValuesSpies: jasmine.Spy[] = [];
        let validatorSpy: jasmine.Spy;
        spyOnDynamicallyCreatedComponent([GenticsTagEditorComponent, TextTagPropertyEditor],
            (componentType, componentInstance: ComponentRef<any>) => {
                if (componentType === TextTagPropertyEditor) {
                    registerOnChangeSpies.push(
                        spyOn(componentInstance.instance, 'registerOnChange').and.callThrough(),
                    );
                    writeChangedValuesSpies.push(
                        spyOn(componentInstance.instance, 'writeChangedValues').and.callThrough(),
                    );
                }
                if (componentType === GenticsTagEditorComponent) {
                    const tagEditor = (<GenticsTagEditorComponent> componentInstance.instance);
                    const origEditTag = tagEditor.editTag.bind(tagEditor);
                    spyOn(tagEditor, 'editTag').and.callFake((tag: EditableTag, context: TagEditorContext) => {
                        validatorSpy = spyOn(context.validator, 'validateTagProperty').and.callThrough();
                        return origEditTag(tag, context);
                    });
                }
            });

        const origTag = getMockedTag();
        const context = getMockedTagEditorContext(origTag);
        const origTagClone = cloneDeep(origTag);
        const expectedFinalTag = getMockedTag();
        const tagPart0Key = origTag.tagType.parts[0].keyword;
        const tagPart1Key = origTag.tagType.parts[1].keyword;
        const tagPart2Key = origTag.tagType.parts[2].keyword;
        const tagPart5Key = origTag.tagType.parts[5].keyword;
        (expectedFinalTag.properties[tagPart0Key] as StringTagPartProperty).stringValue = 'Changed value of tagProperty0';
        (expectedFinalTag.properties[tagPart1Key] as StringTagPartProperty).stringValue = 'Changed value of tagProperty1';
        (expectedFinalTag.properties[tagPart2Key] as StringTagPartProperty).stringValue = 'Changed value of tagProperty2';
        expect(expectedFinalTag).not.toEqual(origTag);

        const result = instance.tagEditorHost.editTag(origTag, context);
        fixture.detectChanges();
        tick();

        const onTagProp1ChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[1].calls.argsFor(0)[0];
        const onTagProp2ChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[2].calls.argsFor(0)[0];

        validatorSpy.calls.reset();

        // Change the value of tagProperty1.
        let change: Partial<TagPropertyMap> = { };
        change[tagPart1Key] = {
            ...expectedFinalTag.properties[tagPart1Key],
        };
        let changeClone = cloneDeep(change);
        let expectedValidationResults: MultiValidationResult = { };
        expectedValidationResults[tagPart1Key] = getValidationSuccess();
        let actualValidationResults = onTagProp1ChangeFn(change);
        fixture.detectChanges();
        tick();

        // Make sure that the change object has not been modified and that the validation has been performed
        // and the other TagPropertyEditors have been notified, but that source editor has not received a writeChangedValues() call.
        expect(change).toEqual(changeClone);
        expect(validatorSpy.calls.count()).toBe(1);
        expect(validatorSpy.calls.argsFor(0)[0]).toEqual(change[tagPart1Key]);
        expect(actualValidationResults).toEqual(expectedValidationResults);
        assertWriteChangedValuesSpiesCalled(
            [ writeChangedValuesSpies[0], writeChangedValuesSpies[2], writeChangedValuesSpies[3] ],
            change,
        );
        expect(writeChangedValuesSpies[1].calls.count()).toBe(0);
        resetSpies(writeChangedValuesSpies);
        validatorSpy.calls.reset();

        // Use TagPropertyEditor 2 to change tagProperty0 and tagProperty2.
        change = { };
        change[tagPart0Key] = {
            ...expectedFinalTag.properties[tagPart0Key],
        };
        change[tagPart2Key] = {
            ...expectedFinalTag.properties[tagPart2Key],
        };
        expectedValidationResults = { };
        expectedValidationResults[tagPart0Key] = getValidationSuccess();
        expectedValidationResults[tagPart2Key] = getValidationSuccess();
        changeClone = cloneDeep(change);
        actualValidationResults = onTagProp2ChangeFn(change);
        fixture.detectChanges();
        tick();

        // Make sure that the change object has not been modified and that the validation has been performed
        // and the other TagPropertyEditors have been notified, but that source editor has not received a writeChangedValues() call.
        expect(change).toEqual(changeClone);
        expect(validatorSpy.calls.count()).toBe(2);
        expect(validatorSpy.calls.argsFor(0)[0]).toEqual(change[tagPart0Key]);
        expect(validatorSpy.calls.argsFor(1)[0]).toEqual(change[tagPart2Key]);
        expect(actualValidationResults).toEqual(expectedValidationResults);
        assertWriteChangedValuesSpiesCalled(
            [ writeChangedValuesSpies[0], writeChangedValuesSpies[1], writeChangedValuesSpies[3] ],
            change,
        );
        expect(writeChangedValuesSpies[2].calls.count()).toBe(0);

        // Click the OK button and make sure that the promise resolves with the expected edits.
        let promiseResolved = false;
        result.then(editorResult => {
            expect(promiseResolved).toBe(false);
            expect(editorResult.doDelete).toEqual(false);
            expect(editorResult.tag).toEqual(expectedFinalTag);
            promiseResolved = true;
        }).catch(() => fail('The openTagEditor() promise should not be rejected'));
        tick();
        expect(promiseResolved).toBe(false);
        const okButton: HTMLElement = fixture.nativeElement.querySelector('.ok-button button');
        okButton.click();
        fixture.detectChanges();
        tick();
        expect(promiseResolved).toBe(true);
        expect(origTag).toEqual(origTagClone);
    }),
    );

    it('does not communicate changes that fail validation, editTag() disables the OK button, and re-enables it when all properties are valid',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();

            const registerOnChangeSpies: jasmine.Spy[] = [];
            const writeChangedValuesSpies: jasmine.Spy[] = [];
            let validatorSpy: jasmine.Spy;
            spyOnDynamicallyCreatedComponent([GenticsTagEditorComponent, TextTagPropertyEditor],
                (componentType, componentInstance: ComponentRef<any>) => {
                    if (componentType === TextTagPropertyEditor) {
                        registerOnChangeSpies.push(
                            spyOn(componentInstance.instance, 'registerOnChange').and.callThrough(),
                        );
                        writeChangedValuesSpies.push(
                            spyOn(componentInstance.instance, 'writeChangedValues').and.callThrough(),
                        );
                    }
                    if (componentType === GenticsTagEditorComponent) {
                        const tagEditor = (<GenticsTagEditorComponent> componentInstance.instance);
                        const origEditTag = tagEditor.editTag.bind(tagEditor);
                        spyOn(tagEditor, 'editTag').and.callFake((tag: EditableTag, context: TagEditorContext) => {
                            validatorSpy = spyOn(context.validator, 'validateTagProperty').and.callThrough();
                            return origEditTag(tag, context);
                        });
                    }
                });

            const origTag = getMockedTag();
            const tagPart0Key = origTag.tagType.parts[0].keyword;
            const tagPart1Key = origTag.tagType.parts[1].keyword;
            const tagPart2Key = origTag.tagType.parts[2].keyword;
            const tagPart5Key = origTag.tagType.parts[5].keyword;
            (origTag.properties[tagPart2Key] as StringTagPartProperty).stringValue = 'value is set'; // Set the mandatory property.

            const context = getMockedTagEditorContext(origTag);
            const origTagClone = cloneDeep(origTag);
            const expectedFinalTag = cloneDeep(origTag);
            (expectedFinalTag.properties[tagPart0Key] as StringTagPartProperty).stringValue = 'Changed value of tagProperty0';
            (expectedFinalTag.properties[tagPart1Key] as StringTagPartProperty).stringValue = 'Changed value of tagProperty1';
            (expectedFinalTag.properties[tagPart2Key] as StringTagPartProperty).stringValue = 'Changed value of tagProperty2';
            expect(expectedFinalTag).not.toEqual(origTag);

            const result = instance.tagEditorHost.editTag(origTag, context);
            fixture.detectChanges();
            tick();

            const onTagProp1ChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[1].calls.argsFor(0)[0];
            const onTagProp2ChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[2].calls.argsFor(0)[0];

            validatorSpy.calls.reset();

            // Make sure that the OK button is enabled.
            const okButton: HTMLElement = fixture.nativeElement.querySelector('.ok-button button');
            let disabled = okButton.attributes.getNamedItem('disabled');
            expect(disabled).toBeFalsy();

            // Change the value of tagProperty1, but make the validation fail.
            let change: Partial<TagPropertyMap> = { };
            change[tagPart1Key] = {
                ...origTag.properties[tagPart1Key],
                stringValue: 'This will fail validation',
            } as StringTagPartProperty;
            let changeClone = cloneDeep(change);
            validatorSpy.and.returnValue(getValidationFailed());
            let expectedValidationResults: MultiValidationResult = { };
            expectedValidationResults[tagPart1Key] = getValidationFailed();
            let actualValidationResults = onTagProp1ChangeFn(change);
            fixture.detectChanges();
            tick();

            // Make sure that the change object has not been modified, that the validation has been performed,
            // and that no TagPropertyEditor has been notified, because the validation has failed.
            expect(change).toEqual(changeClone);
            expect(validatorSpy.calls.count()).toBe(1);
            expect(validatorSpy.calls.argsFor(0)[0]).toEqual(change[tagPart1Key]);
            expect(actualValidationResults).toEqual(expectedValidationResults);
            // eslint-disable-next-line @typescript-eslint/no-misused-promises
            writeChangedValuesSpies.forEach(spy => expect(spy.calls.count()).toBe(0));
            validatorSpy.calls.reset();

            // Make sure that the OK button has been disabled.
            disabled = okButton.attributes.getNamedItem('disabled');
            expect(disabled).toBeTruthy();

            // Use TagPropertyEditor 2 to change tagProperty0 (invalid change) and tagProperty2 (valid change).
            change = { };
            change[tagPart0Key] = {
                ...origTag.properties[tagPart0Key],
                stringValue: 'Another invalid change',
            } as StringTagPartProperty;
            change[tagPart2Key] = {
                ...expectedFinalTag.properties[tagPart2Key],
            };
            changeClone = cloneDeep(change);
            validatorSpy.and.callFake((tagProp: TagPartProperty) => {
                if (tagProp.partId === 0) {
                    return getValidationFailed();
                } else {
                    return getValidationSuccess();
                }
            });
            let expectedPropagatedChange = cloneDeep(change);
            delete expectedPropagatedChange[tagPart0Key];
            expectedValidationResults = { };
            expectedValidationResults[tagPart0Key] = getValidationFailed();
            expectedValidationResults[tagPart2Key] = getValidationSuccess();
            actualValidationResults = onTagProp2ChangeFn(change);
            fixture.detectChanges();
            tick();

            // Make sure that the change object has not been modified, that the validation has been performed,
            // the other TagPropertyEditors have been notified only with the change to tagProperty2,
            // and that the source editor has not received a writeChangedValues() call.
            expect(change).toEqual(changeClone);
            expect(validatorSpy.calls.count()).toBe(2);
            expect(validatorSpy.calls.argsFor(0)[0]).toEqual(change[tagPart0Key]);
            expect(validatorSpy.calls.argsFor(1)[0]).toEqual(change[tagPart2Key]);
            expect(actualValidationResults).toEqual(expectedValidationResults);
            assertWriteChangedValuesSpiesCalled(
                [ writeChangedValuesSpies[0], writeChangedValuesSpies[1], writeChangedValuesSpies[3] ],
                expectedPropagatedChange,
            );
            expect(writeChangedValuesSpies[2].calls.count()).toBe(0);
            resetSpies(writeChangedValuesSpies);
            validatorSpy.calls.reset();

            // Make sure that the OK button is still disabled.
            disabled = okButton.attributes.getNamedItem('disabled');
            expect(disabled).toBeTruthy();

            // Use TagPropertyEditor 1 to change the values of tagProperty0 and tagProperty1 again to valid values.
            change = { };
            change[tagPart0Key] = {
                ...expectedFinalTag.properties[tagPart0Key],
            };
            change[tagPart1Key] = {
                ...expectedFinalTag.properties[tagPart1Key],
            };
            changeClone = cloneDeep(change);
            expectedPropagatedChange = cloneDeep(change);
            validatorSpy.and.returnValue(getValidationSuccess());
            expectedValidationResults = { };
            expectedValidationResults[tagPart0Key] = getValidationSuccess();
            expectedValidationResults[tagPart1Key] = getValidationSuccess();
            actualValidationResults = onTagProp1ChangeFn(change);
            fixture.detectChanges();
            tick();

            // Make sure that the change object has not been modified, that the validation has been performed,
            // the other TagPropertyEditors have been notified with both changes,
            // and that the source editor has not received a writeChangedValues() call.
            expect(change).toEqual(changeClone);
            expect(validatorSpy.calls.count()).toBe(2);
            expect(validatorSpy.calls.argsFor(0)[0]).toEqual(change[tagPart0Key]);
            expect(validatorSpy.calls.argsFor(1)[0]).toEqual(change[tagPart1Key]);
            expect(actualValidationResults).toEqual(expectedValidationResults);
            assertWriteChangedValuesSpiesCalled(
                [ writeChangedValuesSpies[0], writeChangedValuesSpies[2], writeChangedValuesSpies[3] ],
                expectedPropagatedChange,
            );
            expect(writeChangedValuesSpies[1].calls.count()).toBe(0);

            // Make sure that the OK button is enabled again.
            disabled = okButton.attributes.getNamedItem('disabled');
            expect(disabled).toBeFalsy();

            // Click the OK button and make sure that the promise resolves with the expected edits.
            let promiseResolved = false;
            result.then(editorResult => {
                expect(promiseResolved).toBe(false);
                expect(editorResult.doDelete).toEqual(false);
                expect(editorResult.tag).toEqual(expectedFinalTag);
                promiseResolved = true;
            }).catch(() => fail('The openTagEditor() promise should not be rejected'));
            tick();
            expect(promiseResolved).toBe(false);
            okButton.click();
            fixture.detectChanges();
            tick();
            expect(promiseResolved).toBe(true);
            expect(origTag).toEqual(origTagClone);
        }),
    );

    it('editTag() rejects the promise correctly when Cancel is clicked',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();

            const registerOnChangeSpies: jasmine.Spy[] = [];
            const writeChangedValuesSpies: jasmine.Spy[] = [];
            spyOnDynamicallyCreatedComponent([TextTagPropertyEditor],
                (componentType, componentInstance: ComponentRef<any>) => {
                    registerOnChangeSpies.push(
                        spyOn(componentInstance.instance, 'registerOnChange').and.callThrough(),
                    );
                    writeChangedValuesSpies.push(
                        spyOn(componentInstance.instance, 'writeChangedValues').and.callThrough(),
                    );
                });

            const origTag = getMockedTag();
            const context = getMockedTagEditorContext(origTag);
            const origTagClone = cloneDeep(origTag);
            const tagPart1Key = origTag.tagType.parts[1].keyword;

            const result = instance.tagEditorHost.editTag(origTag, context);
            fixture.detectChanges();
            tick();

            const onTagProp1ChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[1].calls.argsFor(0)[0];

            // Change the value of tagProperty1.
            const change: Partial<TagPropertyMap> = { };
            change[tagPart1Key] = {
                ...origTag.properties[tagPart1Key],
                stringValue: 'modified value',
            } as StringTagPartProperty;
            const changeClone = cloneDeep(change);
            onTagProp1ChangeFn(change);
            fixture.detectChanges();
            tick();

            // Make sure that the change object has not been modified and that the other TagPropertyEditors
            // have been notified, but that source editor has not received a writeChangedValues() call.
            expect(change).toEqual(changeClone);
            assertWriteChangedValuesSpiesCalled(
                [ writeChangedValuesSpies[0], writeChangedValuesSpies[2] ],
                change,
            );
            expect(writeChangedValuesSpies[1].calls.count()).toBe(0);

            // Click the Cancel button and make sure that the promise rejects as expected
            let promiseRejected = false;
            result.then(() => fail('The openTagEditor() promise should not be resolved'))
                .catch(() => {
                    expect(promiseRejected).toBe(false);
                    promiseRejected = true;
                });
            tick();
            expect(promiseRejected).toBe(false);
            const cancelButton: HTMLElement = fixture.nativeElement.querySelector('.cancel-button button');
            cancelButton.click();
            fixture.detectChanges();
            tick();
            expect(promiseRejected).toBe(true);
            expect(origTag).toEqual(origTagClone);
        }),
    );

    it('prevents an onTagPropertyChanged() infinite loop',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();

            const errorHandler: ErrorHandler = TestBed.inject(ErrorHandler);
            const catchErrorSpy = spyOn(errorHandler, 'catch').and.callThrough();

            const registerOnChangeSpies: jasmine.Spy[] = [];
            const writeChangedValuesSpies: jasmine.Spy[] = [];
            spyOnDynamicallyCreatedComponent([TextTagPropertyEditor],
                (componentType, componentInstance: ComponentRef<any>) => {
                    registerOnChangeSpies.push(
                        spyOn(componentInstance.instance, 'registerOnChange').and.callThrough(),
                    );
                    writeChangedValuesSpies.push(
                        spyOn(componentInstance.instance, 'writeChangedValues').and.callThrough(),
                    );
                });

            const origTag = getMockedTag();
            const context = getMockedTagEditorContext(origTag);
            const tagPart0Key = origTag.tagType.parts[0].keyword;
            const tagPart1Key = origTag.tagType.parts[1].keyword;

            instance.tagEditorHost.editTag(origTag, context);
            fixture.detectChanges();
            tick();

            resetSpies(writeChangedValuesSpies);
            const onTagProp0ChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[0].calls.argsFor(0)[0];
            const onTagProp1ChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[1].calls.argsFor(0)[0];

            // Configure TagPropertyEditor0 and TagPropertyEditor1 to trigger an infinite loop when writeChangedValues() is called.
            writeChangedValuesSpies[0].and.callFake((changes: Partial<TagPropertyMap>) => {
                if (writeChangedValuesSpies[0].calls.count() > 1) {
                    fail('onTagPropertyChanged() infinite loop prevention does not work');
                }
                const newChange: Partial<TagPropertyMap> = { };
                newChange[tagPart0Key] = {
                    ...origTag.properties[tagPart0Key],
                    stringValue: 'Changed from writeChangedValues()',
                } as StringTagPartProperty;
                onTagProp0ChangeFn(newChange);
            });
            writeChangedValuesSpies[1].and.callFake((changes: Partial<TagPropertyMap>) => {
                const newChange: Partial<TagPropertyMap> = { };
                newChange[tagPart1Key] = {
                    ...origTag.properties[tagPart1Key],
                    stringValue: 'Changed from writeChangedValues()',
                } as StringTagPartProperty;
                onTagProp1ChangeFn(newChange);
            });

            // Change the value of tagProperty1.
            const change: Partial<TagPropertyMap> = { };
            change[tagPart1Key] = {
                ...origTag.properties[tagPart1Key],
                stringValue: 'modified value',
            } as StringTagPartProperty;
            onTagProp1ChangeFn(change);
            expect(catchErrorSpy).toHaveBeenCalledTimes(1);
        }),
    );

    it('prevents an onTagPropertyChanged() call during initialization',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();

            const errorHandler: ErrorHandler = TestBed.inject(ErrorHandler);
            const catchErrorSpy = spyOn(errorHandler, 'catch').and.callThrough();

            const tag = getMockedTag();
            const context = getMockedTagEditorContext(tag);

            // The registerOnChange() method of TagPropertyEditor 2 should trigger onTagPropertyChanged()
            let currPropEditorIndex = 0;
            spyOnDynamicallyCreatedComponent([TextTagPropertyEditor],
                (componentType, componentInstance: ComponentRef<any>) => {
                    const index = currPropEditorIndex;
                    let onChangeSpy = spyOn(componentInstance.instance, 'registerOnChange');

                    const writeChangedValuesSpy = spyOn(componentInstance.instance, 'writeChangedValues').and.callFake(() => {
                        if (writeChangedValuesSpy.calls.count() > 1) {
                            fail('onTagPropertyChanged() call should not be allowed during initialization');
                        }
                    });

                    if (index !== 2) {
                        onChangeSpy = onChangeSpy.and.callThrough();
                    } else {
                        onChangeSpy = onChangeSpy.and.callFake((onChangeFn: TagPropertiesChangedFn) => {
                            const change: Partial<TagPropertyMap> = { };
                            change[tag.tagType.parts[index].keyword] = {
                                ...tag.properties[tag.tagType.parts[index].keyword],
                                stringValue: 'Changed from registerOnChange()',
                            } as StringTagPartProperty;
                            onChangeFn(change);
                        });
                    }
                    ++currPropEditorIndex;
                });

            instance.tagEditorHost.editTag(tag, context);
            fixture.detectChanges();
            tick();
            expect(catchErrorSpy).toHaveBeenCalledTimes(1);
        }),
    );

    it('catches errors thrown by a TagPropertyEditor during initialization',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();

            const errorHandler: ErrorHandler = TestBed.inject(ErrorHandler);
            const catchErrorSpy = spyOn(errorHandler, 'catch').and.callThrough();
            const expectedError = new Error('error during init');

            let initSpy: jasmine.Spy;
            spyOnDynamicallyCreatedComponent([TextTagPropertyEditor],
                (componentType, componentInstance: ComponentRef<any>) => {
                    if (!initSpy) {
                        initSpy = spyOn(componentInstance.instance, 'initTagPropertyEditor').and
                            .callFake(() => { throw expectedError; });
                    }
                });

            const origTag = getMockedTag();
            const context = getMockedTagEditorContext(origTag);

            instance.tagEditorHost.editTag(origTag, context);
            expect(() => {
                fixture.detectChanges();
                tick();
            }).not.toThrow();
            expect(catchErrorSpy).toHaveBeenCalledWith(expectedError, { notification: true });
        }),
    );

    it('editTagLive() calls onTagChangeFn with the current properties after every valid change', componentTest(() => TestComponent, (fixture, instance) => {
        const origTag = getMockedTag();
        const context = getMockedTagEditorContext(origTag);
        const tagPart0Key = origTag.tagType.parts[0].keyword;
        const tagPart1Key = origTag.tagType.parts[1].keyword;
        const tagPart2Key = origTag.tagType.parts[2].keyword;

        // Set up the changes
        const propertiesAfterChange0 = cloneDeep(origTag.properties);
        (propertiesAfterChange0[tagPart1Key] as StringTagPartProperty).stringValue = 'Changed value of tagProperty0';
        const propertiesAfterChange1 = cloneDeep(propertiesAfterChange0);
        (propertiesAfterChange1[tagPart0Key] as StringTagPartProperty).stringValue = 'Changed value of tagProperty1';
        (propertiesAfterChange1[tagPart2Key] as StringTagPartProperty).stringValue = 'Changed value of tagProperty2';

        const registerOnChangeSpies: jasmine.Spy[] = [];
        spyOnDynamicallyCreatedComponent([GenticsTagEditorComponent, TextTagPropertyEditor],
            (componentType, componentInstance: ComponentRef<any>) => {
                if (componentType === TextTagPropertyEditor) {
                    registerOnChangeSpies.push(
                        spyOn(componentInstance.instance, 'registerOnChange').and.callThrough(),
                    );
                }
                if (componentType === GenticsTagEditorComponent) {
                    const tagEditor = (<GenticsTagEditorComponent> componentInstance.instance);
                    const origEditTagLive = tagEditor.editTagLive.bind(tagEditor);
                    spyOn(tagEditor, 'editTagLive').and.callFake((tag: EditableTag, context: TagEditorContext, onTagChangeFn: TagChangedFn) => {
                        spyOn(context.validator, 'validateTagProperty').and.returnValue(getValidationSuccess());
                        return origEditTagLive(tag, context, onTagChangeFn);
                    });
                }
            });

        const reportedChangedStates: TagPropertyMap[] = [];
        const onTagChangeHandler: TagChangedFn = (tagProperties) => reportedChangedStates.push(tagProperties);
        instance.tagEditorHost.editTagLive(origTag, context, onTagChangeHandler);
        fixture.detectChanges();
        tick();

        const onTagProp1ChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[1].calls.argsFor(0)[0];
        const onTagProp2ChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[2].calls.argsFor(0)[0];

        // Change the value of tagProperty1.
        let change: Partial<TagPropertyMap> = { };
        change[tagPart1Key] = {
            ...propertiesAfterChange0[tagPart1Key],
        };
        onTagProp1ChangeFn(change);
        fixture.detectChanges();
        tick();
        expect(reportedChangedStates.length).toBe(1);
        expect(reportedChangedStates[0]).toEqual(propertiesAfterChange0);

        // Use TagPropertyEditor 2 to change tagProperty0 and tagProperty2.
        change = { };
        change[tagPart0Key] = {
            ...propertiesAfterChange1[tagPart0Key],
        };
        change[tagPart2Key] = {
            ...propertiesAfterChange1[tagPart2Key],
        };
        onTagProp2ChangeFn(change);
        fixture.detectChanges();
        tick();
        expect(reportedChangedStates.length).toBe(2);
        expect(reportedChangedStates[1]).toEqual(propertiesAfterChange1);
    }));

    it('editTagLive() calls onTagChangeFn with null after an invalid change', componentTest(() => TestComponent, (fixture, instance) => {
        const origTag = getMockedTag();
        const context = getMockedTagEditorContext(origTag);
        const tagPart0Key = origTag.tagType.parts[0].keyword;
        const tagPart1Key = origTag.tagType.parts[1].keyword;

        // Set up the changes
        const propertiesAfterInvalidChange = cloneDeep(origTag.properties);
        (propertiesAfterInvalidChange[tagPart0Key] as StringTagPartProperty).stringValue = 'invalid change';
        const propertiesAfterValidChange0 = cloneDeep(propertiesAfterInvalidChange);
        (propertiesAfterValidChange0[tagPart1Key] as StringTagPartProperty).stringValue = 'Changed value of tagProperty1';
        const propertiesAfterValidChange1 = cloneDeep(propertiesAfterValidChange0);
        (propertiesAfterValidChange1[tagPart0Key] as StringTagPartProperty).stringValue = 'Valid change of tagProperty0';

        const registerOnChangeSpies: jasmine.Spy[] = [];
        let validatorSpy: jasmine.Spy;
        spyOnDynamicallyCreatedComponent([GenticsTagEditorComponent, TextTagPropertyEditor],
            (componentType, componentInstance: ComponentRef<any>) => {
                if (componentType === TextTagPropertyEditor) {
                    registerOnChangeSpies.push(
                        spyOn(componentInstance.instance, 'registerOnChange').and.callThrough(),
                    );
                }
                if (componentType === GenticsTagEditorComponent) {
                    const tagEditor = (<GenticsTagEditorComponent> componentInstance.instance);
                    const origEditTagLive = tagEditor.editTagLive.bind(tagEditor);
                    spyOn(tagEditor, 'editTagLive').and.callFake((tag: EditableTag, context: TagEditorContext, onTagChangeFn: TagChangedFn) => {
                        validatorSpy = spyOn(context.validator, 'validateTagProperty').and.returnValue(getValidationSuccess());
                        return origEditTagLive(tag, context, onTagChangeFn);
                    });
                }
            });

        const reportedChangedStates: TagPropertyMap[] = [];
        const onChangeHandler: TagChangedFn = (tagProperties) => reportedChangedStates.push(tagProperties);
        instance.tagEditorHost.editTagLive(origTag, context, onChangeHandler);
        fixture.detectChanges();
        tick();

        const onTagProp0ChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[0].calls.argsFor(0)[0];
        const onTagProp1ChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[1].calls.argsFor(0)[0];

        // Simulate an invalid change of property0.
        validatorSpy.and.returnValue(getValidationFailed());
        let change: Partial<TagPropertyMap> = { };
        change[tagPart0Key] = {
            ...propertiesAfterInvalidChange[tagPart0Key],
        };
        onTagProp0ChangeFn(change);
        fixture.detectChanges();
        tick();
        expect(reportedChangedStates.length).toBe(1);
        expect(reportedChangedStates[0]).toBeNull();

        // Simulate a valid change of property1.
        // Since property0 is still invalid, we expect null to be emitted by the onTagChangeFn.
        validatorSpy.and.returnValue(getValidationSuccess());
        change = { };
        change[tagPart1Key] = {
            ...propertiesAfterValidChange0[tagPart1Key],
        };
        onTagProp1ChangeFn(change);
        fixture.detectChanges();
        tick();
        expect(reportedChangedStates.length).toBe(2);
        expect(reportedChangedStates[1]).toBeNull();

        // Correct the value of property0, now we should get the current properties state.
        change = { };
        change[tagPart0Key] = {
            ...propertiesAfterValidChange1[tagPart0Key],
        };
        onTagProp0ChangeFn(change);
        fixture.detectChanges();
        tick();
        expect(reportedChangedStates.length).toBe(3);
        expect(reportedChangedStates[2]).toEqual(propertiesAfterValidChange1);
    }));

    it('editTagLive() does not report changes that did not modify anything', componentTest(() => TestComponent, (fixture, instance) => {
        const origTag = getMockedTag();
        const context = getMockedTagEditorContext(origTag);
        const tagPart0Key = origTag.tagType.parts[0].keyword;
        const tagPart1Key = origTag.tagType.parts[1].keyword;
        const tagPart2Key = origTag.tagType.parts[2].keyword;

        // Set up the changes
        const propertiesAfterNoChange = cloneDeep(origTag.properties);
        const propertiesAfterRealChange = cloneDeep(propertiesAfterNoChange);
        (propertiesAfterRealChange[tagPart2Key] as StringTagPartProperty).stringValue = 'Changed value of tagProperty2';

        const registerOnChangeSpies: jasmine.Spy[] = [];
        spyOnDynamicallyCreatedComponent([GenticsTagEditorComponent, TextTagPropertyEditor],
            (componentType, componentInstance: ComponentRef<any>) => {
                if (componentType === TextTagPropertyEditor) {
                    registerOnChangeSpies.push(
                        spyOn(componentInstance.instance, 'registerOnChange').and.callThrough(),
                    );
                }
                if (componentType === GenticsTagEditorComponent) {
                    const tagEditor = (<GenticsTagEditorComponent> componentInstance.instance);
                    const origEditTagLive = tagEditor.editTagLive.bind(tagEditor);
                    spyOn(tagEditor, 'editTagLive').and.callFake((tag: EditableTag, context: TagEditorContext, onTagChangeFn: TagChangedFn) => {
                        spyOn(context.validator, 'validateTagProperty').and.returnValue(getValidationSuccess());
                        return origEditTagLive(tag, context, onTagChangeFn);
                    });
                }
            });

        const reportedChangedStates: TagPropertyMap[] = [];
        const onTagChangeHandler: TagChangedFn = (tagProperties) => reportedChangedStates.push(tagProperties);
        instance.tagEditorHost.editTagLive(origTag, context, onTagChangeHandler);
        fixture.detectChanges();
        tick();

        const onTagProp1ChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[1].calls.argsFor(0)[0];
        const onTagProp2ChangeFn: TagPropertiesChangedFn = registerOnChangeSpies[2].calls.argsFor(0)[0];

        // Signal a change of tagProperty1, which does not actually modify anything.
        // onTagChangeFn should not be called.
        let change: Partial<TagPropertyMap> = { };
        change[tagPart1Key] = {
            // This is the same as the initial value.
            ...propertiesAfterNoChange[tagPart1Key],
        };
        onTagProp1ChangeFn(change);
        fixture.detectChanges();
        tick();
        expect(reportedChangedStates.length).toBe(0, 'onTagChangeFn reported a change that did not modify any values.');

        // Use TagPropertyEditor 2 to change tagProperty0 (no real change) and tagProperty2 (actually changed value).
        // This time onTagChangeFn should be called.
        change = { };
        change[tagPart0Key] = {
            // This is the same as the initial value.
            ...propertiesAfterNoChange[tagPart0Key],
        };
        change[tagPart2Key] = {
            ...propertiesAfterRealChange[tagPart2Key],
        };
        onTagProp2ChangeFn(change);
        fixture.detectChanges();
        tick();
        expect(reportedChangedStates.length).toBe(1);
        expect(reportedChangedStates[0]).toEqual(propertiesAfterRealChange);
    }));

});

function assertWriteChangedValuesSpiesCalled(spies: jasmine.Spy[], expectedChange: Partial<TagPropertyMap>): void {
    spies.forEach(writeChangedValuesSpy => {
        expect(writeChangedValuesSpy.calls.count()).toBe(1);
        expect(writeChangedValuesSpy.calls.argsFor(0)[0]).toEqual(expectedChange);
        expect(writeChangedValuesSpy.calls.argsFor(0)[0]).not.toBe(expectedChange);
    });
}

function resetSpies(spies: jasmine.Spy[]): void {
    spies.forEach(spy => spy.calls.reset());
}

function getMockedTag(): EditableTag {
    const tag = getExampleEditableTag();
    validateMockedTag(tag);
    return tag;
}

const getValidationSuccess = (): ValidationResult => ({ isSet: true, success: true });

const getValidationFailed = (): ValidationResult => ({ isSet: true, success: false, errorMessage: 'Validation error' });

/**
 * Verifies that the specified tag has the properties set, which are expected by the tests.
 * This is used as a guard against unexpected modifications of the mocked tag.
 */
function validateMockedTag(tag: EditableTag): void {
    const propertyKeys = Object.keys(tag.properties);
    expect(tag.tagType.parts.length).toBe(6);
    expect(propertyKeys.length).toBe(tag.tagType.parts.length);

    expect(tag.tagType.parts[0].editable).toBe(true);
    expect(tag.tagType.parts[0].hidden).toBe(false);
    expect(tag.tagType.parts[0].hideInEditor).toBe(false);
    expect(tag.tagType.parts[0].mandatory).toBe(false);
    expect(tag.tagType.parts[0].type).toBe(TagPropertyType.STRING);
    expect((tag.properties[tag.tagType.parts[0].name] as StringTagPartProperty).stringValue).toBe('');

    expect(tag.tagType.parts[1].editable).toBe(true);
    expect(tag.tagType.parts[1].hidden).toBe(false);
    expect(tag.tagType.parts[1].hideInEditor).toBe(false);
    expect(tag.tagType.parts[1].mandatory).toBe(false);
    expect(tag.tagType.parts[1].type).toBe(TagPropertyType.STRING);
    expect((tag.properties[tag.tagType.parts[1].name] as StringTagPartProperty).stringValue).toBe('');

    expect(tag.tagType.parts[2].editable).toBe(true);
    expect(tag.tagType.parts[2].hidden).toBe(false);
    expect(tag.tagType.parts[2].hideInEditor).toBe(false);
    expect(tag.tagType.parts[2].mandatory).toBe(true);
    expect(tag.tagType.parts[2].type).toBe(TagPropertyType.STRING);
    expect((tag.properties[tag.tagType.parts[2].name] as StringTagPartProperty).stringValue).toBe('');

    expect(tag.tagType.parts[3].editable).toBe(false);
    expect(tag.tagType.parts[3].hidden).toBe(false);
    expect(tag.tagType.parts[3].hideInEditor).toBe(false);
    expect(tag.tagType.parts[3].mandatory).toBe(false);
    expect(tag.tagType.parts[3].type).toBe(TagPropertyType.STRING);
    expect((tag.properties[tag.tagType.parts[3].name] as StringTagPartProperty).stringValue).toBe('');

    expect(tag.tagType.parts[4].editable).toBe(true);
    expect(tag.tagType.parts[4].hidden).toBe(false);
    expect(tag.tagType.parts[4].hideInEditor).toBe(true);
    expect(tag.tagType.parts[4].mandatory).toBe(false);
    expect(tag.tagType.parts[4].type).toBe(TagPropertyType.STRING);
    expect((tag.properties[tag.tagType.parts[4].name] as StringTagPartProperty).stringValue).toBe('');

    expect(tag.tagType.parts[5].editable).toBe(true);
    expect(tag.tagType.parts[5].hidden).toBe(true);
    expect(tag.tagType.parts[5].hideInEditor).toBe(false);
    expect(tag.tagType.parts[5].mandatory).toBe(false);
    expect(tag.tagType.parts[5].type).toBe(TagPropertyType.STRING);
    expect((tag.properties[tag.tagType.parts[0].name] as StringTagPartProperty).stringValue).toBe('');
}

/**
 * We don't add the GenticsTagEditor directly to the template, but instead have it
 * created dynamically just like in the real use cases.
 */
@Component({
    template: `
        <tag-editor-host #tagEditorHost></tag-editor-host>
    `,
    standalone: false,
})
class TestComponent {
    @ViewChild('tagEditorHost', { static: true })
    tagEditorHost: TagEditorHostComponent;
}

class MockTagEditorService {
    registerTagEditorHost(tagEditorHost: TagEditorHostComponent): void { }
    unregisterTagEditorHost(tagEditorHost: TagEditorHostComponent): void { }
}

class MockErrorHandlerService {
    catch(error: Error, options?: { notification: boolean }): void { }
}

class MockI18nService implements Partial<TranslateService> {
    instant(key: string | string[], params?: any): string {
        return key as string;
    }
}
