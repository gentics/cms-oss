import {IndexByKey} from '@gentics/cms-models';

/**
 * Describes the breadcrumb for a route part.
 */
export interface BreadcrumbInfo {

    /**
     * The title that should be displayed for this breadcrumb.
     * If `doNotTranslate` is false or not set, this string is passed
     * to the `I18nService` before being displayed.
     */
    title: string;

    /**
     * Optional parameters that will be passed to the I18nService
     * when translating `title`.
     */
    titleParams?: IndexByKey<any>;

    /**
     * Optional tooltip to show for this breadcrumb.
     * This follows the same translation semantics as `title`.
     */
    tooltip?: string;

    /**
     * Optional parameters that will be passed to the I18nService
     * when translating `tooltip`.
     */
    tooltipParams?: IndexByKey<any>;

    /**
     * If this is true, the `title` and `tooltip` strings are not translated
     * using the `I18nService` before being displayed.
     */
    doNotTranslate?: boolean;

}
