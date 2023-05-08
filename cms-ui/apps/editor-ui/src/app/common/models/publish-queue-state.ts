import { ItemsInfo } from './folder-state';

export interface PublishQueueState {
    pages: ItemsInfo;
    users: number[];
    fetching: boolean;
    assigning: boolean;
}
