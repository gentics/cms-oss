import { Injectable } from '@angular/core';
import { IS_NORMALIZED, RecursivePartial } from '@gentics/cms-models';
import { Store } from '@ngxs/store';
import { cloneDeep, mergeWith } from 'lodash-es';
import { Observable, defer } from 'rxjs';
import { finalize } from 'rxjs/operators';
import {
    AppState,
    AuthState,
    ContentStagingState,
    EditorState,
    EntityState,
    EntityTypesMap,
    FavouritesState,
    FeaturesState,
    FolderState,
    MaintenanceModeState,
    MessageState,
    PublishQueueState,
    ToolsState,
    UIState,
    UsageState,
    UserState,
    WastebinState,
} from '../common/models';
import { ApplicationStateService } from './providers';

/** Only available in testing. A partial type that can be used to mock application state. */
export type MockAppState = Partial<AppState> | {
    auth?: Partial<AuthState>;
    contentStaging?: Partial<ContentStagingState>;
    editor?: TwoLevelsPartial<EditorState>;
    entities?: PartialEntityState;
    favourites?: Partial<FavouritesState>;
    features?: Partial<FeaturesState>;
    folder?: TwoLevelsPartial<FolderState>;
    maintenanceMode?: Partial<MaintenanceModeState>;
    messages?: MessageState;
    publishQueue?: Partial<PublishQueueState>;
    tools?: Partial<ToolsState>;
    ui?: Partial<UIState>;
    usage?: Partial<UsageState>;
    user?: Partial<UserState>;
    wastebin?: TwoLevelsPartial<WastebinState>;
};

export const REPLACE_MOCK_OBJECT = Symbol('replace-mock');

export const replaceInState: <T>(value: any) => T = (value) => {
    value[REPLACE_MOCK_OBJECT] = true;
    return value;
};

/** ApplicationStateService for tests. */
@Injectable()
export class TestApplicationState extends ApplicationStateService {

    /** Only available in testing. All tracked method calls will be saved in this array. */
    public trackedActionCalls: Array<{ method: string, args: any[] }> = [];

    /** Only available in testing. All subscribed select() calls will be saved in this array. */
    private trackedSubscriptions: Array<(state: AppState) => any> = [];

    constructor(store: Store) {
        super(store);
    }

    /** Only available in testing. Updates the app state with a partial hash. */
    public mockState(partialState: MockAppState, replaceWithoutMerge: boolean = false): void {
        let newState: MockAppState;

        if (replaceWithoutMerge) {
            newState = cloneDeep(partialState);
        } else {
            const currentState: AppState = cloneDeep(this.store.snapshot());
            // Merge the state, but replace all arrays. Otherwise, you can never truly replace arrays
            newState = mergeWith({}, currentState, partialState, (a, b) => {
                if (Array.isArray(a) || Array.isArray(b)) {
                    return b;
                }
                if (b != null && typeof b === 'object' && b[REPLACE_MOCK_OBJECT]) {
                    return b;
                }
            });
        }

        // Since _.merge() ignores symbols, we need to manually copy [IS_NORMALIZED] for entities.
        if (partialState.entities) {
            this.applyNormalizationStatuses(newState as any, partialState.entities);
        }

        this.store.reset(newState);
    }

    private applyNormalizationStatuses(newState: AppState, modifiedEntities: RecursivePartial<EntityState>): void {
        Object.keys(modifiedEntities).forEach(type => {
            const srcEntityBranch = modifiedEntities[type];
            const destEntityBranch = newState.entities[type];

            Object.keys(srcEntityBranch).forEach(id => {
                const entityChanges = srcEntityBranch[id];
                if (entityChanges[IS_NORMALIZED]) {
                    destEntityBranch[id][IS_NORMALIZED] = entityChanges[IS_NORMALIZED];
                }
            });
        });
    }

    /**
     * Only available in testing. Track all select() calls.
     * Returns an array that contains the selector of every `select()` call
     * which has been subscribed to. Unsubscribing removes the selector from the array.
     */
    public trackSubscriptions(): Array<(state: AppState) => any> {
        this.trackedSubscriptions = [];
        const subscriptionList = this.trackedSubscriptions;

        this.select = jasmine.createSpy('select').and.callFake((selector: any): Observable<any> => {
            const observable: Observable<any> = super.select(selector);
            return defer(() => {
                subscriptionList.push(selector);
                return observable;
            }).pipe(
                finalize(() => {
                    const index = subscriptionList.indexOf(selector);
                    if (index >= 0) {
                        subscriptionList.splice(index, 1);
                    }
                }),
            );
        });

        return subscriptionList;
    }

    /**
     * Only available in testing.
     * When trackSubscriptions() is used, returns the state branches that `select()` calls map to.
     *
     * @example
     *     appState.trackSubscriptions();
     *     const stream = appState.select(state => state.editor && state.folder);
     *     appState.getSubscribedBranches(); // => []
     *     const sub = stream.subscribe();
     *     appState.getSubscribedBranches(); // => ['editor', 'folder']
     *     sub.unsubscribe();
     *     appState.getSubscribedBranches(); // => []
     */
    public getSubscribedBranches(selectors?: Array<(state: AppState) => any>): Array<keyof AppState> {
        selectors = selectors || this.trackedSubscriptions;

        const usedBranches: any = {};
        for (const selectorFunction of selectors) {
            // Get name of first function argument from function body
            // Parsing the source of a function is NOT a best practice,
            // but during the upgrade to Angular 8 and ES2015 output, we decided that it
            // would be less effort to just fix a failing regular expression, than
            // to properly redesign and reimplement state subscription logging in the tests.
            const source: string = Function.prototype.toString.call(selectorFunction);
            const regexEs2015 = /^\(?(\w+)\)?\s*=>\s*(.+)$/;
            let parsed = regexEs2015.exec(source);
            if (!parsed) {
                // eslint-disable-next-line no-useless-escape
                const regexEs5 = /^function\s*[\(]*\(\s*([^,\)\s]+)[^\)]*\)\s*\{(.+)\}$/;
                parsed = regexEs5.exec(source);
                if (!parsed) {
                    continue;
                }
            }
            const [, firstParam, functionBody] = parsed;
            // Search for "state.something" in the function body
            // eslint-disable-next-line no-useless-escape
            const regex = new RegExp(`(?:^|[^.\[])${firstParam}\.([a-zA-Z0-9_$]+)`, 'g');
            const matches = functionBody.match(regex) || [];
            for (const match of matches) {
                const accessedProperty = match.split('.')[1];
                usedBranches[accessedProperty] = true;
            }
        }

        return Object.keys(usedBranches).sort() as Array<keyof AppState>;
    }
}

export type TwoLevelsPartial<T> = { [K in keyof T]?: Partial<T[K]> };
export type PartialEntityState = { [K in keyof EntityState]?: PartialHash<EntityTypesMap[K]> };
export type PartialHash<T> = { [id: number]: TwoLevelsPartial<T> };
