import { Component, ViewChild } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserDynamicTestingModule } from '@angular/platform-browser-dynamic/testing';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { EditableTag, StringTagPartProperty, TagChangedFn, TagPropertyMap } from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { cloneDeep } from 'lodash-es';
import { componentTest } from '../../../../testing/component-test';
import { configureComponentTest } from '../../../../testing/configure-component-test';
import { spyOnDynamicallyCreatedComponent } from '../../../../testing/dynamic-components';
import { mockPipes } from '../../../../testing/mock-pipe';
import { getExampleEditableTag, getMockedTagEditorContext } from '../../../../testing/test-tag-editor-data.mock';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { UserAgentRef } from '../../../shared/providers/user-agent-ref';
import { assertTagEditorContextsEqual } from '../../common/impl/tag-editor-context.spec';
import { IFrameStylesService } from '../../providers/iframe-styles/iframe-styles.service';
import { CustomTagEditorHostComponent } from '../custom-tag-editor-host/custom-tag-editor-host.component';
import { GenticsTagEditorComponent } from '../gentics-tag-editor/gentics-tag-editor.component';
import { IFrameWrapperComponent } from '../iframe-wrapper/iframe-wrapper.component';
import { TagPropertyEditorHostComponent } from '../tag-property-editor-host/tag-property-editor-host.component';
import { TagEditorHostComponent } from './tag-editor-host.component';

