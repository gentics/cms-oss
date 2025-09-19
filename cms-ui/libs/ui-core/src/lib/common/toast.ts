import { ColorThemes } from './colors';

export interface INotificationOptions {
    /**
     * ID for this notification.
     * Mainly used for e2e tests.
     */
    /*
     * TODO: Turn into feature to only allow one notif/toast with the same ID?
     * Would close the old notif to only display one?
     */
    id?: string;
    message: string;
    type?: ColorThemes | 'default';
    /**
     * The notification will automatically be dismissed after this delay.
     * To turn off auto-dismissal, set this to 0.
     */
    delay?: number;
    dismissOnClick?: boolean;
    action?: {
        label: string;
        onClick?: () => void;
    };
}

export const DEFAULT_TOAST_OPTIONS: INotificationOptions = {
    message: '',
    type: 'default',
    delay: 3000,
    dismissOnClick: true,
};

export interface OpenedNotification {
    dismiss: () => void;
}
