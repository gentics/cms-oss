import { FolderListResponse } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';

interface CacheEntry {
    timestamp: number;
    value: Observable<number[]>;
}

/**
 * Elastic search has no notion of the folder heirarchy of a node. To implement a recursive search from a given folder in a node,
 * it is therefore necessary to first get a list of all subfolders of the given folder (recusively). This list of folderIds can
 * then be used as a contraint in the elastic query.
 *
 * To prevent unnessary calls to get lists of folders, this service should be used rather than directly calling the API on each
 * search request (for starters this would generate 4 requests immediately, one for each item type).
 *
 * This service implements a simple FIFO cache which remembers the results for a given parent folder id.
 *
 * To prevent stale data being returned, a simple timeout mechanism is used.
 */
export class ElasticFolderCache {

    private cache = new Map<number, CacheEntry>();
    private MAX_CACHE_SIZE = 30;
    private MAX_AGE_IN_SECONDS = 60;
    private fetchFoldersFn: (parentId: number) => Observable<FolderListResponse> = (parentId: number) => {
        throw new Error('No fetchFn has been set. Please call setFetchFunction() before attempting to call getAllSubFoldersOf().');
    }

    constructor(config?: { maxCacheSize: number; maxAge: number; }) {
        // config arg is for better testability
        if (config) {
            this.MAX_CACHE_SIZE = config.maxCacheSize;
            this.MAX_AGE_IN_SECONDS = config.maxAge;
        }
    }

    setFetchFunction(fetchFn: (parentId: number) => Observable<FolderListResponse>): void {
        this.fetchFoldersFn = fetchFn;
    }

    getAllSubfoldersOf(parentId: number): Observable<number[]> {
        const now = this.now();

        if (this.cache.has(parentId)) {
            const entry = this.cache.get(parentId);
            if (this.MAX_AGE_IN_SECONDS < now - entry.timestamp) {
                this.cache.delete(parentId);
            } else {
                // Cache hit - return the cached values
                return this.cache.get(parentId).value;
            }
        }

        if (this.MAX_CACHE_SIZE <= this.cache.size) {
            this.trim();
        }

        const folderIds$ = this.fetchFoldersFn(parentId).pipe(
            map(res => res.folders.map(f => f.id).concat(parentId)),
            shareReplay(1),
        );

        this.cache.set(parentId, {
            timestamp: this.now(),
            value: folderIds$,
        });

        return folderIds$;
    }

    private trim(): void {
        const max = this.MAX_CACHE_SIZE;
        while (this.cache.size >= max) {
            const popKey = this.cache.keys().next().value;
            this.cache.delete(popKey);
        }
    }

    private now(): number {
        return Math.round(Date.now() / 1000);
    }
}
