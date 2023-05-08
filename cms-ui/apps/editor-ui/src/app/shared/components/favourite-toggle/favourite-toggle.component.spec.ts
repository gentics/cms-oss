import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { FavouritesService } from '../../../core/providers/favourites/favourites.service';
import { ApplicationStateService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { IsFavouritePipe } from '../../pipes/is-favourite/is-favourite.pipe';
import { FavouriteToggle } from './favourite-toggle.component';

describe('FavouriteToggle', () => {

    let state: TestApplicationState;
    let favouritesService: MockFavouritesService;

    beforeEach(() => {
        favouritesService = new MockFavouritesService();

        configureComponentTest({
            imports: [GenticsUICoreModule],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: FavouritesService, useValue: favouritesService },
            ],
            declarations: [TestComponent, FavouriteToggle, IsFavouritePipe],
        });
        state = TestBed.get(ApplicationStateService);
    });

    function getButtons(fixture: ComponentFixture<any>): { star: HTMLElement, unstar: HTMLElement } {
        let iconButtons: HTMLElement[] =  Array.from(fixture.nativeElement.querySelectorAll('gtx-button .material-icons'));
        const buttonWithText = (text: string) => iconButtons.filter(btn => btn.innerText === text)[0];

        return {
            star: buttonWithText('star_border'),
            unstar: buttonWithText('star'),
        };
    }

    it('shows a filled star if an item is a favourite',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.item = { id: 1, globalId: 'item1', type: 'page' };
            state.mockState({
                favourites: {
                    list: [
                        { id: 1, globalId: 'item1', nodeId: 2, type: 'page', name: 'Page 1' },
                    ],
                },
                folder: { activeNode: 2 },
            });
            fixture.detectChanges();

            let {star, unstar} = getButtons(fixture);

            expect(star).not.toBeDefined();
            expect(unstar).toBeDefined();
        }),
    );

    it('shows an empty star when an item can be added as a favourite',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.item = { id: 1, globalId: 'item1', type: 'page' };
            state.mockState({
                favourites: { list: [] },
                folder: { activeNode: 2 },
            });
            fixture.detectChanges();

            let {star, unstar} = getButtons(fixture);
            expect(star).toBeDefined();
            expect(unstar).not.toBeDefined();
        }),
    );

    it('changes its icon when the bound item is added as a favourite',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.item = { id: 1, globalId: 'item1', type: 'page' };
            state.mockState({
                favourites: { list: [] },
                folder: { activeNode: 2 },
            });
            fixture.detectChanges();

            let buttons = getButtons(fixture);
            expect(buttons.star).toBeDefined('add icon not visible with empty list');
            expect(buttons.unstar).toBeUndefined('remove icon is visible with empty list');

            state.mockState({
                favourites: {
                    list: [
                        { id: 1, globalId: 'item1', type: 'page', nodeId: 2, name: 'Page 1' },
                    ],
                },
            });
            fixture.detectChanges();

            buttons = getButtons(fixture);
            expect(buttons.star).toBeUndefined('add icon is visible when item is favourite');
            expect(buttons.unstar).toBeDefined('remove icon not visible when item is favourite');
        }),
    );

    it('calls FavouriteService.add when favourite star is clicked',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.item = { id: 1, globalId: 'item1', type: 'page' };
            state.mockState({
                favourites: { list: [] },
                folder: { activeNode: 2 },
            });
            fixture.detectChanges();

            let {star} = getButtons(fixture);
            expect(star).toBeDefined();
            triggerClickEventOn(star);

            expect(favouritesService.add).toHaveBeenCalledTimes(1);
            expect(favouritesService.add).toHaveBeenCalledWith([instance.item]);
        }),
    );

    it('calls FavouriteService.remove when unfavourite star is clicked',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.item = { id: 1, globalId: 'item1', type: 'page' };
            state.mockState({
                favourites: {
                    list: [
                        { id: 1, globalId: 'item1', nodeId: 2, type: 'page', name: 'Page 1' },
                    ],
                },
                folder: { activeNode: 2 },
            });
            fixture.detectChanges();

            let {unstar} = getButtons(fixture);
            expect(unstar).toBeDefined('remove button not shown');
            triggerClickEventOn(unstar);

            expect(favouritesService.remove).toHaveBeenCalledTimes(1);
            expect(favouritesService.remove).toHaveBeenCalledWith([instance.item], { nodeId: 2 });
        }),
    );

});

function triggerClickEventOn(element: Element): void {
    let clickEvent: Event;
    try {
        clickEvent = new Event('click', { bubbles: true });
    } catch (ie11) {
        clickEvent = document.createEvent('Event');
        clickEvent.initEvent('click', true, false);
    }
    element.dispatchEvent(clickEvent);
}

@Component({
    template: `<favourite-toggle [item]="item"></favourite-toggle>`
    })
class TestComponent {
    item: any = {};
}

class MockFavouritesService {
    constructor() {
        spyOn(this as any, 'add');
        spyOn(this as any, 'remove');
    }

    add(items: any[], node?: { nodeId: number }): void { }
    remove(items: any[], node?: { nodeId: number }): void { }
}
