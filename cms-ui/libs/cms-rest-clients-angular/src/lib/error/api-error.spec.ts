import {ApiError} from './api-error';

describe('ApiError', () => {

    it('is an instanceof Error', () => {
        const error = new ApiError('test', 'failed');
        expect(error instanceof Error).toBe(true);
    });

    it('is an instanceof ApiError', () => {
        const error = new ApiError('test', 'failed');
        expect(error instanceof ApiError).toBe(true);
    });
});
