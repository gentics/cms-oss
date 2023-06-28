import { Injectable } from '@angular/core';
import { Normalized, Page } from '@gentics/cms-models';
import { StateContext } from '@ngxs/store';
import { patch } from '@ngxs/store/operators';
import { normalize, schema } from 'normalizr';
import { PublishQueueState, pageSchema, userSchema } from '../../../common/models';
import { emptyItemInfo, plural } from '../../../common/models/folder-state';
import { ActionDefinition, AppStateBranch } from '../../state-utils';
import { AddEntitiesAction, UpdateEntitiesAction } from '../entity/entity.actions';
import {
    AssigningUsersToPagesErrorAction,
    AssigningUsersToPagesSuccessAction,
    PUBLISH_QUEUE_STATE_KEY,
    PublishQueueFetchingErrorAction,
    PublishQueuePagesFetchingSuccessAction,
    PublishQueueUsersFetchingSuccessAction,
    SetPublishQueueListDisplayFieldsAction,
    StartAssigningUsersToPagesAction,
    StartPublishQueueFetchingAction,
} from './publish-queue.actions';

const INITIAL_PUBLISH_QUEUE_STATE: PublishQueueState = {
    pages: emptyItemInfo,
    users: [],
    fetching: false,
    assigning: false,
};

@AppStateBranch<PublishQueueState>({
    name: PUBLISH_QUEUE_STATE_KEY,
    defaults: INITIAL_PUBLISH_QUEUE_STATE,
})
@Injectable()
export class PublishQueueStateModule {

    @ActionDefinition(StartAssigningUsersToPagesAction)
    handleStartAssigningUsersToPagesAction(ctx: StateContext<PublishQueueState>, action: StartAssigningUsersToPagesAction): void {
        ctx.patchState({
            assigning: true,
        });
    }

    @ActionDefinition(AssigningUsersToPagesSuccessAction)
    async handleAssigningUsersToPagesSuccessAction(ctx: StateContext<PublishQueueState>, action: AssigningUsersToPagesSuccessAction): Promise<void> {
        const pageUpdates: { [id: number]: Partial<Page<Normalized>> } = {};
        for (let id of action.pageIds) {
            pageUpdates[id] = {
                modified: true,
            };
        }
        await ctx.dispatch(new UpdateEntitiesAction({
            page: pageUpdates,
        })).toPromise();
        ctx.patchState({
            assigning: false,
        });
    }

    @ActionDefinition(AssigningUsersToPagesErrorAction)
    handleAssigningUsersToPagesErrorAction(ctx: StateContext<PublishQueueState>, action: AssigningUsersToPagesErrorAction): void {
        ctx.patchState({
            assigning: false,
        });
    }

    @ActionDefinition(StartPublishQueueFetchingAction)
    handleStartPublishQueueFetchingAction(ctx: StateContext<PublishQueueState>, action: StartPublishQueueFetchingAction): void {
        ctx.patchState({
            fetching: true,
        });
    }

    @ActionDefinition(PublishQueuePagesFetchingSuccessAction)
    async handlePublishQueueFetchingSuccessAction(
        ctx: StateContext<PublishQueueState>,
        action: PublishQueuePagesFetchingSuccessAction,
    ): Promise<void> {
        const normalized = normalize(action.pages, new schema.Array(pageSchema));

        await ctx.dispatch(new AddEntitiesAction(normalized)).toPromise();

        ctx.setState(patch<PublishQueueState>({
            fetching: false,
            pages: patch({
                list: normalized.result,
            }),
        }));
    }

    @ActionDefinition(PublishQueueFetchingErrorAction)
    handlePublishQueueFetchingErrorAction(ctx: StateContext<PublishQueueState>, action: PublishQueueFetchingErrorAction): void {
        ctx.patchState({
            fetching: false,
        });
    }

    @ActionDefinition(PublishQueueUsersFetchingSuccessAction)
    async handlePublishQueueUsersFetchingSuccessAction(
        ctx: StateContext<PublishQueueState>,
        action: PublishQueueUsersFetchingSuccessAction,
    ): Promise<void> {
        const normalized = normalize(action.users, new schema.Array(userSchema));

        await ctx.dispatch(new AddEntitiesAction(normalized)).toPromise();

        ctx.setState(patch<PublishQueueState>({
            fetching: false,
            users: normalized.result,
        }));
    }

    @ActionDefinition(SetPublishQueueListDisplayFieldsAction)
    handleSetPublishQueueListDisplayFieldsAction(ctx: StateContext<PublishQueueState>, action: SetPublishQueueListDisplayFieldsAction): void {
        const type = plural[action.type] || action.type;

        ctx.setState(patch<PublishQueueState>({
            [type]: patch({
                displayFields: action.displayFields,
            }),
        }));
    }
}
