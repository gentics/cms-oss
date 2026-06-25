import { Component, DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { configureComponentTest, MockI18nPipe } from '@gentics/cms-components/testing';
import { FormElementContainerComponent, GenticsUICoreModule } from '@gentics/ui-core';
import { componentTest } from '@gentics/ui-core/testing';
import { BrowseBoxComponent } from './browse-box.component';
import { ComponentFixture } from '@angular/core/testing';
import { GCMS_UI_SERVICES_PROVIDER } from '../../providers';

type ActionButton = 'browse' | 'upload' | 'clear';

function getDisplayValue(fixture: ComponentFixture<any>): string {
    return (fixture.nativeElement as HTMLElement).querySelector('.display-value').textContent.trim();
}

function getButton(fixture: ComponentFixture<any>, action: ActionButton): DebugElement | null {
    return fixture.debugElement.query(By.css(`.addon-button[data-action="${action}"]`));
}

function getButtonElement(fixture: ComponentFixture<any>, action: ActionButton): HTMLButtonElement | null {
    const dbg = getButton(fixture, action);
    return dbg == null ? null : dbg.nativeElement;
}

describe('BrowseBoxComponent', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
            declarations: [
                FormElementContainerComponent,
                BrowseBoxComponent,
                TestComponent,
                MockI18nPipe,
            ],
            providers: [
                { provide: GCMS_UI_SERVICES_PROVIDER, useValue: {} },
            ],
        });
    });

    it('label is shown correctly',
        componentTest(() => TestComponent, (fixture, instance) => {
            const testLabel = 'Test Label';
            instance.label = testLabel;
            fixture.detectChanges();

            const labelElement = fixture.debugElement.query(By.css('label'));
            expect(labelElement.nativeElement.textContent).toEqual(testLabel);
        }),
    );

    it('clear button is shown if content is clearable',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.clearable = true;
            fixture.detectChanges();

            expect(getButton(fixture, 'clear')).not.toEqual(null);
        }),
    );

    it('clear button is not shown if content is not clearable',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.clearable = false;
            fixture.detectChanges();

            expect(getButton(fixture, 'clear')).toEqual(null);
        }),
    );

    it('clear button shows the set tooltip',
        componentTest(() => TestComponent, (fixture, instance) => {
            const testClearTooltip = 'Test Clear Tooltip';
            instance.clearTooltip = testClearTooltip;
            fixture.detectChanges();

            const clearButton = getButtonElement(fixture, 'clear');
            expect(clearButton.title).toEqual(testClearTooltip);
        }),
    );

    it('clear button shows the default tooltip if none is set',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.clearTooltip = undefined;
            fixture.detectChanges();

            const clearButton = getButtonElement(fixture, 'clear');
            expect(clearButton.title).toEqual('tag_editor.clear_selection');
        }),
    );

    it('browse button is shown',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();

            expect(getButton(fixture, 'browse')).not.toEqual(null);
        }),
    );

    it('browse button shows the set tooltip',
        componentTest(() => TestComponent, (fixture, instance) => {
            const testBrowseTooltip = 'Test Clear Tooltip';
            instance.browseTooltip = testBrowseTooltip;
            fixture.detectChanges();

            const browseButton = getButtonElement(fixture, 'browse');
            expect(browseButton.title).toEqual(testBrowseTooltip);
        }),
    );

    it('browse button shows the default tooltip if none is set',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.browseTooltip = undefined;
            fixture.detectChanges();

            const browseButton = getButtonElement(fixture, 'browse');
            expect(browseButton.title).toEqual('tag_editor.browse');
        }),
    );

    it('upload button is shown if content can be uploaded',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.canUpload = true;
            fixture.detectChanges();

            expect(getButton(fixture, 'upload')).not.toEqual(null);
        }),
    );

    it('upload button is not shown if content cannot be uploaded',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.canUpload = false;
            fixture.detectChanges();

            expect(getButton(fixture, 'upload')).toEqual(null);
        }),
    );

    it('upload button shows the set tooltip',
        componentTest(() => TestComponent, (fixture, instance) => {
            const testUploadTooltip = 'Test Clear Tooltip';
            instance.uploadTooltip = testUploadTooltip;
            fixture.detectChanges();

            const uploadButton = getButtonElement(fixture, 'upload');
            expect(uploadButton.title).toEqual(testUploadTooltip);
        }),
    );

    it('upload button shows the default tooltip if none is set',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.uploadTooltip = undefined;
            fixture.detectChanges();

            const uploadButton = getButtonElement(fixture, 'upload');
            expect(uploadButton.title).toEqual('tag_editor.upload');
        }),
    );

    it('all buttons are disabled if browse box is disabled',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.disabled = true;
            fixture.detectChanges();

            const clearButton = getButtonElement(fixture, 'clear');
            expect(clearButton.disabled).toEqual(true);

            const browseButton = getButtonElement(fixture, 'browse');
            expect(browseButton.disabled).toEqual(true);

            const uploadButton = getButtonElement(fixture, 'upload');
            expect(uploadButton.disabled).toEqual(true);
        }),
    );

});

@Component({
    template: `
        <gtx-browse-box
            [label]="label"
            [disabled]="disabled"
            [clearable]="clearable"
            [canUpload]="canUpload"
            [clearTooltip]="clearTooltip"
            [browseTooltip]="browseTooltip"
            [uploadTooltip]="uploadTooltip"
            (upload)="uploadCallback()"
        />
    `,
    standalone: false,
})
class TestComponent {
    label = 'Label';
    disabled = false;
    clearable = true;
    canUpload = true;
    clearTooltip = 'Clear';
    browseTooltip = 'Browse';
    uploadTooltip = 'Upload';
    uploadCallback: () => void;
}
