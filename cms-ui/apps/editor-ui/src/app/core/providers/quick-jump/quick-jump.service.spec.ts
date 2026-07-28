import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ResourceUrlBuilder } from '../resource-url-builder/resource-url-builder';
import { QuickJumpService } from './quick-jump.service';

const testPageId = 17;
const testNodeId = 5;

describe('QuickJumpService', () => {

    let httpTestingController: HttpTestingController;
    let quickJumpService: QuickJumpService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                QuickJumpService,
                { provide: ResourceUrlBuilder, useClass: MockResourceUrlBuilder },
            ],
            imports: [ HttpClientTestingModule ],
        });
        httpTestingController = TestBed.inject(HttpTestingController);
        quickJumpService = TestBed.inject(QuickJumpService);
    });

    afterEach(() => {
        // After every test, assert that there are no more pending requests.
        httpTestingController.verify();
    });

    describe('searchPageById()', () => {

        it('resolves to undefined if no match', fakeAsync(() => {
            let promiseResolved = false;
            quickJumpService.searchPageById(testPageId, testNodeId)
                .then(page => {
                    expect(page).toBeUndefined();
                    promiseResolved = true;
                });

            const req = httpTestingController.expectOne(testPageId.toString());
            req.flush('');
            tick();
            expect(promiseResolved).toBe(true);
        }));

        it('resolves to a pageId, nodeId pair with the value of nodeId equal to the current node', fakeAsync(() => {
            const response = `
                <div class="ac_node">Channel One</div>
                <div class="ac_folder">News</div>
                <div class="ac_page" page_id="${testPageId}" node_id="3">Test Page</div>
                <div class="ac_node">Channel Three</div>
                <div class="ac_folder">News</div>
                <div class="ac_page" page_id="${testPageId}" node_id="6">Test Page</div>
                <div class="ac_node">Channel Two</div>
                <div class="ac_folder">News</div>
                <div class="ac_page" page_id="${testPageId}" node_id="5">Test Page</div>
                <div class="ac_node">GCN5 Demo</div>
                <div class="ac_folder">News</div>
                <div class="ac_page" page_id="${testPageId}" node_id="1">Test Page</div>
            `;

            let promiseResolved = false;
            quickJumpService.searchPageById(testPageId, testNodeId)
                .then(page => {
                    expect(page).toBeDefined();
                    expect(page.id).toBe(testPageId);
                    expect(page.nodeId).toBe(testNodeId);
                    promiseResolved = true;
                });

            const req = httpTestingController.expectOne(testPageId.toString());
            req.flush(response);
            tick();
            expect(promiseResolved).toBe(true);
        }));

        it('resolves to a pageId, nodeId pair with the lowest value of nodeId if the page does not exist in the current node', fakeAsync(() => {
            const response = `
                <div class="ac_node">Channel One</div>
                <div class="ac_folder">News</div>
                <div class="ac_page" page_id="${testPageId}" node_id="3">Test Page</div>
                <div class="ac_node">Channel Three</div>
                <div class="ac_folder">News</div>
                <div class="ac_page" page_id="${testPageId}" node_id="6">Test Page</div>
                <div class="ac_node">Channel Two</div>
                <div class="ac_folder">News</div>
                <div class="ac_page" page_id="${testPageId}" node_id="5">Test Page</div>
                <div class="ac_node">GCN5 Demo</div>
                <div class="ac_folder">News</div>
                <div class="ac_page" page_id="${testPageId}" node_id="1">Test Page</div>
            `;

            let promiseResolved = false;
            quickJumpService.searchPageById(testPageId, 4)
                .then(page => {
                    expect(page).toBeDefined();
                    expect(page.id).toBe(testPageId);
                    expect(page.nodeId).toBe(1);
                    promiseResolved = true;
                });

            const req = httpTestingController.expectOne(testPageId.toString());
            req.flush(response);
            tick();
            expect(promiseResolved).toBe(true);
        }));

        it('does not return pages with a different ID than the input', fakeAsync(() => {
            const response = `
                <div class="ac_node">Channel One</div>
                <div class="ac_folder">News</div>
                <div class="ac_page" page_id="${testPageId + 1}" node_id="3">Irrelevant Page</div>
                <div class="ac_node">Channel Three</div>
                <div class="ac_folder">News</div>
                <div class="ac_page" page_id="${testPageId + 2}" node_id="6">Irrelevant Page</div>
                <div class="ac_node">Channel Two</div>
                <div class="ac_folder">News</div>
                <div class="ac_page" page_id="${testPageId + 3}" node_id="5">Irrelevant Page</div>
                <div class="ac_node">GCN5 Demo</div>
                <div class="ac_folder">News</div>
                <div class="ac_page" page_id="${testPageId + 4}" node_id="1">Irrelevant Page</div>
            `;

            let promiseResolved = false;
            quickJumpService.searchPageById(testPageId, testNodeId)
                .then(page => {
                    expect(page).toBeUndefined();
                    promiseResolved = true;
                });

            const req = httpTestingController.expectOne(testPageId.toString());
            req.flush(response);
            tick();
            expect(promiseResolved).toBe(true);
        }));

    });

});

class MockResourceUrlBuilder {
    autocomplete(term: string | number): string {
        return String(term);
    }
}
