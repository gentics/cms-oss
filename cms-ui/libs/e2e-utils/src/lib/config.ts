import type { Variant } from '@gentics/cms-models';

export const ENV_CI = 'CI';
export const ENV_E2E_CMS_VARIANT = 'E2E_CMS_VARIANT';
export const ENV_E2E_LOCAL_PLAYWRIGHT = 'E2E_LOCAL_PLAYWRIGHT';
export const ENV_E2E_LOCAL_APP = 'E2E_LOCAL_APP';
export const ENV_SKIP_E2E_LOCAL_APP_LAUNCH = 'SKIP_E2E_LOCAL_APP_LAUNCH';
export const ENV_E2E_FORCE_REPEATS = 'E2E_FORCE_REPEATS';

export const ENV_E2E_KEYCLOAK_URL = 'E2E_KEYCLOAK_URL';
export const ENV_E2E_CMS_URL = 'E2E_CMS_URL';
export const ENV_E2E_APP_PATH = 'E2E_APP_PATH';
export const ENV_E2E_APP_URL = 'E2E_APP_URL';

export const ENV_E2E_CMS_IMPORTER_USERNAME = 'E2E_CMS_IMPORTER_USERNAME';
export const ENV_E2E_CMS_IMPORTER_PASSWORD = 'E2E_CMS_IMPORTER_PASSWORD';

declare global {
    // eslint-disable-next-line @typescript-eslint/no-namespace
    namespace NodeJS {
        interface ProcessEnv {
            /** Flag which determines if we're running in a CI context. */
            [ENV_CI]: string;

            /** The CMS Variant that is being tested. */
            [ENV_E2E_CMS_VARIANT]: Variant;
            /** If it should use the local playwright server instead of the container. */
            [ENV_E2E_LOCAL_PLAYWRIGHT]?: string;
            /** If it should use the local application instead of the application in the container. */
            [ENV_E2E_LOCAL_APP]?: string;
            /** If it should not automatically launch the local application. */
            [ENV_SKIP_E2E_LOCAL_APP_LAUNCH]?: string;
            /** If it should force repeats of intergration tests. */
            [ENV_E2E_FORCE_REPEATS]?: string;

            /** Override for the URL where keycloak is reachable. */
            [ENV_E2E_KEYCLOAK_URL]?: string;
            /** The URL where the CMS is reachable. */
            [ENV_E2E_CMS_URL]?: string;
            /** The path where the app is reachable, if it is hosted on the CMS. */
            [ENV_E2E_APP_PATH]?: string;
            /** Full URL for the current test application. */
            [ENV_E2E_APP_URL]?: string;

            /** Username override for setup rest calls */
            [ENV_E2E_CMS_IMPORTER_USERNAME]?: string;
            /** Password override for setup rest calls */
            [ENV_E2E_CMS_IMPORTER_PASSWORD]?: string;
        }
    }
}

/**
 * Simple helper function to convert/"parse" a environment value as bool.
 * Checks for `1`, `"1"`, `true`, and `"true"`.
 * @param value The value of the environment value
 * @returns A properly checked/converted boolean from the value.
 */
export function isEnvBool(value: string | number | boolean): boolean {
    return value === 1 || value === '1' || value === true || value === 'true';
}

export function isCIEnvironment(): boolean {
    return isEnvBool(process.env[ENV_CI]);
}
