export interface MessageState {
    all: number[];
    read: number[];
    unread: number[];
    deliveredInstantMessages: number[];
    fetching: boolean;
    lastError?: string;
}
