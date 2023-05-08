import { Injectable, OnDestroy } from '@angular/core';

/**
 * Common superclass for services.
 *
 * It provides custom destruction hook (`onServiceDestroy()`) to deriving classes
 * and may be extended with more services, required by most services, in the future.
 *
 * If you need an initialization hook and an `ObservableStopper`, use `InitializableServiceBase` instead.
 */
// TODO: Add Angular decorator.
@Injectable()
export abstract class ServiceBase implements OnDestroy {

    ngOnDestroy(): void {
        this.onServiceDestroy();
    }

    /**
     * This method is called in the `ngOnDestroy` hook and may be overridden by a
     * deriving class. It is called before the `ObservableStopper` is stopped.
     *
     * Do not override `ngOnDestroy()`.
     */
    protected onServiceDestroy(): void { }

}
