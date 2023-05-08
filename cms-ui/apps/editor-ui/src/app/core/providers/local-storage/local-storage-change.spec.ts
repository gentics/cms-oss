import {LocalStorageChange} from './local-storage-change';

describe('LocalStorageChange:', () => {

    let originalJsonParse: any;
    beforeEach(() => {
        originalJsonParse = JSON.parse;
        JSON.parse = jasmine.createSpy('JSON.parse', JSON.parse).and.callThrough();
    });
    afterEach(() => {
        JSON.parse = originalJsonParse;
    });

    it('copies the key property of the input', () => {
        let change = new LocalStorageChange({ key: 'someKey' });
        expect(change.key).toBe('someKey');
    });

    it('parses the JSON string of oldValue and newValue', () => {
        let change = new LocalStorageChange({
            key: 'testKey',
            newValue: '{"color":"green"}',
            oldValue: '{"color":"red"}'
        });

        expect(change.newValue).toEqual({ color: 'green' });
        expect(change.oldValue).toEqual({ color: 'red' });
        expect(change.newValue).not.toEqual('{"color":"green"}');
        expect(change.oldValue).not.toEqual('{"color":"red"}');
    });

    it('only parses JSON when oldValue or newValue are accessed', () => {
        let change = new LocalStorageChange({
            key: 'testKey',
            newValue: '{"color":"green"}',
            oldValue: '{"color":"red"}'
        });

        expect(JSON.parse).not.toHaveBeenCalled();
        let testme = change.oldValue;
        expect(JSON.parse).toHaveBeenCalledTimes(1);
        testme = change.newValue;
        expect(JSON.parse).toHaveBeenCalledTimes(2);
    });

    it('does not re-parse JSON when oldValue or newValue are accessed multiple times', () => {
        let change = new LocalStorageChange({
            key: 'testKey',
            newValue: '{"color":"green"}',
            oldValue: '{"color":"red"}'
        });

        expect(JSON.parse).not.toHaveBeenCalled();

        let testme = change.oldValue;
        expect(JSON.parse).toHaveBeenCalledTimes(1);
        testme = change.oldValue;
        expect(JSON.parse).toHaveBeenCalledTimes(1);
        testme = change.oldValue;
        expect(JSON.parse).toHaveBeenCalledTimes(1);

        testme = change.newValue;
        expect(JSON.parse).toHaveBeenCalledTimes(2);
        testme = change.newValue;
        expect(JSON.parse).toHaveBeenCalledTimes(2);
        testme = change.newValue;
        expect(JSON.parse).toHaveBeenCalledTimes(2);
    });

});
