import { ComponentFixture, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { componentTest, configureComponentTest } from '../../../../testing';
import { GetInheritancePipe } from '../../pipes/get-inheritance/get-inheritance.pipe';
import { DisplayFieldSelector } from './display-field-selector.component';


describe('DisplayFieldSelector:', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [GenticsUICoreModule, FormsModule],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
            declarations: [DisplayFieldSelector, GetInheritancePipe],
        });
    });

    it('should display correct initial fields for folder type',
        componentTest(() => DisplayFieldSelector, fixture => {
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
        componentTest(() => DisplayFieldSelector, fixture => {
            expect(initialFieldNamesForType('page', fixture)).toEqual([
                'showPath',
                'cdate',
                'creator',
                'edate',
                'editor',
                'id',
                'globalId',
                'pdate',
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
        componentTest(() => DisplayFieldSelector, fixture => {
            expect(initialFieldNamesForType('file', fixture)).toEqual([
                'showPath',
                'cdate',
                'creator',
                'edate',
                'editor',
                'id',
                'globalId',
                'fileType',
                'fileSize',
                'usage',
                'inheritance',
            ]);
        }),
    );

    it('should display correct initial fields for image type',
        componentTest(() => DisplayFieldSelector, fixture => {
            expect(initialFieldNamesForType('image', fixture)).toEqual([
                'showPath',
                'cdate',
                'creator',
                'edate',
                'editor',
                'id',
                'globalId',
                'fileType',
                'fileSize',
                'usage',
                'inheritance',
            ]);
        }),
    );

    it('should check the fields passed in',
        componentTest(() => DisplayFieldSelector, (fixture, instance) => {
            instance.type = 'folder';
            instance.fields = ['creator', 'id'];
            fixture.detectChanges();
            tick();
            fixture.detectChanges();
            let checked = getListedFields(fixture).filter(item => item.checked).map(item => item.fieldName);

            expect(checked).toEqual(['creator', 'id']);
        }),
    );

    it('should order the list based on order of fields passed in',
        componentTest(() => DisplayFieldSelector, (fixture, instance) => {
            instance.type = 'folder';
            instance.fields = ['id', 'creator'];
            fixture.detectChanges();
            let checked = getListedFields(fixture).map(item => item.fieldName);

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
function initialFieldNamesForType(type: any, fixture: ComponentFixture<DisplayFieldSelector>): string[] {
    fixture.componentRef.instance.type = type;
    fixture.detectChanges();
    return getListedFields(fixture).map(field => field.fieldName);
}

/**
 * Returns a list of objects representing the checkboxes in the fields list.
 */
const getListedFields = (fixture: ComponentFixture<DisplayFieldSelector>): FieldNames => fixture.debugElement
    .queryAll(By.css('input[type="checkbox"]'))
    .map(debugElement => <HTMLInputElement> debugElement.nativeElement)
    .map(checkbox => ({
        fieldName: checkbox.name,
        checked: checkbox.checked,
    }));
