export interface MaintenanceModeState {
    active: boolean;
    fetching: boolean;
    /** Maintenance mode endpoint only exists in ContentNode >= 5.27.7 */
    reportedByServer: boolean | undefined;
    showBanner: boolean;
    message: string;
}
