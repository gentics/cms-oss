import { TestBed } from '@angular/core/testing';
import { Observable } from 'rxjs';
import { FormEditorConfigurationService } from '..';
import { FormEditorConfiguration } from '../../../common';
import { FormEditorService } from './form-editor.service';

describe('FormEditorService', () => {

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

        TestBed.configureTestingModule({
            providers: [
                FormEditorService,
                { provide: FormEditorConfigurationService, useValue: formEditorConfigurationServiceMock },
            ],
        });

    });

    it('should be created', () => {
        formEditorConfigurationServiceMockConfiguration$Spy.and.returnValue(new Observable());
        const service: FormEditorService = TestBed.inject(FormEditorService);
        expect(service).toBeTruthy();
    });
});
