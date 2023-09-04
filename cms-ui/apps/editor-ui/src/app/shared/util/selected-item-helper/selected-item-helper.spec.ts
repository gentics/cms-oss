import { fakeAsync, tick } from '@angular/core/testing';
import { ItemInNode, Page, PageResponse, Raw } from '@gentics/cms-models';
import { getExamplePageData } from '@gentics/cms-models/testing/test-data.mock';
import { cloneDeep } from 'lodash-es';
import { Observable, Subscription } from 'rxjs';
import { SelectedItemHelper } from './selected-item-helper';

const DEFAULT_NODE_ID = 4711;
const STICKY_NODE_ID = 1234;

const PAGE_ID = 1000;

type PageWithNodeId = ItemInNode<Page<Raw>>;

describe('SelectedItemHelper', () => {

    let selectedItemHelper: SelectedItemHelper<PageWithNodeId>;
    let mockFolderActionsService: MockFolderActions;
    let getItemSpy: jasmine.Spy;

    beforeEach(() => {
        mockFolderActionsService = new MockFolderActions();
        selectedItemHelper = new SelectedItemHelper('page', DEFAULT_NODE_ID, mockFolderActionsService as any);
        getItemSpy = spyOn(mockFolderActionsService, 'getItem').and.callThrough();
    });

    it('setSelectedItem(item) works and emits a value from selectedItem$', fakeAsync(() => {
        const expectedPage = getExamplePageWithNodeId({ pageId: PAGE_ID, nodeId: STICKY_NODE_ID });
        const page = cloneDeep(expectedPage);
        let emittedItem: PageWithNodeId;
        const sub = selectedItemHelper.selectedItem$.subscribe(item => emittedItem = item);

        selectedItemHelper.setSelectedItem(page);
        sub.unsubscribe();
        expect(emittedItem).toEqual(expectedPage);
        expect(emittedItem).toBe(page);
        expect(getItemSpy).not.toHaveBeenCalled();
        expect(selectedItemHelper.selectedItem).toBe(emittedItem);
    }));

    it('setSelectedItem(null) works and emits a value from selectedItem$', fakeAsync(() => {
        let emittedItem: PageWithNodeId;
        const sub = selectedItemHelper.selectedItem$.subscribe(item => emittedItem = item);

        selectedItemHelper.setSelectedItem(null);
        sub.unsubscribe();
        expect(emittedItem).toBeNull();
        expect(getItemSpy).not.toHaveBeenCalled();
        expect(selectedItemHelper.selectedItem).toBe(emittedItem);
    }));

    it('setSelectedItem(itemId) works and emits a value from selectedItem$', fakeAsync(() => {
        const expectedPage = getExamplePageWithNodeId({ pageId: PAGE_ID, nodeId: DEFAULT_NODE_ID });
        getItemSpy.and.returnValue(mockResponseObservable(expectedPage));
        let emittedItem: PageWithNodeId;
        const sub = selectedItemHelper.selectedItem$.subscribe(item => emittedItem = item);

        selectedItemHelper.setSelectedItem(PAGE_ID);
        tick();
        sub.unsubscribe();
        expect(emittedItem).toEqual(expectedPage);
        expect(getItemSpy).toHaveBeenCalledWith(PAGE_ID, 'page', { nodeId: DEFAULT_NODE_ID })
        expect(selectedItemHelper.selectedItem).toBe(emittedItem);
    }));

    it('setSelectedItem(itemId, nodeId) works and emits a value from selectedItem$', fakeAsync(() => {
        const expectedPage = getExamplePageWithNodeId({ pageId: PAGE_ID, nodeId: STICKY_NODE_ID });
        getItemSpy.and.returnValue(mockResponseObservable(expectedPage));
        let emittedItem: PageWithNodeId;
        const sub = selectedItemHelper.selectedItem$.subscribe(item => emittedItem = item);

        selectedItemHelper.setSelectedItem(PAGE_ID, STICKY_NODE_ID);
        tick();
        sub.unsubscribe();
        expect(emittedItem).toEqual(expectedPage);
        expect(getItemSpy).toHaveBeenCalledWith(PAGE_ID, 'page', { nodeId: STICKY_NODE_ID });
        expect(selectedItemHelper.selectedItem).toBe(emittedItem);
    }));

    it('sequences of setSelectedItem() work and emit values from selectedItem$', fakeAsync(() => {
        let expectedPage = getExamplePageWithNodeId({ pageId: PAGE_ID, nodeId: STICKY_NODE_ID });
        let emittedItem: PageWithNodeId;
        const sub = selectedItemHelper.selectedItem$.subscribe(item => emittedItem = item);

        // First set an item with an itemId and a nodeId.
        getItemSpy.and.returnValue(mockResponseObservable(expectedPage));
        selectedItemHelper.setSelectedItem(PAGE_ID, STICKY_NODE_ID);
        tick();
        expect(emittedItem).toEqual(expectedPage);
        expect(getItemSpy).toHaveBeenCalledWith(PAGE_ID, 'page', { nodeId: STICKY_NODE_ID });
        expect(selectedItemHelper.selectedItem).toBe(emittedItem);

        // Set an item object.
        emittedItem = null;
        getItemSpy.calls.reset();
        expectedPage = getExamplePageWithNodeId({ pageId: PAGE_ID + 1, nodeId: DEFAULT_NODE_ID });
        selectedItemHelper.setSelectedItem(cloneDeep(expectedPage));
        tick();
        expect(emittedItem).toEqual(expectedPage);
        expect(getItemSpy).not.toHaveBeenCalled();
        expect(selectedItemHelper.selectedItem).toBe(emittedItem);

        // Set an item with just an itemId.
        emittedItem = null;
        expectedPage = getExamplePageWithNodeId({ pageId: PAGE_ID + 2, nodeId: DEFAULT_NODE_ID });
        getItemSpy.and.returnValue(mockResponseObservable(expectedPage));
        selectedItemHelper.setSelectedItem(PAGE_ID);
        tick();
        expect(emittedItem).toEqual(expectedPage);
        expect(getItemSpy).toHaveBeenCalledWith(PAGE_ID, 'page', { nodeId: DEFAULT_NODE_ID });
        expect(selectedItemHelper.selectedItem).toBe(emittedItem);

        sub.unsubscribe();
    }));

    it('loadingError$ emits errors and setSelectedItem() still works afterwards', fakeAsync(() => {
        let emittedItem: PageWithNodeId;
        let emittedError: any;
        const sub = new Subscription();
        sub.add(selectedItemHelper.selectedItem$.subscribe(item => emittedItem = item));
        sub.add(selectedItemHelper.loadingError$.subscribe(error => emittedError = error));

        // Simulate an error.
        const expectedError = new Error('Simulated error');
        getItemSpy.and.returnValue(Observable.throw(expectedError));
        selectedItemHelper.setSelectedItem(PAGE_ID);
        expect(getItemSpy).toHaveBeenCalledWith(PAGE_ID, 'page', { nodeId: DEFAULT_NODE_ID });
        expect(emittedItem).toBeFalsy();
        expect(emittedError).toBe(expectedError);

        // Try setting the same page again, this time it will work.
        emittedError = null;
        getItemSpy.calls.reset();
        const expectedPage = getExamplePageWithNodeId({ pageId: PAGE_ID, nodeId: DEFAULT_NODE_ID });
        getItemSpy.and.returnValue(mockResponseObservable(expectedPage));
        selectedItemHelper.setSelectedItem(PAGE_ID);
        tick();
        expect(getItemSpy).toHaveBeenCalledWith(PAGE_ID, 'page', { nodeId: DEFAULT_NODE_ID });
        expect(emittedItem).toEqual(expectedPage);
        expect(emittedError).toBeFalsy();

        sub.unsubscribe();
    }));

});

function getExamplePageWithNodeId({ pageId, nodeId }: { pageId: number, nodeId: number }): PageWithNodeId {
    const page: PageWithNodeId = getExamplePageData({ id: pageId }) as any;
    page.nodeId = nodeId;
    return page;
}

/**
 * Creates an Observable with a mocked PageResponse with a deep clone of the specified page
 * and page.nodeId removed (if it existed).
 */
function mockResponseObservable(page: Page<Raw>): Observable<PageResponse> {
    page = cloneDeep(page);
    // Loaded pages don't have a nodeId.
    delete (<any> page)['nodeId'];

    const response: Partial<PageResponse> = {
        page: page,
    };
    return Observable.of(response as any).delay(0);
}

class MockFolderActions {
    getItem(): any { }
}
