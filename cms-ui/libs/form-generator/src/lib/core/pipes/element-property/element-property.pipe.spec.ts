import { TestBed } from '@angular/core/testing';
import { ElementPropertyPipe } from './element-property.pipe';

describe('ElementPropertyPipe', () => {

    beforeEach(() => {
        // create mock, no function spies
        /**
         * createSpyObj not usable without function spies and does not support property spies declarations in our version
         * replace with https://jasmine.github.io/api/3.6/jasmine.html#.createSpyObj after updating
         */
        const formEditorServiceMock = {} as any; // SpyObj<T> seems not to be exported

        TestBed.configureTestingModule({
        });

    });

    it('should be created', () => {
        const pipe = new ElementPropertyPipe();
        expect(pipe).toBeTruthy();
    });
});
