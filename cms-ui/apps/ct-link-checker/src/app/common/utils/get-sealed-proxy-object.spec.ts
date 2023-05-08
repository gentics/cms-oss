import { fakeAsync, tick } from '@angular/core/testing';

import { EventEmitter } from '@angular/core';
import { first } from 'rxjs/operators';
import { getSealedProxyObject } from './get-sealed-proxy-object';

const TEST_OBJECT = {
    name: 'Test',
    lang: 'de',
    values: 10
};

describe('get-sealed-proxy-object', () => {
    it('Proxy element has all properties and events$ are undefined because it\'s not provided', () => {
        const proxyObj = getSealedProxyObject(TEST_OBJECT);
        expect(TEST_OBJECT).not.toEqual(proxyObj);
        expect(proxyObj).toEqual(jasmine.objectContaining(TEST_OBJECT));
        expect(proxyObj.events$).toBeUndefined();
    });

    it('Proxy element has all properties and events$ is a valid EventEmitter and emits on value change', fakeAsync(() => {
        const events$ = new EventEmitter<typeof TEST_OBJECT>();

        // Check if emits at first time
        events$.pipe(first()).subscribe(val => {
            expect(val.lang).toEqual('de');
        });

        const proxyObj = getSealedProxyObject(TEST_OBJECT, undefined, events$);
        tick();
        expect(TEST_OBJECT).not.toEqual(proxyObj);
        expect(proxyObj).toEqual(jasmine.objectContaining(TEST_OBJECT));
        expect(proxyObj.events$).toBeDefined();

        // Check if emits after value change
        events$.pipe(first()).subscribe(val => {
            expect(val.lang).toEqual('en');
        });

        expect(proxyObj.lang).toEqual(TEST_OBJECT.lang);
        proxyObj.lang = 'en';
        tick();
        expect(proxyObj.lang).not.toEqual(TEST_OBJECT.lang);
        expect(proxyObj.lang).toEqual('en');
    }));
});
