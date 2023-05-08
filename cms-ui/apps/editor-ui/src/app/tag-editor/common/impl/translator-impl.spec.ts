import { Translator } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { TranslatorImpl } from './translator-impl';

describe('TranslatorImpl', () => {

    let translator: Translator;
    let translateService: MockTranslateService;
    let getSpy: jasmine.Spy;
    let instantSpy: jasmine.Spy;

    beforeEach(() => {
        translateService = new MockTranslateService();
        getSpy = spyOn(translateService, 'get');
        instantSpy = spyOn(translateService, 'instant');
        translator = new TranslatorImpl(translateService as any);
    });

    it('get(string) works', () => {
        const key = 'test.key';
        const expectedResult = Observable.of('test');
        getSpy = getSpy.and.returnValue(expectedResult);

        const actualResult = translator.get(key);
        expect(actualResult).toBe(expectedResult);
        expect(getSpy).toHaveBeenCalledWith(key, undefined);
    });

    it('get(string[]) works', () => {
        const keys = [ 'test.key0', 'test.key1' ];
        const expectedResult = Observable.of({
            'test.key0': 'test0',
            'test.key1': 'test1'
        });
        getSpy = getSpy.and.returnValue(expectedResult);

        const actualResult = translator.get(keys);
        expect(actualResult).toBe(expectedResult);
        expect(getSpy).toHaveBeenCalledWith(keys, undefined);
    });

    it('get(string, interpolateParams) works', () => {
        const key = 'test.key';
        const interpolateParams = { paramA: 'A' };
        const expectedResult = Observable.of('test');
        getSpy = getSpy.and.returnValue(expectedResult);

        const actualResult = translator.get(key, interpolateParams);
        expect(actualResult).toBe(expectedResult);
        expect(getSpy).toHaveBeenCalledWith(key, interpolateParams);
    });

    it('get(string[], interpolateParams) works', () => {
        const keys = [ 'test.key0', 'test.key1' ];
        const interpolateParams = { paramA: 'A' };
        const expectedResult = Observable.of({
            'test.key0': 'test0',
            'test.key1': 'test1'
        });
        getSpy = getSpy.and.returnValue(expectedResult);

        const actualResult = translator.get(keys, interpolateParams);
        expect(actualResult).toBe(expectedResult);
        expect(getSpy).toHaveBeenCalledWith(keys, interpolateParams);
    });

    it('instant(string) works', () => {
        const key = 'test.key';
        const expectedResult = 'test';
        instantSpy = instantSpy.and.returnValue(expectedResult);

        const actualResult = translator.instant(key);
        expect(actualResult).toBe(expectedResult);
        expect(instantSpy).toHaveBeenCalledWith(key, undefined);
    });

    it('instant(string[]) works', () => {
        const keys = [ 'test.key0', 'test.key1' ];
        const expectedResult = {
            'test.key0': 'test0',
            'test.key1': 'test1'
        };
        instantSpy = instantSpy.and.returnValue(expectedResult);

        const actualResult = translator.instant(keys);
        expect(actualResult).toBe(expectedResult);
        expect(instantSpy).toHaveBeenCalledWith(keys, undefined);
    });

    it('instant(string, interpolateParams) works', () => {
        const key = 'test.key';
        const interpolateParams = { paramA: 'A' };
        const expectedResult = 'test';
        instantSpy = instantSpy.and.returnValue(expectedResult);

        const actualResult = translator.instant(key, interpolateParams);
        expect(actualResult).toBe(expectedResult);
        expect(instantSpy).toHaveBeenCalledWith(key, interpolateParams);
    });

    it('instant(string[], interpolateParams) works', () => {
        const keys = [ 'test.key0', 'test.key1' ];
        const interpolateParams = { paramA: 'A' };
        const expectedResult = {
            'test.key0': 'test0',
            'test.key1': 'test1'
        };
        instantSpy = instantSpy.and.returnValue(expectedResult);

        const actualResult = translator.instant(keys, interpolateParams);
        expect(actualResult).toBe(expectedResult);
        expect(instantSpy).toHaveBeenCalledWith(keys, interpolateParams);
    });

});

class MockTranslateService {
    get(key: string | string[], interpolateParams?: Object): Observable<string | Object> {
        return null;
    }
    instant(key: string | string[], interpolateParams?: Object): string | Object {
        return null;
    }
}
