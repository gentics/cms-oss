import { Component, NO_ERRORS_SCHEMA, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import * as Sortable from 'sortablejs';
import { ISortableEvent } from '../../common';
import { componentTest } from '../../testing';
import { SortableListComponent, sortFactory } from './sortable-list.component';

describe('SortableListComponent', () => {

    beforeEach(() => TestBed.configureTestingModule({
        declarations: [SortableListComponent, TestComponent],
        teardown: { destroyAfterEach: false },
        schemas: [NO_ERRORS_SCHEMA],
    }));

    describe('sort() method', () => {

        /**
         * Returns the sort() function of the SortableList class, configured with the indexes supplied.
         */
        function getSortFn(oldIndex: number = 0, newIndex: number = 2): Function {
            const event: ISortableEvent = <ISortableEvent> {
                oldIndex,
                newIndex,
            };
            return sortFactory(event);
        }

        it('is a function',
            componentTest(() => TestComponent, fixture => {
                const sortFn: any = getSortFn();
                expect(typeof sortFn).toBe('function');
            }),
        );

        it('returns a new array by default',
            componentTest(() => TestComponent, fixture => {
                const initial = [1, 2, 3];
                const sortFn = getSortFn();
                const sorted = sortFn(initial);
                expect(initial).not.toBe(sorted);
            }),
        );

        it('returns the same array when byReference = true',
            componentTest(() => TestComponent, fixture => {
                const initial = [1, 2, 3];
                const sortFn = getSortFn();
                const sorted = sortFn(initial, true);
                expect(initial).toBe(sorted);
            }),
        );

        it('sorts a simple small array',
            componentTest(() => TestComponent, fixture => {
                const initial = [1, 2, 3];
                const expected = [2, 3, 1];
                const sortFn = getSortFn();

                expect(sortFn(initial)).toEqual(expected);
            }),
        );

        it('sorts a simple large array',
            componentTest(() => TestComponent, fixture => {
                const initial = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
                const expected = [1, 2, 3, 5, 6, 7, 4, 8, 9, 10];
                const sortFn = getSortFn(3, 6);

                expect(sortFn(initial)).toEqual(expected);
            }),
        );

        it('sorts an array of objects',
            componentTest(() => TestComponent, fixture => {
                const initial = [{ name: 'john' }, { name: 'joe' }, { name: 'mary' }];
                const expected = [{ name: 'joe' }, { name: 'mary' }, { name: 'john' }];
                const sortFn = getSortFn();

                expect(sortFn(initial)).toEqual(expected);
            }),
        );

        it('outputs the input array when undefined index values are passed',
            componentTest(() => TestComponent, fixture => {
                const initial = [1, 2, 3];
                const instance: SortableListComponent = fixture.componentInstance.listInstance;
                const sortFn = sortFactory(<ISortableEvent> {});

                expect(sortFn(initial)).toEqual(initial);
            }),
        );

        it('outputs the input array for out-of-bound oldIndex',
            componentTest(() => TestComponent, fixture => {
                const initial = [1, 2, 3];
                const sortFn = getSortFn(3, 1);

                expect(sortFn(initial)).toEqual(initial);
            }),
        );

        it('outputs the input array for out-of-bound newIndex',
            componentTest(() => TestComponent, fixture => {
                const initial = [1, 2, 3];
                const sortFn = getSortFn(0, 3);

                expect(sortFn(initial)).toEqual(initial);
            }),
        );

    });

    describe('disabled attribute:', () => {

        it('is forwarded to the options of its sortable instance',
            componentTest(() => TestComponent, (fixture, instance) => {
                fixture.detectChanges();
                const sortable: Sortable = (instance.listInstance as any).sortable;

                expect(sortable.option('disabled')).toBe(false);

                fixture.componentInstance.disabled = true;
                fixture.detectChanges();

                expect(sortable.option('disabled')).toBe(true);
            }),
        );

    });

});


@Component({
    template: '<gtx-sortable-list [disabled]="disabled"></gtx-sortable-list>',
})
class TestComponent {
    disabled = false;

    @ViewChild(SortableListComponent, { static: true })
    listInstance: SortableListComponent;
}