describe('TagEditorHostComponent', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [GenticsUICoreModule.forRoot()],
            providers: [
                { provide: ErrorHandler, useClass: MockErrorHandlerService },
                IFrameStylesService,
                UserAgentRef,
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
            declarations: [
                CustomTagEditorHostComponent,
                GenticsTagEditorComponent,
                IFrameWrapperComponent,
                TagEditorHostComponent,
                TagPropertyEditorHostComponent,
                TestComponent,
                mockPipes('objTagName'),
            ],
        });
        TestBed.overrideModule(BrowserDynamicTestingModule, {
            set: {
                entryComponents: [
                    CustomTagEditorHostComponent,
                    GenticsTagEditorComponent,
                ],
            },
        });
    });

    describe('editTag()', () => {

        it('creates and shows the GenticsTagEditor and passes on the result\'s resolve(), and destroys the TagEditor again',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getExampleEditableTag();
                const expectedEditedTag = getExampleEditableTag();
                (expectedEditedTag.properties['property0'] as StringTagPartProperty).stringValue = 'modified Value';
                const context = getMockedTagEditorContext(tag);

                let editTagSpy: jasmine.Spy = null;
                let resolve: (tag: EditableTag) => void = null;
                spyOnDynamicallyCreatedComponent([GenticsTagEditorComponent], (componentType, componentInstance) => {
                    editTagSpy = spyOn(componentInstance.instance, 'editTag').and.returnValue(
                        new Promise(resolveFn => resolve = resolveFn),
                    );
                });

                fixture.detectChanges();
                const result = fixture.componentInstance.tagEditorHost.editTag(tag, context);

                // Make sure that the TagEditor's editTag() method has been called appropriately.
                expect(editTagSpy.calls.argsFor(0)[0]).toEqual(tag);
                assertTagEditorContextsEqual(context, editTagSpy.calls.argsFor(0)[1]);
                expect(result instanceof Promise).toBe(true);

                // Make sure that the TagEditor is actually displayed.
                fixture.detectChanges();
                tick();
                let tagEditor = fixture.debugElement.query(By.directive(GenticsTagEditorComponent));
                expect(tagEditor).toBeTruthy();

                // Make sure that the promise is resolved correctly.
                let origPromiseResolved = false;
                let resultResolved = false;
                result.then(actualEditedTag => {
                    expect(origPromiseResolved).toBe(true);
                    expect(actualEditedTag).toEqual(expectedEditedTag);
                    resultResolved = true;
                }).catch(() => fail('result promise should not be rejected.'));
                resolve(expectedEditedTag);
                origPromiseResolved = true;
                tick();
                expect(resultResolved).toBe(true);

                // Make sure that the TagEditor has been destroyed again.
                fixture.detectChanges();
                tick();
                tagEditor = fixture.debugElement.query(By.directive(GenticsTagEditorComponent));
                expect(tagEditor).toBeFalsy();
            }),
        );

        it('creates and shows the CustomTagEditorHostComponent and passes on the result\'s resolve(), and destroys the TagEditor again',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getExampleEditableTag();
                tag.tagType.externalEditorUrl = 'http://localhost/customTagEditor';
                const expectedEditedTag = getExampleEditableTag();
                expectedEditedTag.tagType.externalEditorUrl = tag.tagType.externalEditorUrl;
                (expectedEditedTag.properties['property0'] as StringTagPartProperty).stringValue = 'modified Value';
                const context = getMockedTagEditorContext(tag);

                let editTagSpy: jasmine.Spy = null;
                let resolve: (tag: EditableTag) => void = null;
                spyOnDynamicallyCreatedComponent([CustomTagEditorHostComponent], (componentType, componentInstance) => {
                    editTagSpy = spyOn(componentInstance.instance, 'editTag').and.returnValue(
                        new Promise(resolveFn => resolve = resolveFn),
                    );
                });

                fixture.detectChanges();
                const result = fixture.componentInstance.tagEditorHost.editTag(tag, context);

                // Make sure that the TagEditor's editTag() method has been called appropriately.
                expect(editTagSpy.calls.argsFor(0)[0]).toEqual(tag);
                assertTagEditorContextsEqual(context, editTagSpy.calls.argsFor(0)[1]);
                expect(result instanceof Promise).toBe(true);

                // Make sure that the TagEditor is actually displayed.
                fixture.detectChanges();
                tick();
                let tagEditor = fixture.debugElement.query(By.directive(CustomTagEditorHostComponent));
                expect(tagEditor).toBeTruthy();

                // Make sure that the promise is resolved correctly.
                let origPromiseResolved = false;
                let resultResolved = false;
                result.then(actualEditedTag => {
                    expect(origPromiseResolved).toBe(true);
                    expect(actualEditedTag).toEqual(expectedEditedTag);
                    resultResolved = true;
                }).catch(() => fail('result promise should not be rejected.'));
                resolve(expectedEditedTag);
                origPromiseResolved = true;
                tick();
                expect(resultResolved).toBe(true);

                // Make sure that the TagEditor has been destroyed again.
                fixture.detectChanges();
                tick();
                tagEditor = fixture.debugElement.query(By.directive(CustomTagEditorHostComponent));
                expect(tagEditor).toBeFalsy();
            }),
        );

        it('creates and shows the GenticsTagEditor, passes on the result\'s reject(), and destroys the TagEditor again',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getExampleEditableTag();
                const context = getMockedTagEditorContext(tag);

                let editTagSpy: jasmine.Spy = null;
                let reject: (error?: any) => void = null;
                spyOnDynamicallyCreatedComponent([GenticsTagEditorComponent], (componentType, componentInstance) => {
                    editTagSpy = spyOn(componentInstance.instance, 'editTag').and.returnValue(
                        new Promise((resolveFn, rejectFn) => reject = rejectFn),
                    );
                });

                fixture.detectChanges();
                const result = fixture.componentInstance.tagEditorHost.editTag(tag, context);

                // Make sure that the TagEditor's editTag() method has been called appropriately.
                expect(editTagSpy.calls.argsFor(0)[0]).toEqual(tag);
                assertTagEditorContextsEqual(context, editTagSpy.calls.argsFor(0)[1]);
                expect(result instanceof Promise).toBe(true);

                // Make sure that the TagEditor is actually displayed.
                fixture.detectChanges();
                tick();
                let tagEditor = fixture.debugElement.query(By.directive(GenticsTagEditorComponent));
                expect(tagEditor).toBeTruthy();

                // Make sure that the promise is rejected correctly.
                let origPromiseRejected = false;
                let resultRejected = false;
                result.then(() => fail('result promise should not be resolved'))
                    .catch(() => {
                        expect(origPromiseRejected).toBe(true);
                        resultRejected = true;
                    });
                reject();
                origPromiseRejected = true;
                tick();
                expect(resultRejected).toBe(true);

                // Make sure that the TagEditor has been destroyed again.
                fixture.detectChanges();
                tick();
                tagEditor = fixture.debugElement.query(By.directive(GenticsTagEditorComponent));
                expect(tagEditor).toBeFalsy();
            }),
        );

        it('creates and shows the GenticsTagEditor in read-only mode, rejects the promise if the TagEditor resolves it, and destroys the TagEditor again',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getExampleEditableTag();
                const editedTag = getExampleEditableTag();
                (editedTag.properties['property0'] as StringTagPartProperty).stringValue = 'modified Value';
                const context = getMockedTagEditorContext(tag);
                context.readOnly = true;

                let editTagSpy: jasmine.Spy = null;
                let resolve: (error?: any) => void = null;
                spyOnDynamicallyCreatedComponent([GenticsTagEditorComponent], (componentType, componentInstance) => {
                    editTagSpy = spyOn(componentInstance.instance, 'editTag').and.returnValue(
                        new Promise((resolveFn, rejectFn) => resolve = resolveFn),
                    );
                });

                fixture.detectChanges();
                const result = fixture.componentInstance.tagEditorHost.editTag(tag, context);

                // Make sure that the TagEditor's editTag() method has been called appropriately.
                expect(editTagSpy.calls.argsFor(0)[0]).toEqual(tag);
                assertTagEditorContextsEqual(context, editTagSpy.calls.argsFor(0)[1]);
                expect(result instanceof Promise).toBe(true);

                // Make sure that the TagEditor is actually displayed.
                fixture.detectChanges();
                tick();
                let tagEditor = fixture.debugElement.query(By.directive(GenticsTagEditorComponent));
                expect(tagEditor).toBeTruthy();

                // Make sure that the promise is rejected correctly.
                let origPromiseResolved = false;
                let resultRejected = false;
                result.then(() => fail('result promise should not be resolved, because context.readOnly=true'))
                    .catch(() => {
                        expect(origPromiseResolved).toBe(true);
                        resultRejected = true;
                    });
                resolve(editedTag);
                origPromiseResolved = true;
                tick();
                expect(resultRejected).toBe(true);

                // Make sure that the TagEditor has been destroyed again.
                fixture.detectChanges();
                tick();
                tagEditor = fixture.debugElement.query(By.directive(GenticsTagEditorComponent));
                expect(tagEditor).toBeFalsy();
            }),
        );
    });

    describe('editTagLive()', () => {

        function testOnChangeFnCalls(tag: EditableTag, onChangeFn: TagChangedFn, reportedChangedStates: TagPropertyMap[]): void {
            // Set up the changes.
            const changedProperties = cloneDeep(tag.properties);
            (changedProperties['property0'] as StringTagPartProperty).stringValue = 'modified value0';
            const changedState0 = cloneDeep(changedProperties);
            const changedState1 = null;
            (changedProperties['property1'] as StringTagPartProperty).stringValue = 'modified value1';
            const changedState2 = cloneDeep(changedProperties);

            // Make sure that the onChangeFn calls are passed on correctly.
            onChangeFn(changedState0);
            expect(reportedChangedStates.length).toBe(1);
            expect(reportedChangedStates[0]).toBe(changedState0);

            onChangeFn(changedState1);
            expect(reportedChangedStates.length).toBe(2);
            expect(reportedChangedStates[1]).toBe(changedState1);

            onChangeFn(changedState2);
            expect(reportedChangedStates.length).toBe(3);
            expect(reportedChangedStates[2]).toBe(changedState2);
        }

        it('creates and shows the GenticsTagEditor and passes on onChangeFn calls',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getExampleEditableTag();
                const context = getMockedTagEditorContext(tag);

                let editTagLiveSpy: jasmine.Spy = null;
                let onChangeFn: TagChangedFn;
                spyOnDynamicallyCreatedComponent([GenticsTagEditorComponent], (componentType, componentInstance) => {
                    editTagLiveSpy = spyOn(componentInstance.instance, 'editTagLive').and
                        .callFake((tag, context, changeFn) => onChangeFn = changeFn);
                });

                const reportedChangedStates: TagPropertyMap[] = [];
                const onChangeHandler: TagChangedFn = (tagProperties) => reportedChangedStates.push(tagProperties)

                fixture.detectChanges();
                fixture.componentInstance.tagEditorHost.editTagLive(tag, context, onChangeHandler);

                // Make sure that the TagEditor's editTagLive() method has been called appropriately.
                expect(editTagLiveSpy.calls.argsFor(0)[0]).toEqual(tag);
                assertTagEditorContextsEqual(context, editTagLiveSpy.calls.argsFor(0)[1]);
                expect(editTagLiveSpy.calls.argsFor(0)[2] instanceof Function).toBeTruthy();

                // Make sure that the TagEditor is actually displayed.
                fixture.detectChanges();
                tick();
                let tagEditor = fixture.debugElement.query(By.directive(GenticsTagEditorComponent));
                expect(tagEditor).toBeTruthy();

                testOnChangeFnCalls(tag, onChangeFn, reportedChangedStates);
            }),
        );

        it('creates and shows the CustomTagEditorHostComponent and passes on onChangeFn calls that pass validation',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag = getExampleEditableTag();
                tag.tagType.externalEditorUrl = 'http://localhost/customTagEditor';
                const context = getMockedTagEditorContext(tag);

                let editTagLiveSpy: jasmine.Spy = null;
                let onChangeFn: TagChangedFn;
                spyOnDynamicallyCreatedComponent([CustomTagEditorHostComponent], (componentType, componentInstance) => {
                    editTagLiveSpy = spyOn(componentInstance.instance, 'editTagLive').and
                        .callFake((tag, context, changeFn) => onChangeFn = changeFn);
                });

                const reportedChangedStates: TagPropertyMap[] = [];
                const onChangeHandler: TagChangedFn = (tagProperties) => reportedChangedStates.push(tagProperties)

                fixture.detectChanges();
                fixture.componentInstance.tagEditorHost.editTagLive(tag, context, onChangeHandler);

                // Make sure that the TagEditor's editTagLive() method has been called appropriately.
                expect(editTagLiveSpy.calls.argsFor(0)[0]).toEqual(tag);
                assertTagEditorContextsEqual(context, editTagLiveSpy.calls.argsFor(0)[1]);
                expect(editTagLiveSpy.calls.argsFor(0)[2] instanceof Function).toBeTruthy();

                // Make sure that the TagEditor is actually displayed.
                fixture.detectChanges();
                tick();
                let tagEditor = fixture.debugElement.query(By.directive(CustomTagEditorHostComponent));
                expect(tagEditor).toBeTruthy();

                testOnChangeFnCalls(tag, onChangeFn, reportedChangedStates);
            }),
        );
    });

});

@Component({
    template: `
        <tag-editor-host #tagEditorHost></tag-editor-host>
    `
    })
class TestComponent {
    @ViewChild('tagEditorHost', { static: true })
    tagEditorHost: TagEditorHostComponent;
}

class MockErrorHandlerService {
    catch(error: Error, options?: { notification: boolean }): void { }
}
