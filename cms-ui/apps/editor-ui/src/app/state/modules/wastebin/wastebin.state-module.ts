import { Injectable } from '@angular/core';
import { StateContext } from '@ngxs/store';
import { patch } from '@ngxs/store/operators';
import { normalize, schema } from 'normalizr';
import { WastebinState, WastebinTypeList } from '../../../common/models';
import { ActionDefinition, AppStateBranch, concatUnique, getNormalizrSchema } from '../../state-utils';
import { AddEntitiesAction } from '../entity/entity.actions';
import {
    RestoreWasteBinItemsAction,
    SetWasteBinSortingAction,
    StartWasteBinItemsDeletionAction,
    StartWasteBinItemsFetchingAction,
    WASTE_BIN_STATE_KEY,
    WasteBinItemsDeletionErrorAction,
    WasteBinItemsDeletionSuccessAction,
    WasteBinItemsFetchingErrorAction,
    WasteBinItemsFetchingSuccessAction,
} from './wastebin.actions';

export type WastebinItemType = 'folder' | 'page' | 'file' | 'form' | 'image';

const INITIAL_WASTE_BIN_STATE: WastebinState = {
    folder: {
        list: [],
        requesting: false,
    },
    form: {
        list: [],
        requesting: false,
    },
    page: {
        list: [],
        requesting: false,
    },
    file: {
        list: [],
        requesting: false,
    },
    image: {
        list: [],
        requesting: false,
    },
    sortBy: 'name',
    sortOrder: 'asc',
    lastError: undefined,
};

@AppStateBranch<WastebinState>({
    name: WASTE_BIN_STATE_KEY,
    defaults: INITIAL_WASTE_BIN_STATE,
})
@Injectable()
export class WastebinStateModule {

    @ActionDefinition(StartWasteBinItemsFetchingAction)
    handleStartWasteBinItemsFetchingAction(ctx: StateContext<WastebinState>, action: StartWasteBinItemsFetchingAction): void {
        ctx.setState(patch<WastebinState>({
            [action.itemType]: patch<WastebinTypeList>({
                requesting: true,
            }),
        }));
    }

    @ActionDefinition(WasteBinItemsFetchingSuccessAction)
    async handleWasteBinItemsFetchingSuccessAction(ctx: StateContext<WastebinState>, action: WasteBinItemsFetchingSuccessAction): Promise<void> {
        const normalized = normalize(action.items, new schema.Array(getNormalizrSchema(action.itemType)));
        await ctx.dispatch(new AddEntitiesAction(normalized)).toPromise();

        ctx.setState(patch<WastebinState>({
            [action.itemType]: patch<WastebinTypeList>({
                requesting: false,
                list: action.items.map(item => item.id),
            }),
        }));
    }

    @ActionDefinition(WasteBinItemsFetchingErrorAction)
    handleWasteBinItemsFetchingErrorAction(ctx: StateContext<WastebinState>, action: WasteBinItemsFetchingErrorAction): void {
        ctx.setState(patch<WastebinState>({
            [action.itemType]: patch<WastebinTypeList>({
                requesting: false,
            }),
            lastError: action.errorMessage,
        }));
    }

    @ActionDefinition(StartWasteBinItemsDeletionAction)
    handleStartWasteBinItemsDeletionAction(ctx: StateContext<WastebinState>, action: StartWasteBinItemsDeletionAction): void {
        const state = ctx.getState();

        ctx.setState(patch<WastebinState>({
            [action.itemType]: patch<WastebinTypeList>({
                requesting: true,
                list: (state[action.itemType]?.list || []).filter(id => !action.ids.includes(id)),
            }),
        }));
    }

    @ActionDefinition(WasteBinItemsDeletionSuccessAction)
    handleWasteBinItemsDeletionSuccessAction(ctx: StateContext<WastebinState>, action: WasteBinItemsDeletionSuccessAction): void {
        const state = ctx.getState();

        ctx.setState(patch<WastebinState>({
            [action.itemType]: patch<WastebinTypeList>({
                requesting: false,
                list: (state[action.itemType]?.list || []).filter(id => !action.ids.includes(id)),
            }),
        }));
    }

    @ActionDefinition(WasteBinItemsDeletionErrorAction)
    handleWasteBinItemsDeletionErrorAction(ctx: StateContext<WastebinState>, action: WasteBinItemsDeletionErrorAction): void {
        const state = ctx.getState();

        ctx.setState(patch<WastebinState>({
            [action.itemType]: patch<WastebinTypeList>({
                requesting: false,
                list: concatUnique(state[action.itemType]?.list || [], action.ids),
            }),
            lastError: action.errorMessage,
        }));
    }

    @ActionDefinition(RestoreWasteBinItemsAction)
    handleRestoreWasteBinItemsAction(ctx: StateContext<WastebinState>, action: WasteBinItemsDeletionSuccessAction): void {
        const state = ctx.getState();

        ctx.setState(patch<WastebinState>({
            [action.itemType]: patch<WastebinTypeList>({
                list: (state[action.itemType]?.list || []).filter(id => !action.ids.includes(id)),
            }),
        }));
    }

    @ActionDefinition(SetWasteBinSortingAction)
    handleSetWasteBinSortingAction(ctx: StateContext<WastebinState>, action: SetWasteBinSortingAction): void {
        ctx.patchState({
            sortBy: action.sortBy,
            sortOrder: action.sortOrder,
        });
    }
}
