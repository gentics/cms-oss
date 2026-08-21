import { createConfiguration } from '../../playwright.config';

/**
 * E2E configuration for the `form-translations` custom tool.
 *
 * The third argument is the base URL path the CMS serves this tool under
 * (matches `baseHref` in `project.json`'s production configuration and the
 * `toolUrl` in `cms-registration-config.json`).
 */
export default createConfiguration(__filename, 'form-translations', '/tools/form-translations/');
