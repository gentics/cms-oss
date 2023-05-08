import { Injectable } from '@angular/core';
import { fileSchema, imageSchema, pageSchema } from '@editor-ui/app/common/models';
import {
    File,
    Folder,
    Image,
    Page,
    Template,
} from '@gentics/cms-models';
import { StateContext } from '@ngxs/store';
import { Schema, normalize, schema as schemaNamespace } from 'normalizr';
import { UsageState } from '../../../common/models';
import { ActionDefinition, AppStateBranch, getNormalizrSchema } from '../../state-utils';
import { AddEntitiesAction } from '../entity/entity.actions';
import { ItemUsageFetchingErrorAction, ItemUsageFetchingSuccessAction, StartItemUsageFetchingAction, USAGE_STATE_KEY } from './usage.actions';

type UsageType = keyof Omit<UsageState, 'itemType' | 'itemId' | 'fetching'>;

const USAGE_TYPES: UsageType[] = [
    'files',
    'folders',
    'images',
    'pages',
    'tags',
    'templates',
    'variants',
    'linkedPages',
    'linkedFiles',
    'linkedImages',
];

export interface PartialUsageData {
    /** Files that reference the item the usage was requested for. */
    files?: File[];
    /** Folders that reference the item the usage was requested for. */
    folders?: Folder[];
    /** Images that reference the item the usage was requested for. */
    images?: Image[];
    /** Pages that reference the item the usage was requested for. */
    pages?: Page[];
    /** Pages which contain tags with reference for the item the usage was requested for. */
    tags?: Page[];
    /** Templates that reference the item the usage was requested for. */
    templates?: Template[];
    /** Page variants that reference the item the usage was requested for. */
    variants?: Page[];
}

const INITIAL_USAGE_STATE: UsageState = {
    linkedPages: [],
    linkedFiles: [],
    linkedImages: [],
    files: [],
    folders: [],
    images: [],
    pages: [],
    tags: [],
    templates: [],
    variants: [],
    fetching: false,
    itemType: undefined,
    itemId: undefined,
};

@AppStateBranch<UsageState>({
    name: USAGE_STATE_KEY,
    defaults: INITIAL_USAGE_STATE,
})
@Injectable()
export class UsageStateModule  {

    @ActionDefinition(StartItemUsageFetchingAction)
    handleStartItemUsageFetchingAction(ctx: StateContext<UsageState>, action: StartItemUsageFetchingAction): void {
        const diff: Partial<UsageState> = {
            fetching: true,
            itemType: action.itemType,
            itemId: action.itemId,
        };
        for (let type of USAGE_TYPES) {
            diff[type] = [];
        }

        ctx.patchState(diff);
    }

    @ActionDefinition(ItemUsageFetchingSuccessAction)
    handleItemUsageFetchingSuccessAction(ctx: StateContext<UsageState>, action: ItemUsageFetchingSuccessAction): void {
        const diff: Partial<UsageState> = {
            fetching: false,
        };

        for (let usageType of USAGE_TYPES) {
            const typeData: any[] = action.usageData[usageType];
            if (!typeData) {
                continue;
            }

            diff[usageType] = typeData.map(item => item.id);
            const normalized = normalize(typeData, new schemaNamespace.Array(getUsageSchema(usageType)));
            ctx.dispatch(new AddEntitiesAction(normalized));
        }

        ctx.patchState(diff);
    }

    @ActionDefinition(ItemUsageFetchingErrorAction)
    handleItemUsageFetchingErrorAction(ctx: StateContext<UsageState>, action: ItemUsageFetchingErrorAction): void {
        ctx.patchState({
            fetching: false,
        });
    }
}

function getUsageSchema(itemType: UsageType): Schema {
    const found = getNormalizrSchema(itemType, true);
    if (found) {
        return found;
    }

    switch (itemType) {
        case 'tags':
        case 'variants':
        case 'linkedPages':
            return pageSchema;
        case 'linkedFiles':
            return fileSchema;
        case 'linkedImages':
            return imageSchema;
    }
}
