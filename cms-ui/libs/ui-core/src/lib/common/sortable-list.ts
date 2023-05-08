import type * as Sortable from 'sortablejs';

export type SortFunction<T> = (source: T[], byReference?: boolean) => T[];

/**
 * An augmented version of the event object returned by each of the Sortablejs callbacks, which can then be emitted up
 * to the consuming component.
 */
export interface ISortableEvent extends Sortable.SortableEvent {
    sort: SortFunction<any>;
}

export interface ISortableMoveEvent extends Sortable.MoveEvent {
    sort: SortFunction<any>;
}

export interface SortableInstance {
    el: HTMLElement;
    nativeDraggable: boolean;
    options: any;
}

export type PutPullType = true | false | 'clone';
export type PutPullFn = (to: SortableInstance, from: SortableInstance) => PutPullType;

export type ISortableGroupOptions = Sortable.GroupOptions;

export type SortableGroup = string | ISortableGroupOptions;
