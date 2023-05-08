
const REDUX_DEVTOOLS_PROD_ENABLED_KEY = 'GCMS_ADMIN_UI-state-redux-devtools-prod';
const ENABLED_VALUE = 'true';

/**
 * Checks if the AppState Redux Devtools plugin should be enabled also in a PROD build.
 */
export function checkStateReduxDevtoolsEnabledForProd(): boolean {
    const settingValue = localStorage.getItem(REDUX_DEVTOOLS_PROD_ENABLED_KEY);
    return settingValue === ENABLED_VALUE;
}

export function enableStateReduxDevtoolsForProd(): void {
    localStorage.setItem(REDUX_DEVTOOLS_PROD_ENABLED_KEY, ENABLED_VALUE);
}

export function disableStateReduxDevtoolsForProd(): void {
    localStorage.removeItem(REDUX_DEVTOOLS_PROD_ENABLED_KEY);
}

