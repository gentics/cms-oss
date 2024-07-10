import { ChangeDetectorRef, Component } from '@angular/core';
import { TestBed, inject } from '@angular/core/testing';
import { Favourite } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { componentTest } from '../../../../testing';
import { ApplicationStateService, STATE_MODULES } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { IsFavouritePipe } from './is-favourite.pipe';

describe('IsFavouritePipe', () => {

    let state: TestApplicationState;
    let changeDetector: SpyChangeDetector;

    beforeEach(() => {
        changeDetector = new SpyChangeDetector();

        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: ChangeDetectorRef, useValue: changeDetector },
            ],
            declarations: [TestComponent, IsFavouritePipe],
        });
        state = TestBed.get(ApplicationStateService);
    });

    function mockFavouritesState(list: Favourite[]): void {
        state.mockState({ favourites: { list } });
    }

    it('unsubscribes from state changes on destroy',
        inject([ApplicationStateService, ChangeDetectorRef],
            (applicationStateService: ApplicationStateService, changeDetectorRef: ChangeDetectorRef) => {
                const pipe = new IsFavouritePipe(applicationStateService, changeDetectorRef);
                pipe.ngOnDestroy();
            },
        ),
    );

    it('transforms to false if the input is undefined or null',
        inject([ApplicationStateService, ChangeDetectorRef],
            (applicationStateService: ApplicationStateService, changeDetectorRef: ChangeDetectorRef) => {
                const pipe = new IsFavouritePipe(applicationStateService, changeDetectorRef);
                expect(pipe.transform(undefined)).toBe(false);
                expect(pipe.transform(null)).toBe(false);
            },
        ),
    );

    it('transforms to false if the favourites are not retrieved yet',
        inject([ApplicationStateService, ChangeDetectorRef],
            (applicationStateService: ApplicationStateService, changeDetectorRef: ChangeDetectorRef) => {
                const pipe = new IsFavouritePipe(applicationStateService, changeDetectorRef);
                const result = pipe.transform({ nodeId: 7, globalId: 'AX-15', id: 23, type: 'folder', name: 'Folder 23' });
                expect(result).toBe(false);
            },
        ),
    );

    it('transforms to true if the input is in favourites state',
        inject([ApplicationStateService, ChangeDetectorRef],
            (applicationStateService: ApplicationStateService, changeDetectorRef: ChangeDetectorRef) => {
                const pipe = new IsFavouritePipe(applicationStateService, changeDetectorRef);
                state.mockState({
                    favourites: {
                        list: [],
                        loaded: true,
                    },
                    folder: {
                        activeNode: 1,
                    },
                });
                let result = pipe.transform({ id: 23, type: 'folder', globalId: 'folder-23', nodeId: 1, name: 'Folder 23' });
                expect(result).toBe(false);

                mockFavouritesState([ { id: 23, type: 'folder', name: 'My folder', globalId: 'folder-23', nodeId: 1 } ]);
                result = pipe.transform({ id: 23, type: 'folder', globalId: 'folder-23', nodeId: 1, name: 'Folder 23' });
                expect(result).toBe(true);
            },
        ),
    );

    it('transform to true if "all" input array entities are in favourites state',
        inject([ApplicationStateService, ChangeDetectorRef],
            (applicationStateService: ApplicationStateService, changeDetectorRef: ChangeDetectorRef) => {
                const pipe = new IsFavouritePipe(applicationStateService, changeDetectorRef);
                let result = pipe.transform({ id: 23, type: 'folder', globalId: 'folder-23', nodeId: 1, name: 'Folder Twenty Three' });
                expect(result).toBe(false);

                mockFavouritesState([
                    { id: 23, type: 'folder', name: 'Folder Twenty Three', globalId: 'folder-23', nodeId: 1 },
                    { id: 47, type: 'folder', name: 'Folder Fourty Seven', globalId: 'folder-47', nodeId: 1 },
                    { id: 11, type: 'page', name: 'Page Eleven', globalId: 'page-11', nodeId: 2 },
                ]);

                let input = [
                    { id: 23, type: 'folder', globalId: 'folder-23', nodeId: 1, name: 'Folder Twenty Three' },
                    { id: 11, type: 'page', globalId: 'page-11', nodeId: 2, name: 'Page Two' },
                ];
                result = pipe.transform(input, 'all');
                expect(result).toBe(true);

                input = [
                    { id: 11, type: 'page', globalId: 'page-11', nodeId: 1, name: 'Page Eleven' },
                    { id: 66, type: 'page', globalId: 'page-66', nodeId: 2, name: 'Page Sixty Six' },
                ];
                result = pipe.transform(input, 'all');
                expect(result).toBe(false);

                input = [
                    { id: 23, type: 'folder', globalId: 'folder-23', nodeId: 1, name: 'Folder Twenty Three' },
                    { id: 66, type: 'page', globalId: 'page-66', nodeId: 1, name: 'Folder Sixty Six' },
                ];
                result = pipe.transform(input, 'all');
                expect(result).toBe(false);
            },
        ),
    );

    it('transform to true if "any" input array entity is in favourites state',
        inject([ApplicationStateService, ChangeDetectorRef],
            (applicationStateService: ApplicationStateService, changeDetectorRef: ChangeDetectorRef) => {
                const pipe = new IsFavouritePipe(applicationStateService, changeDetectorRef);
                let result = pipe.transform({ id: 23, type: 'folder', globalId: 'folder-23', name: 'Folder Twenty Three' });
                expect(result).toBe(false);

                mockFavouritesState([
                    { id: 23, type: 'folder', name: 'Folder Twenty Three', globalId: 'folder-23', nodeId: 1 },
                    { id: 47, type: 'folder', name: 'Folder Fourty Seven', globalId: 'folder-47', nodeId: 1 },
                    { id: 11, type: 'page', name: 'Folder Eleven', globalId: 'page-11', nodeId: 2 },
                ]);

                result = pipe.transform({ id: 23, type: 'folder', globalId: 'folder-23', nodeId: 1, name: 'Folder Twenty Three' });
                expect(result).toBe(true);

                let input = [
                    { id: 77, type: 'folder', globalId: 'folder-77', nodeId: 1, name: 'Folder Seventy Seven' },
                    { id: 66, type: 'page', globalId: 'page-66', nodeId: 1, name: 'Page Sixty Six' },
                ];
                result = pipe.transform(input, 'any');
                expect(result).toBe(false);

                input = [
                    { id: 47, type: 'folder', globalId: 'folder-47', nodeId: 1, name: 'Folder Fourty Seven' },
                    { id: 66, type: 'page', globalId: 'page-66', nodeId: 1, name: 'Page Sixty Six' },
                ];
                result = pipe.transform(input, 'any');
                expect(result).toBe(true);
            },
        ),
    );

    it('works with elements with a "globalId" property',
        inject([ApplicationStateService, ChangeDetectorRef],
            (applicationStateService: ApplicationStateService, changeDetectorRef: ChangeDetectorRef) => {
                const pipe = new IsFavouritePipe(applicationStateService, changeDetectorRef);
                const items: Favourite[] = [
                    { name: 'Favourite 1', type: 'page', id: 77, globalId: 'page-77', nodeId: 1 },
                    { name: 'Favourite 2', type: 'folder', id: 22, globalId: 'folder-22', nodeId: 1 },
                    { name: 'Favourite 3', type: 'file', id: 33, globalId: 'file-33', nodeId: 1 },
                ];
                let result = pipe.transform(items[0]);
                expect(result).toBe(false);

                mockFavouritesState(items);

                result = pipe.transform(items[0]);
                expect(result).toBe(true);

                const itemWithUpperCaseGlobalId = Object.assign({}, items[0], { globalId: items[0].globalId.toUpperCase() });
                result = pipe.transform(itemWithUpperCaseGlobalId);
                expect(result).toBe(false, 'should not match globalId case insensitive');
            },
        ),
    );

    it('marks itself for change detection when the favourites state changes',
        inject([ApplicationStateService, ChangeDetectorRef],
            (applicationStateService: ApplicationStateService, changeDetectorRef: ChangeDetectorRef) => {
                const pipe = new IsFavouritePipe(applicationStateService, changeDetectorRef);
                changeDetector.markForCheck.calls.reset();

                const itemA: Favourite = { name: 'My folder', type: 'folder', id: 23, globalId: 'folder-23', nodeId: 1 };
                const itemB: Favourite = { name: 'My page', type: 'page', id: 7, globalId: 'page-7', nodeId: 1 };

                state.mockState({
                    favourites: {
                        list: [ itemA ],
                    },
                });
                expect(changeDetector.markForCheck).toHaveBeenCalledTimes(1);

                state.mockState({
                    favourites: {
                        list: [ itemA, itemB ],
                    },
                });
                expect(changeDetector.markForCheck).toHaveBeenCalledTimes(2);
            },
        ),
    );

    it('marks itself for change detection when a different node is selected',
        inject([ApplicationStateService, ChangeDetectorRef],
            (applicationStateService: ApplicationStateService, changeDetectorRef: ChangeDetectorRef) => {
                const pipe = new IsFavouritePipe(applicationStateService, changeDetectorRef);
                state.mockState({
                    favourites: {
                        list: [],
                    },
                });
                changeDetector.markForCheck.calls.reset();

                state.mockState({ folder: { activeNode: 1 } });
                expect(changeDetector.markForCheck).toHaveBeenCalledTimes(1);

                state.mockState({ folder: { activeNode: 7 } });
                expect(changeDetector.markForCheck).toHaveBeenCalledTimes(2);
            },
        ),
    );

    describe('when used in a component', () => {

        it('transforms falsy if the passed item is not a favourite',
            componentTest(() => TestComponent, (fixture, component) => {
                state.mockState({
                    favourites: {
                        list: [{ type: 'folder', id: 44, globalId: 'folder-44', name: 'some folder', nodeId: 7 }],
                    },
                    folder: {
                        activeNode: 7,
                    },
                });

                component.item = { type: 'not-a-folder', id: 77, globalId: 'some other item' };

                fixture.detectChanges();
                expect(fixture.nativeElement.innerText).toEqual('no');
            }),
        );

        it('transforms falsy if the item is favourited in a different node',
            componentTest(() => TestComponent, (fixture, component) => {
                state.mockState({
                    favourites: {
                        list: [{ nodeId: 222, type: 'folder', id: 44, globalId: 'folder-44', name: 'some folder' }],
                    },
                    folder: {
                        activeNode: 111,
                    },
                });
                component.item = { type: 'folder', id: 44, globalId: 'some folder' };

                fixture.detectChanges();
                expect(fixture.nativeElement.innerText).toEqual('no');
            }),
        );

        it('transforms truthy if the passed item is already a favourite',
            componentTest(() => TestComponent, (fixture, component) => {
                state.mockState({
                    favourites: {
                        list: [{ type: 'folder', id: 44, globalId: 'folder-44', name: 'some folder', nodeId: 7 }],
                    },
                    folder: {
                        activeNode: 7,
                    },
                });
                component.item = { type: 'folder', id: 44, globalId: 'folder-44' };

                fixture.detectChanges();
                expect(fixture.nativeElement.innerText).toEqual('yes');
            }),
        );

        it('reacts to state changes',
            componentTest(() => TestComponent, (fixture, component) => {
                state.mockState({
                    favourites: {
                        list: [],
                    },
                    folder: {
                        activeNode: 7,
                    },
                });
                component.item = { type: 'folder', id: 44, globalId: 'folder-44' };

                fixture.detectChanges();
                expect(fixture.nativeElement.innerText).toEqual('no');

                state.mockState({
                    favourites: {
                        list: [{ type: 'folder', id: 44, globalId: 'folder-44', name: 'some folder', nodeId: 7 }],
                    },
                });
                fixture.detectChanges();
                expect(fixture.nativeElement.innerText).toEqual('yes');
            }),
        );

    });
});

class SpyChangeDetector {
    markForCheck = jasmine.createSpy('markForCheck');
}

@Component({
    template: `
        <div *ngIf="item | isFavourite">yes</div>
        <div *ngIf="!(item | isFavourite)">no</div>`,
})
class TestComponent {
    item = { type: 'folder', id: 44, globalId: 'folder-44' };
}
