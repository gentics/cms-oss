import { Injectable, OnDestroy } from '@angular/core';
import { ObservableStopper } from '../../../common';
import { ServiceBase } from '../service-base/service.base';

/**
 * Common superclass for services that require initialization.
 *
 * It provides an `ObservableStopper` (the `stopper` property)
 * and hooks for initialization (`onServiceInit()`) and destruction (`onServiceDestroy()`)
 * to deriving classes.
 *
 * If you do not require initialization or the stopper, use `ServiceBase` instead.
 */
// TODO: Add Angular decorator.
@Injectable()
export abstract class InitializableServiceBase extends ServiceBase implements OnDestroy {

    protected initialized = false;

    // We provide the stopper here instead of in ServiceBase, because generally a service that
    // requires a stopper, does so because it initializes something.
    protected stopper = new ObservableStopper();

    /**
     * Initializes the service. This method must be called exactly once.
     */
    init(): void {
        if (this.initialized) {
            throw new Error(`This service was already initialized.`);
        }
        this.initialized =  true;

        this.onServiceInit();
    }

    ngOnDestroy(): void {
        // We override ngOnDetroy() here instead of onServiceDestroy(), such that deriving classes
        // do not need to make a super.onServiceDestroy() call.

        super.ngOnDestroy();
        this.stopper.stop();
    }

    /**
     * This method is called when the service is initialized and must be implemented
     * by a deriving class.
     *
     * Do not override `init()`, but provide an implementation of this method instead.
     */
    protected abstract onServiceInit(): void;

}
