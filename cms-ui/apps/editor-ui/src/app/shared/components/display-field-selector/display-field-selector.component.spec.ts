import { ComponentFixture, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { ApplicationStateService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { GetInheritancePipe } from '../../pipes/get-inheritance/get-inheritance.pipe';
import { DisplayFieldSelectorModal } from './display-field-selector.component';

describe('DisplayFieldSelectorModal', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [GenticsUICoreModule, FormsModule],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
            declarations: [DisplayFieldSelectorModal, GetInheritancePipe],
        });
    });

    it('should display correct initial fields for folder type',
        componentTest(() => DisplayFieldSelectorModal, fixture => {
            expect(initialFieldNamesForType('folder', fixture)).toEqual([
                'cdate',
                'creator',
                'edate',
                'editor',
                'id',
                'globalId',
                'inheritance',
            ]);
        }),
    );

    it('should display correct initial fields for page type',
        componentTest(() => DisplayFieldSelectorModal, fixture => {
            expect(initialFieldNamesForType('page', fixture)).toEqual([
                'showPath',
                'cdate',
                'creator',
                'edate',
                'editor',
                'id',
                'globalId',
                'pdate',
                'unpublishedDate',
                'customCdate',
                'customEdate',
                'priority',
                'template',
                'usage',
                'at',
                'offlineAt',
                'queuedPublish',
                'queuedOffline',
                'inheritance',
            ]);
        }),
    );

    it('should display correct initial fields for file type',
        componentTest(() => DisplayFieldSelectorModal, fixture => {
            expect(initialFieldNamesForType('file', fixture)).toEqual([
                'showPath',
                'cdate',
                'creator',
                'edate',
                'editor',
                'id',
                'globalId',
                'customCdate',
                'customEdate',
                'fileType',
                'fileSize',
                'usage',
                'inheritance',
            ]);
        }),
    );

    it('should display correct initial fields for image type',
        componentTest(() => DisplayFieldSelectorModal, fixture => {
            expect(initialFieldNamesForType('image', fixture)).toEqual([
                'showPath',
                'cdate',
                'creator',
                'edate',
                'editor',
                'id',
                'globalId',
                'customCdate',
                'customEdate',
                'fileType',
                'fileSize',
                'usage',
                'inheritance',
            ]);
        }),
    );

    it('should check the fields passed in',
        componentTest(() => DisplayFieldSelectorModal, (fixture, instance) => {
            instance.type = 'folder';
            instance.fields = ['creator', 'id'];
            fixture.detectChanges();
            tick();
            fixture.detectChanges();
            const checked = getListedFields(fixture).filter(item => item.checked).map(item => item.fieldName);

            expect(checked).toEqual(['creator', 'id']);
        }),
    );

    it('should order the list based on order of fields passed in',
        componentTest(() => DisplayFieldSelectorModal, (fixture, instance) => {
            instance.type = 'folder';
            instance.fields = ['id', 'creator'];
            fixture.detectChanges();
            const checked = getListedFields(fixture).map(item => item.fieldName);

            expect(checked).toEqual([
                'id',
                'creator',
                'cdate',
                'edate',
                'editor',
                'globalId',
                'inheritance',
            ]);
        }),
    );
});


type FieldNames = { fieldName: string; checked: boolean; }[];

/**
 * Helper for getting the initial field names that are present for a given type.
 */
function initialFieldNamesForType(type: any, fixture: ComponentFixture<DisplayFieldSelectorModal>): string[] {
    fixture.componentRef.instance.type = type;
    fixture.detectChanges();
    return getListedFields(fixture).map(field => field.fieldName);
}

/**
 * Returns a list of objects representing the checkboxes in the fields list.
 */
const getListedFields = (fixture: ComponentFixture<DisplayFieldSelectorModal>): FieldNames => fixture.debugElement
    .queryAll(By.css('input[type="checkbox"]'))
    .map(debugElement => <HTMLInputElement> debugElement.nativeElement)
    .map(checkbox => ({
        fieldName: checkbox.name,
        checked: checkbox.checked,
    }));
