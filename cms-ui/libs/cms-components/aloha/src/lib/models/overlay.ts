import { InjectionToken } from '@angular/core';
import { DynamicDialogConfiguration, DynamicDropdownConfiguration, DynamicFormModalConfiguration, OverlayElementControl } from '@gentics/aloha-models';

/**
 * Token for the aloha-overlay service.
 * Roundabout way as otherwise it'd cause cyclic imports.
 * Only for internal use.
 */
export const ALOHA_OVERLAY_TOKEN = new InjectionToken<AlohaOverlayHandler>('gtx-aloha-overlay-token');

/**
 * Interface of the aloha-overlay service.
 * Roundabout way as otherwise it'd cause cyclic imports.
 * Only for internal use.
 */
export interface AlohaOverlayHandler {

    closeRemaining(): void;

    openDialog<T>(config: DynamicDialogConfiguration<T>): Promise<OverlayElementControl<T>>;

    openDynamicDropdown<T>(configuration: DynamicDropdownConfiguration<T>, slot?: string): Promise<OverlayElementControl<T>>;

    openDynamicModal<T>(configuration: DynamicFormModalConfiguration<T>): Promise<OverlayElementControl<T>>;
}
