import { InjectionToken } from '@angular/core';
import { ItemInNode, RepositoryBrowserOptions } from '@gentics/cms-models';

export const GCMS_UI_SERVICES_PROVIDER = new InjectionToken<GcmsUiServices>('GCMS_UI_SERVICES_PROVIDER');

export interface GcmsUiServices {

    /** Method for opening the Repository Browser. */
    openRepositoryBrowser<R = ItemInNode>(options: RepositoryBrowserOptions): Promise<R | R[]>;

    /** Create SelectedItemsHelper instance */
    createSelectedItemsHelper(itemType: 'page' | 'folder' | 'file' | 'image' | 'form', defaultNodeId?: number);

}