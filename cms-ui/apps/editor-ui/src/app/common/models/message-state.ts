export interface MessageState {
    all: number[];
    read: number[];
    unread: number[];
    fetching: boolean;
    lastError?: string;
}
