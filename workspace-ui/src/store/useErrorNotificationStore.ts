import { create } from 'zustand';

/** One error shown as a notification. */
export interface ErrorNotification {
    id: string;
    /** i18n key of the message, translated when shown. */
    messageKey: string;
    /** Technical detail shown below the message, for example an error's message. Not translated. */
    detail?: string;
    /** i18n key of the detail, translated when shown; takes the place of `detail`. */
    detailKey?: string;
}

interface ErrorNotificationState {
    errors: ErrorNotification[];
    addError: (error: Omit<ErrorNotification, 'id'>) => void;
    dismissError: (id: string) => void;
}

export const useErrorNotificationStore = create<ErrorNotificationState>((set) => ({
    errors: [],

    addError: (error) => set((state) => ({
        errors: [...state.errors, { id: crypto.randomUUID(), ...error }],
    })),

    dismissError: (id) => set((state) => ({
        errors: state.errors.filter((error) => error.id !== id),
    })),
}));
