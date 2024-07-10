import { Injectable } from '@angular/core';
import { InheritableItem, ItemType } from '@gentics/cms-models';
import { Observable, forkJoin } from 'rxjs';
import { defaultIfEmpty, map } from 'rxjs/operators';
import { Api } from '../api/api.service';
import { EntityResolver } from '../entity-resolver/entity-resolver';

export interface LocalizationInfo { nodeName: string; itemId: number; }

export interface LocalizationMap {
    [itemId: number]: LocalizationInfo[];
}

/**
 * A service for fetching the localizations of an object.
 */
@Injectable()
export class LocalizationsService {

    constructor(
        private api: Api,
        private entityResolver: EntityResolver,
    ) { }

    /**
     * Returns a map of localized version info for each given item.
     */
    getLocalizationMap(itemIds: number[], type: ItemType): Observable<LocalizationMap>;
    getLocalizationMap(items: InheritableItem[]): Observable<LocalizationMap>;
    getLocalizationMap(itemsOrIds: Array<InheritableItem | number>, type?: ItemType): Observable<LocalizationMap> {
        const normalizedItems = itemsOrIds.map(itemOrId => {
            if (typeof itemOrId !== 'number') {
                return itemOrId;
            }

            return {
                id: itemOrId,
                type,
            };
        });

        return forkJoin(normalizedItems.map(item => this.getLocalizations(item.type, item.id))).pipe(
            map(localizationsArray => {
                const map: LocalizationMap = {};
                normalizedItems.forEach((item, index) => {
                    map[item.id] = localizationsArray[index];
                });
                return map;
            }),
            defaultIfEmpty({}),
        );
    }

    /**
     * Returns an array of info containing ids and channel name for any localized versions of the given item.
     */
    getLocalizations(type: ItemType, id: number): Observable<LocalizationInfo[]> {
        return this.api.folders.getLocalizations(type, id).pipe(
            map(response => {
                return Object.keys(response.nodeIds)
                    .map(stringId => Number(stringId))
                    .filter(itemId => itemId !== id)
                    .map(itemId => {
                        const node = this.entityResolver.getNode(response.nodeIds[itemId]);
                        return {
                            nodeName: node.name,
                            itemId,
                        };
                    });
            }),
        );
    }
}
