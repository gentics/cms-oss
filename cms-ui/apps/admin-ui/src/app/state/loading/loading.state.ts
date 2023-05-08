import { Injectable } from '@angular/core';
import { StateContext } from '@ngxs/store';
import { patch } from '@ngxs/store/operators';
import { ActionDefinition, AppStateBranch, defineInitialState } from '../utils/state-utils';
import {
    DecrementDetailLoading,
    DecrementListLoading,
    DecrementMasterLoading,
    IncrementDetailLoading,
    IncrementListLoading,
    IncrementMasterLoading,
    ResetDetailLoading,
    ResetMasterLoading,
} from './loading.actions';

export interface LoadingStateModel {
    detailLoading: number;
    masterLoading: number;
    detailLoadingMessage: string;
    masterLoadingMessage: string;
    listLoading: {
        [listId: string]: number;
    };
}

export const INITIAL_LOADING_STATE = defineInitialState<LoadingStateModel>({
    detailLoading: 0,
    masterLoading: 0,
    detailLoadingMessage: null,
    masterLoadingMessage: null,
    listLoading: {},
});

@AppStateBranch<LoadingStateModel>({
    name: 'loading',
    defaults: INITIAL_LOADING_STATE,
})
@Injectable()
export class LoadingStateModule {

    @ActionDefinition(IncrementMasterLoading)
    incrementMasterLoading(ctx: StateContext<LoadingStateModel>, action: IncrementMasterLoading): void {
        const master = ctx.getState().masterLoading;
        ctx.setState(
            patch({
                masterLoading: Math.max(0, master + 1),
                masterLoadingMessage: action.message ? action.message : null,
            }),
        );
    }

    @ActionDefinition(DecrementMasterLoading)
    decrementMasterLoading(ctx: StateContext<LoadingStateModel>): void {
        const master = ctx.getState().masterLoading;
        ctx.setState(
            patch({
                masterLoading: Math.max(0, master - 1),
                masterLoadingMessage: null,
            }),
        );
    }

    @ActionDefinition(IncrementDetailLoading)
    incrementDetailLoading(ctx: StateContext<LoadingStateModel>, action: IncrementDetailLoading): void {
        const detail = ctx.getState().detailLoading;
        ctx.setState(
            patch({
                detailLoading: Math.max(0, detail + 1),
                detailLoadingMessage: action.message ? action.message : null,
            }),
        );
    }

    @ActionDefinition(DecrementDetailLoading)
    decrementDetailLoading(ctx: StateContext<LoadingStateModel>): void {
        const detail = ctx.getState().detailLoading;
        ctx.setState(
            patch({
                detailLoading: Math.max(0, detail - 1),
                detailLoadingMessage: null,
            }),
        );
    }

    @ActionDefinition(ResetMasterLoading)
    resetMasterLoading(ctx: StateContext<LoadingStateModel>): void {
        const detail = ctx.getState().detailLoading;
        ctx.setState(
            patch({
                masterLoading: 0,
                masterLoadingMessage: null,
            }),
        );
    }

    @ActionDefinition(ResetDetailLoading)
    resetDetailLoading(ctx: StateContext<LoadingStateModel>): void {
        const detail = ctx.getState().detailLoading;
        ctx.setState(
            patch({
                detailLoading: 0,
                detailLoadingMessage: null,
            }),
        );
    }

    @ActionDefinition(IncrementListLoading)
    incrementListLoading(ctx: StateContext<LoadingStateModel>, action: IncrementListLoading): void {
        const listStatus = ctx.getState().listLoading || {};
        const listCount = listStatus[action.listId] ?? 0;

        ctx.setState(
            patch({
                listLoading: {
                    ...listStatus,
                    // Prevent underflows/negative values
                    [action.listId]: Math.max(0, listCount + 1),
                },
            }),
        );
    }

    @ActionDefinition(DecrementListLoading)
    decrementListLoading(ctx: StateContext<LoadingStateModel>, action: DecrementListLoading): void {
        const listStatus = ctx.getState().listLoading || {};
        const listCount = listStatus[action.listId] ?? 0;

        ctx.setState(
            patch({
                listLoading: {
                    ...listStatus,
                    // Prevent underflows/negative values
                    [action.listId]: Math.max(0, listCount - 1),
                },
            }),
        );
    }
}
