import { Component, DebugElement, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ApplicationStateService } from '@editor-ui/app/state';
import { Folder, FileUpload } from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../../testing';
import { TestApplicationState, TwoLevelsPartial } from '../../../../state/test-application-state.mock';
import { UploadWithPropertiesModalComponent } from './upload-with-properties-modal.component';

describe('UploadWithPropertiesModalComponent', () => {

    beforeEach(() => {

        configureComponentTest({
            imports: [
                FormsModule,
                GenticsUICoreModule.forRoot(),
                ReactiveFormsModule,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
            declarations: [
                MockUploadWithPropertiesComponent,
                UploadWithPropertiesModalComponent,
                TestComponent,
            ],
        });

    });

    describe('keeps the status between itself and UploadWithPropertiesComponent consistent', () => {

        it('by relaying the inputs',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.allowFolderSelection = true;
                instance.destinationFolder = null;
                instance.itemType = 'image';
                fixture.detectChanges();

                const uploadWithPropertiesComponent: DebugElement = fixture.debugElement.query(By.directive(MockUploadWithPropertiesComponent));
                expect(uploadWithPropertiesComponent.componentInstance.allowFolderSelection).toEqual(true);
                expect(uploadWithPropertiesComponent.componentInstance.destinationFolder).toBeNull();
                expect(uploadWithPropertiesComponent.componentInstance.itemType).toEqual('image');
            }),
        );

        it('by hiding the upload button in UploadWithPropertiesComponent',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.itemType = 'image';
                fixture.detectChanges();

                const uploadWithPropertiesComponent: DebugElement = fixture.debugElement.query(By.directive(MockUploadWithPropertiesComponent));
                expect(uploadWithPropertiesComponent.componentInstance.showUploadButton).toEqual(false);
            }),
        );

        it('by enabling the upload button if a file has been selected in UploadWithPropertiesComponent',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.itemType = 'image';
                fixture.detectChanges();

                const uploadButton: DebugElement = fixture.debugElement.query(By.css('#upload-button button'));
                expect(uploadButton.nativeElement.disabled).toEqual(true);

                const uploadWithPropertiesComponent: DebugElement = fixture.debugElement.query(By.directive(MockUploadWithPropertiesComponent));
                uploadWithPropertiesComponent.componentInstance.uploadPossible.emit(true);
                fixture.detectChanges();

                expect(uploadButton.nativeElement.disabled).toEqual(false);
            }),
        );

        it('by triggering the close callback when a new upload was completed',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.itemType = 'image';
                fixture.detectChanges();
                const test_upload: TwoLevelsPartial<FileUpload> = { destinationFolder: { description: 'This is a testfolder' } };
                let receivedUpload;
                instance.uploadWithPropertiesModalComponent.registerCloseFn((upload: FileUpload) => {
                    receivedUpload = upload
                });

                const uploadWithPropertiesComponent: DebugElement = fixture.debugElement.query(By.directive(MockUploadWithPropertiesComponent));
                uploadWithPropertiesComponent.componentInstance.upload.emit(test_upload);
                fixture.detectChanges();

                expect(receivedUpload).toEqual(test_upload);
            }),
        );

        it('by triggering an upload in the UploadWithPropertiesComponent if the upload button is clicked',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.itemType = 'image';
                let uploadTriggered = false;
                fixture.detectChanges();

                const uploadButton: DebugElement = fixture.debugElement.query(By.css('#upload-button'));

                const uploadWithPropertiesComponent: DebugElement = fixture.debugElement.query(By.directive(MockUploadWithPropertiesComponent));
                uploadWithPropertiesComponent.componentInstance.uploadPossible.emit(true);
                uploadWithPropertiesComponent.componentInstance.triggerUpload = () => {
                    uploadTriggered = true;
                }
                // manually set ViewChild, since the mock is not recognized as the correct child in UploadWithPropertiesModalComponent
                instance.uploadWithPropertiesModalComponent.uploadWithPropertiesComponent = uploadWithPropertiesComponent.componentInstance;
                fixture.detectChanges();

                uploadButton.nativeElement.click();

                expect(uploadTriggered).toEqual(true);
            }),
        );
    });
});


@Component({
    template: `<upload-with-properties-modal
        [allowFolderSelection]="allowFolderSelection"
        [destinationFolder]="destinationFolder"
        [itemType]="itemType"
    ></upload-with-properties-modal>`,
})
class TestComponent {

    allowFolderSelection: boolean;
    destinationFolder: Folder;
    itemType: 'file' | 'image';

    @ViewChild(UploadWithPropertiesModalComponent, { static: true }) uploadWithPropertiesModalComponent: UploadWithPropertiesModalComponent;
}

@Component({
    selector: 'upload-with-properties',
    template: '',
})
class MockUploadWithPropertiesComponent implements OnInit {

    @Input() allowFolderSelection = true;
    @Input() destinationFolder: Folder;
    @Input() itemType: 'file' | 'image';
    @Input() showUploadButton = true;

    @Output() uploadPossible = new EventEmitter<boolean>();
    @Output() upload = new EventEmitter<FileUpload>();

    ngOnInit() {
        this.uploadPossible.emit(false);
    }

    triggerUpload: () => void;
}

