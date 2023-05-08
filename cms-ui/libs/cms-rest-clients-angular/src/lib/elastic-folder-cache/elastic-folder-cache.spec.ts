import {forkJoin, of as observableOf} from 'rxjs';
import {take} from 'rxjs/operators';

import {ElasticFolderCache} from './elastic-folder-cache';

const TEST_CACHE_SIZE = 5;
const TEST_CACHE_MAX_AGE = 10;

describe('ElasticFolderCache', () => {

    let fetchFunction: any;
    let elasticFolderCache: ElasticFolderCache;

    beforeEach(() => {
        let fetchCount = 0;
        fetchFunction = jasmine.createSpy('fetchFunction').and.callFake(() => {
            fetchCount ++;
            return observableOf({ folders: [{ id: fetchCount }] });
        });
        elasticFolderCache = new ElasticFolderCache({ maxAge: TEST_CACHE_MAX_AGE, maxCacheSize: TEST_CACHE_SIZE });
        elasticFolderCache.setFetchFunction(fetchFunction);
    });

    it('should invoke FetchFn on first call for parentId', () => {
        const result = elasticFolderCache.getAllSubfoldersOf(42);

        expect(fetchFunction).toHaveBeenCalledWith(42);
        result.pipe(take(1))
            .subscribe(ids => {
                expect(ids).toEqual([1, 42]);
            });
    });

    it('should not invoke FetchFn on subsequent calls for same parentId', () => {
        const result1 = elasticFolderCache.getAllSubfoldersOf(42);
        const result2 = elasticFolderCache.getAllSubfoldersOf(42);
        const result3 = elasticFolderCache.getAllSubfoldersOf(42);

        expect(fetchFunction).toHaveBeenCalledTimes(1);

        forkJoin(result1, result2, result3).pipe(take(1))
            .subscribe(results => {
                expect(results[0]).toEqual([1, 42]);
                expect(results[1]).toEqual([1, 42]);
                expect(results[2]).toEqual([1, 42]);
            });
    });

    it('should re-invoke FetchFn after timeout', () => {
        const oldDateNow = Date.now;
        let currentTime = Date.now();
        Date.now = () => currentTime;

        const result1 = elasticFolderCache.getAllSubfoldersOf(42);
        currentTime += (TEST_CACHE_MAX_AGE * 1000 + 1000);
        const result2 = elasticFolderCache.getAllSubfoldersOf(42);

        expect(fetchFunction).toHaveBeenCalledTimes(2);

        forkJoin(result1, result2).pipe(take(1))
            .subscribe(results => {
                expect(results[0]).toEqual([1, 42]);
                expect(results[1]).toEqual([2, 42]);
            });

        Date.now = oldDateNow;
    });

    it('should drop first entry when cache size exceeded', () => {
        elasticFolderCache.getAllSubfoldersOf(42);

        // fill up the cache
        elasticFolderCache.getAllSubfoldersOf(43);
        elasticFolderCache.getAllSubfoldersOf(44);
        elasticFolderCache.getAllSubfoldersOf(45);
        elasticFolderCache.getAllSubfoldersOf(46);
        elasticFolderCache.getAllSubfoldersOf(47);

        // getting the first key should trigger a new fetch
        elasticFolderCache.getAllSubfoldersOf(42);

        expect(fetchFunction).toHaveBeenCalledTimes(7);
        expect(fetchFunction.calls.argsFor(6)).toEqual([42]);
    });

});
