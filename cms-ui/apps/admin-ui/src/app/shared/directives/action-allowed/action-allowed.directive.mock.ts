import { InterfaceOf, PermissionsCheckResult } from '@admin-ui/common';
import { Directive } from '@angular/core';
import { Observable } from 'rxjs';
import { ActionAllowedDirective, GTX_ACTION_ALLOWED_SELECTOR } from './action-allowed.directive';

/**
 * Mocked version of the ActionAllowedDirective for use in unit tests.
 */
@Directive({
    selector: GTX_ACTION_ALLOWED_SELECTOR,
})
export class MockActionAllowedDirective implements Omit<InterfaceOf<ActionAllowedDirective>, 'ngOnDestroy' | 'ngOnInit'> {

    aaHideElement: boolean;
    aaInstanceId: number;
    actionId: string;
    aaNodeId: number;
    disabled: boolean;
    aaOverrideCheck: PermissionsCheckResult | Observable<PermissionsCheckResult>;

}
