import { ChangeDetectionStrategy, Component, ErrorHandler, ViewChild } from '@angular/core';
import { TestBed, tick, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { StringTagPartProperty } from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { getExampleEditableTag, getMockedTagEditorContext } from '../../../../testing/test-tag-editor-data.mock';
import { UserAgentRef } from '../../../shared/providers/user-agent-ref';
import { EditableTag } from '../../common';
import { assertTagEditorContextsEqual } from '../../common/impl/tag-editor-context.spec';
import { IFrameStylesService } from '../../providers/iframe-styles/iframe-styles.service';
import { TagEditorService } from '../../providers/tag-editor/tag-editor.service';
import { TagEditorOverlayHostComponent } from './tag-editor-overlay-host.component';

describe('TagEditorOverlayHostComponent', () => {

    beforeEach(waitForAsync(() => {
        configureComponentTest({
            imports: [GenticsUICoreModule.forRoot()],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: ErrorHandler, useClass: MockErrorHandlerService },
                { provide: TagEditorService, useClass: MockTagEditorService },
                IFrameStylesService,
                UserAgentRef,
            ],
            declarations: [
                MockTagEditorHostComponent,
                TagEditorOverlayHostComponent,
                TestComponent,
            ],
        });
    }));

    it('registers and unregisters itself with the TagEditorService', () => {
        const tagEditorService = TestBed.get(TagEditorService) as TagEditorService;
        spyOn(tagEditorService, 'registerTagEditorOverlayHost').and.stub();
        spyOn(tagEditorService, 'unregisterTagEditorOverlayHost').and.stub();

        const test = componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();
            expect(tagEditorService.registerTagEditorOverlayHost).toHaveBeenCalledWith(instance.tagEditorOverlayHost);
            expect(tagEditorService.unregisterTagEditorOverlayHost).not.toHaveBeenCalled();

            fixture.destroy();
            expect(tagEditorService.unregisterTagEditorOverlayHost).toHaveBeenCalledWith(instance.tagEditorOverlayHost);
        });
        test();
    });

    it('shows the TagEditorHost, uses its editTag() method to open the TagEditor, passes on the promise\'s resolve, and hides the TagEditorHost again',
        componentTest(() => TestComponent, (fixture, instance) => {
            const tag = getExampleEditableTag();
            const expectedEditedTag = getExampleEditableTag();
            (expectedEditedTag.properties['property0'] as StringTagPartProperty).stringValue = 'modified Value';
            const context = getMockedTagEditorContext(tag);
            fixture.detectChanges();

            const result = fixture.componentInstance.tagEditorOverlayHost.openTagEditor(tag, context);
            fixture.detectChanges();
            tick();
            let tagEditorHostElem = fixture.debugElement.query(By.css('tag-editor-host'));
            expect(tagEditorHostElem).toBeTruthy();
            const tagEditorHost = tagEditorHostElem.componentInstance as MockTagEditorHostComponent;

            // Make sure that the TagEditorHost's editTag() method has been called appropriately.
            expect(tagEditorHost.editTag.calls.count()).toEqual(1);
            expect(tagEditorHost.editTag.calls.argsFor(0)[0]).toEqual(tag);
            assertTagEditorContextsEqual(context, tagEditorHost.editTag.calls.argsFor(0)[1]);
            expect(result instanceof Promise).toBe(true);

            // Make sure that the promise is resolved correctly.
            let origPromiseResolved = false;
            let resultResolved = false;
            result.then(actualEditedTag => {
                expect(origPromiseResolved).toBe(true);
                expect(actualEditedTag).toEqual(expectedEditedTag);
                resultResolved = true;
            }).catch(() => fail('result promise should not be rejected.'));
            tagEditorHost.editTagResolveFn(expectedEditedTag);
            origPromiseResolved = true;
            tick();
            expect(resultResolved).toBe(true);

            // Make sure that the TagEditorHost has been destroyed again.
            fixture.detectChanges();
            tick();
            tagEditorHostElem = fixture.debugElement.query(By.css('tag-editor-host'));
            expect(tagEditorHostElem).toBeFalsy();
        }),
    );

    it('shows the TagEditorHost, uses its editTag() method to open the TagEditor, passes on the promise\'s reject, and hides the TagEditorHost again',
        componentTest(() => TestComponent, (fixture, instance) => {
            const tag = getExampleEditableTag();
            const rejectionReason = 'RejectionReason';
            const context = getMockedTagEditorContext(tag);
            fixture.detectChanges();

            const result = fixture.componentInstance.tagEditorOverlayHost.openTagEditor(tag, context);
            fixture.detectChanges();
            tick();
            let tagEditorHostElem = fixture.debugElement.query(By.css('tag-editor-host'));
            expect(tagEditorHostElem).toBeTruthy();
            const tagEditorHost = tagEditorHostElem.componentInstance as MockTagEditorHostComponent;

            // Make sure that the TagEditorHost's editTag() method has been called appropriately.
            expect(tagEditorHost.editTag.calls.count()).toEqual(1);
            expect(tagEditorHost.editTag.calls.argsFor(0)[0]).toEqual(tag);
            assertTagEditorContextsEqual(context, tagEditorHost.editTag.calls.argsFor(0)[1]);
            expect(result instanceof Promise).toBe(true);

            // Make sure that the promise is rejected correctly.
            let origPromiseRejected = false;
            let resultRejected = false;
            result.then(() => fail('result promise should not be resolved'))
                .catch((reason) => {
                    expect(origPromiseRejected).toBe(true);
                    expect(reason).toBe(rejectionReason);
                    resultRejected = true;
                });
            tagEditorHost.editTagRejectFn(rejectionReason);
            origPromiseRejected = true;
            tick();
            expect(resultRejected).toBe(true);

            // Make sure that the TagEditorHost has been destroyed again.
            fixture.detectChanges();
            tick();
            tagEditorHostElem = fixture.debugElement.query(By.css('tag-editor-host'));
            expect(tagEditorHostElem).toBeFalsy();
        }),
    );

    it('throws an error if the TagEditor is already open',
        componentTest(() => TestComponent, (fixture, instance) => {
            const tag = getExampleEditableTag();
            const context = getMockedTagEditorContext(tag);
            fixture.detectChanges();

            const result = fixture.componentInstance.tagEditorOverlayHost.openTagEditor(tag, context);
            fixture.detectChanges();
            tick();
            let tagEditorHostElem = fixture.debugElement.query(By.css('tag-editor-host'));
            expect(tagEditorHostElem).toBeTruthy();
            const tagEditorHost = tagEditorHostElem.componentInstance as MockTagEditorHostComponent;

            expect(tagEditorHost.editTag.calls.count()).toEqual(1);
            tagEditorHost.editTag.calls.reset();

            // Try opening the TagEditor again
            const tag2 = getExampleEditableTag();
            ++tag2.id;
            const context2 = getMockedTagEditorContext(tag2);
            expect(() => fixture.componentInstance.tagEditorOverlayHost.openTagEditor(tag2, context2)).toThrow();

            // Make sure that the original promise still resolves correctly.
            let origPromiseResolved = false;
            let resultResolved = false;
            result.then(actualEditedTag => {
                expect(origPromiseResolved).toBe(true);
                expect(actualEditedTag).toEqual(tag);
                resultResolved = true;
            }).catch(() => fail('result promise should not be rejected.'));
            tagEditorHost.editTagResolveFn(tag);
            origPromiseResolved = true;
            tick();
            expect(resultResolved).toBe(true);

            // Make sure that the TagEditorHost has been destroyed again.
            fixture.detectChanges();
            tick();
            tagEditorHostElem = fixture.debugElement.query(By.css('tag-editor-host'));
            expect(tagEditorHostElem).toBeFalsy();
        }),
    );

});

@Component({
    selector: 'tag-editor-host',
    template: ``,
    changeDetection: ChangeDetectionStrategy.OnPush
    })
class MockTagEditorHostComponent {
    editTagResolveFn: (value?: EditableTag | PromiseLike<EditableTag>) => void;
    editTagRejectFn: (reason?: any) => void;

    editTag = jasmine.createSpy('editTag').and.callFake(() => {
        return new Promise<EditableTag>((resolve, reject) => {
            this.editTagResolveFn = resolve;
            this.editTagRejectFn = reject;
        });
    });
}

@Component({
    template: `
        <tag-editor-overlay-host #tagEditorOverlayHost></tag-editor-overlay-host>
    `
    })
class TestComponent {
    @ViewChild('tagEditorOverlayHost', { static: true })
    tagEditorOverlayHost: TagEditorOverlayHostComponent;
}

class MockTagEditorService {
    registerTagEditorOverlayHost(tagEditorOverlayHost: TagEditorOverlayHostComponent): void { }
    unregisterTagEditorOverlayHost(tagEditorOverlayHost: TagEditorOverlayHostComponent): void { }
}

class MockErrorHandlerService {
    catch(error: Error, options?: { notification: boolean }): void { }
}
