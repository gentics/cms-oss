import { TestBed } from '@angular/core/testing';
import { Observable } from 'rxjs';
import { FormEditorConfigurationService, FormEditorMappingService } from '..';
import { FormEditorConfiguration } from '../../../common';
import { FormReportService } from './form-report.service';

describe('FormReportService', () => {

    let formEditorConfigurationServiceMockConfiguration$Spy;

    beforeEach(() => {
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

        TestBed.configureTestingModule({
            providers: [
                FormReportService,
                { provide: FormEditorConfigurationService, useValue: formEditorConfigurationServiceMock },
                { provide: FormEditorMappingService, useValue: formEditorMappingServiceMock },
            ],
        });

    });

    it('should be created', () => {
        formEditorConfigurationServiceMockConfiguration$Spy.and.returnValue(new Observable());
        const service: FormReportService = TestBed.inject(FormReportService);
        expect(service).toBeTruthy();
    });
});
