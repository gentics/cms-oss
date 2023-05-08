import { ChangeDetectorRef, OnDestroy, Pipe, PipeTransform } from '@angular/core';
import { Starrable } from '@gentics/cms-models';
import { isEqual } from 'lodash';
import { Subscription } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { ApplicationStateService } from '../../../state';

/**
 * A pipe that resolves to `true` if the input value is marked as a favourite.
 *
 * Usage:
 *   <!-- with an object -->
 *   <div *ngIf="item | isFavourite"></div>
 *
 *   <!-- using an Observable -->
 *   <div *ngIf="items$ | async | isFavourite"></div>
 *
 *   <!-- with an array -->
 *   <div *ngIf="items | isFavourite:'all'"></div>
 *   <div *ngIf="items | isFavourite:'any'"></div>
 */
@Pipe({ name: 'isFavourite', pure: false })
export class IsFavouritePipe implements PipeTransform, OnDestroy {

    private starred: Starrable[];
    private lastInput: Starrable | Starrable[];
    private lastReturnValue: boolean = undefined;
    private subscriptions: Subscription[] = [];
    private activeNode = 0;

    constructor(
        appState: ApplicationStateService,
        changeDetector: ChangeDetectorRef,
    ) {

        this.subscriptions = [
            appState.select(state => state.favourites).pipe(
                map(favs => favs.list),
                distinctUntilChanged(isEqual),
            ).subscribe(favouriteList => {
                // Re-evaluate when the favourites change
                this.starred = favouriteList;
                this.lastInput = undefined;
                changeDetector.markForCheck();
            }),

            appState
                .select(state => state.folder.activeNode)
                .subscribe(node => {
                    this.activeNode = node;
                    changeDetector.markForCheck();
                }),
        ];
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
        this.subscriptions = [];
        // Unset elements to allow garbage collection
        this.lastInput = undefined;
    }

    transform(input: Starrable | Starrable[], combination?: 'all' | 'any'): boolean {
        if (input == undefined) { return false; }

        if (input === this.lastInput) {
            return this.lastReturnValue;
        }

        if (this.starred == undefined) {
            // State subscribed, but not received yet
            return false;
        }

        let result: boolean;
        if (Array.isArray(input)) {
            if (combination === 'any') {
                result = input.some(entity =>
                    this.starred.some(fav => equal(fav, entity, this.activeNode)),
                );
            } else {
                result = input.every(entity =>
                    this.starred.some(fav => equal(fav, entity, this.activeNode)),
                );
            }
        } else {
            result = this.starred.some(fav => equal(fav, input, this.activeNode));
        }
        this.lastInput = input;
        this.lastReturnValue = result;
        return result;
    }
}

function equal(a: any, b: any, activeNode: number): boolean {
    if (!a.nodeId && !b.nodeId) {
        return false;
    } else if (a.nodeId && !b.nodeId && a.nodeId !== activeNode) {
        return false;
    } else if (!a.nodeId && b.nodeId && b.nodeId !== activeNode) {
        return false;
    } else if (a.globalId && b.globalId) {
        return a.globalId === b.globalId;
    } else {
        return a.id === b.id && a.type === b.type;
    }
}
