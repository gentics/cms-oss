import { ErrorHandler } from '@admin-ui/core/providers/error-handler/error-handler.service';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SpyEventTarget } from '../../../../testing/spy-event-target';
import { EDITOR_UI_LOCAL_STORAGE_PREFIX, EditorUiLocalStorageService } from './editor-ui-local-storage.service';
import { LocalStorageChange } from './local-storage-change';

@Injectable()
class MockErrorHandler {
    catch = jasmine.createSpy('ErrorHandler.catch');
}

describe('EditorUiLocalStorageService', () => {

    let localStorage: EditorUiLocalStorageService;
    let mockEventProvider: SpyEventTarget;
    let mockStorageProvider: Storage;
    let mockErrorHandler: MockErrorHandler;

    beforeEach(() => {
        mockEventProvider = new SpyEventTarget('window');
        mockStorageProvider = jasmine.createSpyObj('StorageProvider', ['getItem', 'setItem', 'removeItem']);
        mockErrorHandler = new MockErrorHandler();
        localStorage = new EditorUiLocalStorageService( mockErrorHandler as unknown as ErrorHandler );
        localStorage.storageProvider = mockStorageProvider;
        localStorage.eventProvider = mockEventProvider;
        localStorage.init();
    });

    it('change$ emits when the LocalStorage was updated in another tab', () => {
        const changes = [] as LocalStorageChange[];
        const sub = localStorage.change$.subscribe(c => changes.push(c));
        const event: any = {
            type: 'storage',
            key: EDITOR_UI_LOCAL_STORAGE_PREFIX + 'testKey',
            newValue: '{"color":"green"}',
            oldValue: '{"color":"red"}',
        };

        expect(changes.length).toEqual(0);
        mockEventProvider.dispatchEvent(event);
        expect(changes.length).toEqual(1);
    });

    describe('getForAllUsers', () => {
        it('gets an item value from localStorage', () => {
            mockStorageProvider.getItem = jasmine.createSpy('getItem').and.returnValue('"data"');
            const data = localStorage.getForAllUsers('someData');
            expect(mockStorageProvider.getItem).toHaveBeenCalledWith(EDITOR_UI_LOCAL_STORAGE_PREFIX + 'someData');
            expect(data).toBe('data');
        });

        it('parses the localStorage value as JSON', () => {
            mockStorageProvider.getItem = () => '{"a":1,"b":"data","c":true}';
            const data = localStorage.getForAllUsers('someData');
            expect(data).toEqual({
                a: 1,
                b: 'data',
                c: true,
            });
        });

        it('does not return an unprefixed localStorage item', () => {
            mockStorageProvider.getItem = jasmine.createSpy('getItem').and.callFake(
                (key: string) => (key === 'someData') ? '"example"' : '');
            const data = localStorage.getForAllUsers('someData');
            expect(data).not.toBe('example');
        });
    });

    describe('setForAllUsers', () => {
        it('sets a localStorage item to the passed value', () => {
            localStorage.setForAllUsers('someData', 'example');
            expect(mockStorageProvider.setItem).toHaveBeenCalledWith(EDITOR_UI_LOCAL_STORAGE_PREFIX + 'someData', '"example"');
        });

        it('encodes the value as JSON', () => {
            let savedValue: string;
            mockStorageProvider.setItem = (key, value) => { savedValue = value; };
            localStorage.setForAllUsers('someData', { a: 1, b: 'data', c: true });
            expect(typeof savedValue).toBe('string');
            expect(JSON.parse(savedValue)).toEqual({ a: 1, b: 'data', c: true });
        });

        it('does not set an unprefixed localStorage item', () => {
            localStorage.setForAllUsers('someData', '1234');
            expect(mockStorageProvider.setItem).not.toHaveBeenCalledWith('someData', jasmine.anything());
        });
    });

    describe('getForUser', () => {
        it('gets an item value from localStorage', () => {
            mockStorageProvider.getItem = jasmine.createSpy('getItem').and.returnValue('"data"');
            const data = localStorage.getForUser(1234, 'someData');
            expect(mockStorageProvider.getItem).toHaveBeenCalledWith(EDITOR_UI_LOCAL_STORAGE_PREFIX + 'USER-1234_someData');
            expect(data).toBe('data');
        });

        it('parses the localStorage value as JSON', () => {
            mockStorageProvider.getItem = () => '{"a":1,"b":"data","c":true}';
            const data = localStorage.getForUser(1234, 'someData');
            expect(data).toEqual({
                a: 1,
                b: 'data',
                c: true,
            });
        });

        it('does not return an unprefixed localStorage item', () => {
            mockStorageProvider.getItem = jasmine.createSpy('getItem').and.callFake(
                (key: string) => (key === 'someData') ? '"example"' : '');
            const data = localStorage.getForUser(1234, 'someData');
            expect(data).not.toBe('example');
        });

        it('does not return a "for-all-users" localStorage item', () => {
            mockStorageProvider.getItem = jasmine.createSpy('getItem').and.callFake(
                (key: string) => (key === EDITOR_UI_LOCAL_STORAGE_PREFIX + 'someData') ? '"example"' : '');
            const data = localStorage.getForUser(1234, 'someData');
            expect(data).not.toBe('example');
        });
    });

    describe('setForUser', () => {
        it('sets a localStorage item to the passed value', () => {
            localStorage.setForUser(1234, 'someData', 'example');
            expect(mockStorageProvider.setItem)
                .toHaveBeenCalledWith(EDITOR_UI_LOCAL_STORAGE_PREFIX + 'USER-1234_someData', '"example"');
        });

        it('encodes the value as JSON', () => {
            let savedValue: string;
            mockStorageProvider.setItem = (key, value) => { savedValue = value; };
            localStorage.setForUser(1234, 'someData', { a: 1, b: 'data', c: true });
            expect(typeof savedValue).toBe('string');
            expect(JSON.parse(savedValue)).toEqual({ a: 1, b: 'data', c: true });
        });

        it('does not set an unprefixed localStorage item', () => {
            localStorage.setForUser(1234, 'someData', '1234');
            expect(mockStorageProvider.setItem).not.toHaveBeenCalledWith('someData', jasmine.anything());
        });

        it('does not set a "for all users" localStorage item', () => {
            localStorage.setForUser(1234, 'someData', '1234');
            expect(mockStorageProvider.setItem).not.toHaveBeenCalledWith(EDITOR_UI_LOCAL_STORAGE_PREFIX + 'someData', jasmine.anything());
        });
    });

    it('getSid returns sid from localStorage', () => {
        mockStorageProvider.getItem = jasmine.createSpy('getItem').and.returnValue('42');
        const sid = localStorage.getSid();
        expect(mockStorageProvider.getItem).toHaveBeenCalledWith(EDITOR_UI_LOCAL_STORAGE_PREFIX + 'sid');
        expect(sid).toBe(42);
    });

    it('setSid invokes setItem with correct parameters', () => {
        localStorage.setSid(42);
        expect(mockStorageProvider.setItem).toHaveBeenCalledWith(EDITOR_UI_LOCAL_STORAGE_PREFIX + 'sid', '42');
    });

});
