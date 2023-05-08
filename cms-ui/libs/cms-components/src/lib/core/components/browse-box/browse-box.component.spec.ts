import {Component, Pipe, PipeTransform} from '@angular/core';
import { By } from '@angular/platform-browser';
import { componentTest, configureComponentTest } from '../../../testing';
import {GenticsUICoreModule} from '@gentics/ui-core';

import {BrowseBoxComponent} from './browse-box.component';

const CLEAR_BUTTON = '.browse-box__button--clear';
const BROWSE_BUTTON = '.browse-box__button--browse';
const UPLOAD_BUTTON = '.browse-box__button--upload';

describe('BrowseBoxComponent', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot()
            ],
            declarations: [
                BrowseBoxComponent,
                TestComponent,
                MockI18nPipe
            ]
        });
    });

    it('label is shown correctly',
        componentTest(() => TestComponent, (fixture, instance) => {
            const testLabel = 'Test Label';
            instance.label = testLabel;
            fixture.detectChanges();

            const labelElement = fixture.debugElement.query(By.css('gtx-input label'));
            expect(labelElement.nativeElement.innerHTML).toEqual(testLabel);
        })
    );

    it('displayValue is shown correctly',
        componentTest(() => TestComponent, (fixture, instance) => {
            const testDisplayValue = 'Test Folder';
            instance.displayValue = testDisplayValue;
            fixture.detectChanges();

            const inputElement = fixture.debugElement.query(By.css('gtx-input input'));
            expect(inputElement.nativeElement.value).toEqual(testDisplayValue);
        })
    );

    it('clear button is shown if content is clearable',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.clearable = true;
            fixture.detectChanges();

            expect(fixture.debugElement.query(By.css(CLEAR_BUTTON))).toBeTruthy();
        })
    );

    it('clear button is not shown if content is not clearable',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.clearable = false;
            fixture.detectChanges();

            expect(fixture.debugElement.query(By.css(CLEAR_BUTTON))).toBeFalsy();
        })
    );

    it('clear button shows the set tooltip',
        componentTest(() => TestComponent, (fixture, instance) => {
            const testClearTooltip = 'Test Clear Tooltip';
            instance.clearTooltip = testClearTooltip;
            fixture.detectChanges();

            const clearButton = fixture.debugElement.query(By.css(CLEAR_BUTTON));
            expect(clearButton.nativeElement.title).toEqual(testClearTooltip);
        })
    );

    it('clear button shows the default tooltip if none is set',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.clearTooltip = undefined;
            fixture.detectChanges();

            const clearButton = fixture.debugElement.query(By.css(CLEAR_BUTTON));
            expect(clearButton.nativeElement.title).toEqual('tag_editor.clear_selection');
        })
    );

    it('clear button click is emitted',
        componentTest(() => TestComponent, (fixture, instance) => {
            let clicked = false;
            instance.clearCallback = () => {
                clicked = true;
            };
            fixture.detectChanges();

            expect(clicked).toEqual(false);
            const clearButton = fixture.debugElement.query(By.css(CLEAR_BUTTON));
            clearButton.nativeElement.click();
            expect(clicked).toEqual(true);
        })
    );

    it('browse button is shown',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();

            expect(fixture.debugElement.query(By.css(BROWSE_BUTTON))).toBeTruthy();
        })
    );

    it('browse button shows the set tooltip',
        componentTest(() => TestComponent, (fixture, instance) => {
            const testBrowseTooltip = 'Test Clear Tooltip';
            instance.browseTooltip = testBrowseTooltip;
            fixture.detectChanges();

            const browseButton = fixture.debugElement.query(By.css(BROWSE_BUTTON));
            expect(browseButton.nativeElement.title).toEqual(testBrowseTooltip);
        })
    );

    it('browse button shows the default tooltip if none is set',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.browseTooltip = undefined;
            fixture.detectChanges();

            const browseButton = fixture.debugElement.query(By.css(BROWSE_BUTTON));
            expect(browseButton.nativeElement.title).toEqual('tag_editor.browse');
        })
    );

    it('browse button click is emitted',
        componentTest(() => TestComponent, (fixture, instance) => {
            let clicked = false;
            instance.browseCallback = () => {
                clicked = true;
            };
            fixture.detectChanges();

            expect(clicked).toEqual(false);
            const browseButton = fixture.debugElement.query(By.css(BROWSE_BUTTON));
            browseButton.nativeElement.click();
            expect(clicked).toEqual(true);
        })
    );

    it('upload button is shown if content can be uploaded',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.canUpload = true;
            fixture.detectChanges();

            expect(fixture.debugElement.query(By.css(UPLOAD_BUTTON))).toBeTruthy();
        })
    );

    it('upload button is not shown if content cannot be uploaded',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.canUpload = false;
            fixture.detectChanges();

            expect(fixture.debugElement.query(By.css(UPLOAD_BUTTON))).toBeFalsy();
        })
    );

    it('upload button shows the set tooltip',
        componentTest(() => TestComponent, (fixture, instance) => {
            const testUploadTooltip = 'Test Clear Tooltip';
            instance.uploadTooltip = testUploadTooltip;
            fixture.detectChanges();

            const uploadButton = fixture.debugElement.query(By.css(UPLOAD_BUTTON));
            expect(uploadButton.nativeElement.title).toEqual(testUploadTooltip);
        })
    );

    it('upload button shows the default tooltip if none is set',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.uploadTooltip = undefined;
            fixture.detectChanges();

            const uploadButton = fixture.debugElement.query(By.css(UPLOAD_BUTTON));
            expect(uploadButton.nativeElement.title).toEqual('tag_editor.upload');
        })
    );

    it('upload button click is emitted',
        componentTest(() => TestComponent, (fixture, instance) => {
            let clicked = false;
            instance.uploadCallback = () => {
                clicked = true;
            };
            fixture.detectChanges();

            expect(clicked).toEqual(false);
            const uploadButton = fixture.debugElement.query(By.css(UPLOAD_BUTTON));
            uploadButton.nativeElement.click();
            expect(clicked).toEqual(true);
        })
    );

    it('all buttons are disabled if browse box is disabled',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.disabled = true;
            fixture.detectChanges();

            const clearButton = fixture.debugElement.query(By.css(`${CLEAR_BUTTON} button`));
            expect(clearButton.nativeElement.disabled).toEqual(true);

            const browseButton = fixture.debugElement.query(By.css(`${BROWSE_BUTTON} button`));
            expect(browseButton.nativeElement.disabled).toEqual(true);

            const uploadButton = fixture.debugElement.query(By.css(`${UPLOAD_BUTTON} button`));
            expect(uploadButton.nativeElement.disabled).toEqual(true);
        })
    );

});


@Component({
    template: `
        <browse-box
            [label] = "label"
            [disabled] = "disabled"
            [displayValue] = "displayValue"
            [clearable] = "clearable"
            [canUpload] = "canUpload"
            [clearTooltip] = "clearTooltip"
            [browseTooltip] = "browseTooltip"
            [uploadTooltip] = "uploadTooltip"
            (clear) = "clearCallback()"
            (browse) = "browseCallback()"
            (upload) = "uploadCallback()"
        ></browse-box>
    `
})
class TestComponent {
    label: string = 'Label';
    disabled: boolean = false;
    displayValue: string = 'Folder';
    clearable: boolean = true;
    canUpload: boolean = true;
    clearTooltip: string = 'Clear';
    browseTooltip: string = 'Browse';
    uploadTooltip: string = 'Upload';
    clearCallback: () => void;
    browseCallback: () => void;
    uploadCallback: () => void;
}

@Pipe({ name: 'i18n' })
class MockI18nPipe implements PipeTransform {
    transform(query: string, ...args: any[]): string {
        return query;
    }
}
