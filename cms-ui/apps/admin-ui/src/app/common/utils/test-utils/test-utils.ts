import { ComponentFixture, tick } from '@angular/core/testing';

/**
 * Here shall be all tools to be reused in unit testing context.
 */

/**
 * More precise method to make fake time pass than original `tick()`.
 */
export function ticktack<T>(fixture: ComponentFixture<T>, amount: number, miliseconds?: number): void {
    for (let i = amount; i > 0; i--) {
        fixture.detectChanges();
        miliseconds ? tick(miliseconds) : tick();
    }
}
