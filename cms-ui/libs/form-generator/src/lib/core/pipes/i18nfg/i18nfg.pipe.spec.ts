import { TestBed } from '@angular/core/testing';
import { I18nFgPipe } from '..';
import { FormEditorService } from '../../providers';

describe('I18nFgPipe', () => {

    beforeEach(() => {
        // create mock, no function spies
        /**
         * createSpyObj not usable without function spies and does not support property spies declarations in our version
         * replace with https://jasmine.github.io/api/3.6/jasmine.html#.createSpyObj after updating
         */
        const formEditorServiceMock = {} as any; // SpyObj<T> seems not to be exported

        TestBed.configureTestingModule({
            providers: [
                { provide: FormEditorService, useValue: formEditorServiceMock },
            ],
        });

    });

    it('should be created', () => {
        const formService: FormEditorService = TestBed.inject(FormEditorService);
        const pipe = new I18nFgPipe(formService);
        expect(pipe).toBeTruthy();
    });
});
