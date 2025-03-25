import { Page } from '@playwright/test';
import { DEFAULT_KEYCLOAK_URL, ENV_KEYCLOAK_URL } from './common';
import { matchesPath } from './utils';

export function blockKeycloakConfig(page: Page): Promise<void> {
    return page.route('/ui-conf/keycloak.json', route => {
        return route.abort('failed');
    });
}

export function waitForKeycloakAuthPage(page: Page): Promise<void> {
    const kcUrl = process.env[ENV_KEYCLOAK_URL] || DEFAULT_KEYCLOAK_URL;
    const parsedUrl = new URL(kcUrl);

    return page.waitForURL(url =>
        url.host === parsedUrl.host
        && matchesPath(url, '/realms/*/protocol/openid-connect/auth'),
    );
}
