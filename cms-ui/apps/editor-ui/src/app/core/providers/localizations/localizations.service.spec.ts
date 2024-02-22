import { TestBed, waitForAsync } from '@angular/core/testing';
import { ItemType, LocalizationsResponse } from '@gentics/cms-models';
import { Observable, of } from 'rxjs';
import { Api } from '../api/api.service';
import { EntityResolver } from '../entity-resolver/entity-resolver';
import { LocalizationsService } from './localizations.service';

const mockNodeName = 'MockNode';

describe('LocalizationsService', () => {

    let localizationsService: LocalizationsService;
    let api: MockApi;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                LocalizationsService,
                { provide: EntityResolver, useClass: MockEntityResolver },
                { provide: Api, useClass: MockApi },
            ],
        });
        localizationsService = TestBed.get(LocalizationsService);
        api = TestBed.get(Api);
    });

    describe('getLocalizations()', () => {

        it('should return correct result when no localizations exist', waitForAsync(() => {
            const itemId = 4;
            spyOn(api.folders, 'getLocalizations').and.returnValue(of({
                masterId: itemId,
                nodeIds: {},
                masterNodeId: 1,
            }) as any);

            localizationsService.getLocalizations('page', itemId)
                .subscribe(result => {
                    expect(result).toEqual([]);
                });
        }));

        it('should return correct result when one localization exists', () => {
            const itemId = 4;
            spyOn(api.folders, 'getLocalizations').and.returnValue(of({
                masterId: itemId,
                nodeIds: {
                    23: 2,
                },
                masterNodeId: 1,
            }) as any);

            localizationsService.getLocalizations('page', itemId)
                .subscribe(result => {
                    expect(result).toEqual([
                        { itemId: 23, nodeName: mockNodeName },
                    ]);
                });
        });

        it('should return correct result when several localizations exist', () => {
            const itemId = 4;
            spyOn(api.folders, 'getLocalizations').and.returnValue(of({
                masterId: itemId,
                nodeIds: {
                    23: 2,
                    44: 3,
                    55: 4,
                },
                masterNodeId: 1,
            }) as any);

            localizationsService.getLocalizations('page', itemId)
                .subscribe(result => {
                    expect(result).toEqual([
                        { itemId: 23, nodeName: mockNodeName },
                        { itemId: 44, nodeName: mockNodeName },
                        { itemId: 55, nodeName: mockNodeName },
                    ]);
                });
        });

        it('should omit self from results', () => {
            const itemId = 4;
            spyOn(api.folders, 'getLocalizations').and.returnValue(of({
                masterId: itemId,
                nodeIds: {
                    23: 2,
                    [itemId]: 3,
                    55: 4,
                },
                masterNodeId: 1,
            }) as any);

            localizationsService.getLocalizations('page', itemId)
                .subscribe(result => {
                    expect(result).toEqual([
                        { itemId: 23, nodeName: mockNodeName },
                        { itemId: 55, nodeName: mockNodeName },
                    ]);
                });
        });
    });

    describe('getLocalizationMap()', () => {


        it('should return correct value when given array of ids', () => {
            const itemId = 4;
            spyOn(api.folders, 'getLocalizations').and.returnValue(of({
                masterId: itemId,
                nodeIds: {
                    23: 2,
                    55: 4,
                },
                masterNodeId: 1,
            }) as any);

            localizationsService.getLocalizationMap([itemId], 'page')
                .subscribe(result => {
                    expect(result).toEqual({
                        [itemId]: [
                            { itemId: 23, nodeName: mockNodeName },
                            { itemId: 55, nodeName: mockNodeName },
                        ],
                    });
                });
        });

        it('should return correct value when given array of items', () => {
            const itemId = 4;
            spyOn(api.folders, 'getLocalizations').and.returnValue(of({
                masterId: itemId,
                nodeIds: {
                    23: 2,
                    55: 4,
                },
                masterNodeId: 1,
            }) as any);

            const mockItem: any = {
                id: itemId,
                type: 'page',
            };
            localizationsService.getLocalizationMap([mockItem], 'page')
                .subscribe(result => {
                    expect(result).toEqual({
                        [itemId]: [
                            { itemId: 23, nodeName: mockNodeName },
                            { itemId: 55, nodeName: mockNodeName },
                        ],
                    });
                });
        });

        it('should return an empty object when given an empty item array', () => {
            localizationsService.getLocalizationMap([], 'page')
                .subscribe(result => {
                    expect(result).toEqual({});
                });
        });
    });


});

class MockApi {
    folders = {
        getLocalizations: (
            type: ItemType,
            id: number,
        ): Observable<LocalizationsResponse> => of({}) as any,
    };
}

class MockEntityResolver {
    getNode(): any {
        return { name: mockNodeName };
    }
}
