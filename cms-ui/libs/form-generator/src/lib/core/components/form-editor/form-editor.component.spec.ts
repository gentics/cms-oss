import { Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Icon } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { FormEditorConfigurationService, FormEditorMappingService, FormEditorService } from '../../providers';
import { FormEditorConfiguration } from '../../providers/form-editor-configuration/form-editor-configuration.model';
import { FormEditorElementListComponent } from '../form-editor-element-list/form-editor-element-list.component';
import { FormEditorElementComponent } from '../form-editor-element/form-editor-element.component';
import { FormEditorMenuComponent } from '../form-editor-menu/form-editor-menu.component';
import { FormElementDropZoneComponent } from '../form-element-drop-zone/form-element-drop-zone.component';
import { FormEditorComponent } from './form-editor.component';

describe('FormEditorComponent', () => {

    let component: FormEditorComponent;
    let fixture: ComponentFixture<FormEditorComponent>;

    let formEditorConfigurationServiceMockConfiguration$Spy;
    let formEditorMappingServiceMockMapFormBOToFormSpy;
    let formEditorMappingServiceMockMapFormToFormBOSpy;

    beforeEach(waitForAsync(() => {

        // create mock, no function spies
        /**
         * createSpyObj not usable without function spies and does not support property spies declarations in our version
         * replace with https://jasmine.github.io/api/3.6/jasmine.html#.createSpyObj after updating
         */
        const formEditorServiceMock = {} as any; // SpyObj<T> seems not to be exported

        // create mock, no function spies
        /**
         * createSpyObj not usable without function spies and does not support property spies declarations in our version
         * replace with https://jasmine.github.io/api/3.6/jasmine.html#.createSpyObj after updating
         */
        const formEditorConfigurationServiceMock = {
            get configuration$(): Observable<FormEditorConfiguration>  {
                return undefined;
            },
        } as any; // SpyObj<T> seems not to be exported
        // create property spy on getter
        formEditorConfigurationServiceMockConfiguration$Spy = spyOnProperty(formEditorConfigurationServiceMock, 'configuration$', 'get');

        // create mock
        const formEditorMappingServiceMock = jasmine.createSpyObj('FormEditorMappingService', ['mapFormBOToForm', 'mapFormToFormBO']);
        formEditorMappingServiceMockMapFormBOToFormSpy = formEditorMappingServiceMock.mapFormBOToForm;
        formEditorMappingServiceMockMapFormToFormBOSpy = formEditorMappingServiceMock.mapFormToFormBO;

        TestBed.configureTestingModule({
            declarations: [
                FormEditorElementComponent,
                FormEditorElementListComponent,
                FormEditorMenuComponent,
                FormEditorComponent,
                FormElementDropZoneComponent,
                Icon,
                MockI18nPipe,
            ],
            imports: [
                NoopAnimationsModule,
            ],
            providers: [
                { provide: FormEditorService, useValue: formEditorServiceMock },
                { provide: FormEditorConfigurationService, useValue: formEditorConfigurationServiceMock },
                { provide: FormEditorMappingService, useValue: formEditorMappingServiceMock },
            ],
        })
        .compileComponents();
    }));

    beforeEach(() => {
        formEditorConfigurationServiceMockConfiguration$Spy.and.returnValue(new Observable());
        fixture = TestBed.createComponent(FormEditorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

@Pipe({ name: 'i18n' })
class MockI18nPipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}
