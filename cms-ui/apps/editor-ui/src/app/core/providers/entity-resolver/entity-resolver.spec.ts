import { TestBed } from '@angular/core/testing';
import { GcmsNormalizer, Normalized, NormalizedEntityStore, Page } from '@gentics/cms-models';
import { getExampleEntityStore } from '@gentics/cms-models/testing/entity-store-data.mock';
import { NgxsModule } from '@ngxs/store';
import { BehaviorSubject } from 'rxjs';
import { ApplicationStateService, STATE_MODULES } from '../../../state';
import { EntityResolver } from './entity-resolver';

describe('EntityResolver:', () => {

    const mockEntities: NormalizedEntityStore = getExampleEntityStore();
    class MockAppStore {
        select(): BehaviorSubject<any> {
            return new BehaviorSubject<any>(mockEntities);
        }
    }

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                EntityResolver,
                { provide: ApplicationStateService, useClass: MockAppStore },
            ],
        });
    });

    describe('EntityResolver:', () => {
        let entityResolver: EntityResolver;

        beforeEach(() => {
            entityResolver = TestBed.inject(EntityResolver);
        });

        it('should get a folder', () => {
            expect(entityResolver.getEntity('folder', 1)).toBe(mockEntities.folder[1]);
        });

        it('should get a user', () => {
            expect(entityResolver.getEntity('user', 3)).toBe(mockEntities.user[3]);
        });

        it('should get a page', () => {
            expect(entityResolver.getEntity('page', 3)).toBe(mockEntities.page[3]);
        });

        it('should return undefined for non-existent entity id', () => {
            const badId = 3000;
            expect(entityResolver.getEntity('page', badId)).toBeUndefined();
        });

        it('should return undefined for non-existent entity type', () => {
            const badEntityType: any = 'badEntityType';
            expect(entityResolver.getEntity(badEntityType, 1)).toBeUndefined();
        });

        describe('getTemplateByName()', () => {

            it('should return the correct template', () => {
                expect(entityResolver.getTemplateByName('Wikipage')).toBe(mockEntities.template[2]);
            });

            it('should return an empty object if not found', () => {
                expect(entityResolver.getTemplateByName('bad_name')).toEqual({} as any);
            });
        });

        describe('getLanguageByName()', () => {

            it('should return the correct language', () => {
                expect(entityResolver.getLanguageByName('English')).toBe(mockEntities.language[2]);
            });

            it('should return an empty object if not found', () => {
                expect(entityResolver.getLanguageByName('bad_name')).toEqual({} as any);
            });
        });

        describe('getLanguageByCode()', () => {

            it('should return the correct language', () => {
                expect(entityResolver.getLanguageByCode('en')).toBe(mockEntities.language[2]);
            });

            it('should return an empty object if not found', () => {
                expect(entityResolver.getLanguageByCode('bad_code')).toEqual({} as any);
            });
        });

    });

    describe('denormalizeEntity()', () => {

        const PAGE_ID = 3;

        let entityResolver: EntityResolver;
        let normalizer: GcmsNormalizer;

        beforeEach(() => {
            entityResolver = new EntityResolverWithAccessibleNormalizer((new MockAppStore()) as unknown as ApplicationStateService);
            TestBed.overrideProvider(EntityResolver, { useValue: entityResolver });
            normalizer = (entityResolver as EntityResolverWithAccessibleNormalizer).normalizer;
        });

        it('uses GcmsNormalizer to denormalize an entity', () => {
            const page: Page<Normalized> = mockEntities.page[PAGE_ID] as any;
            const denormalizeSpy = spyOn(normalizer, 'denormalize').and.callThrough();
            const result = entityResolver.denormalizeEntity('page', page);

            expect(result).toBeTruthy();
            expect(denormalizeSpy).toHaveBeenCalledTimes(1);
            expect(denormalizeSpy).toHaveBeenCalledWith('page', page, mockEntities);
            expect(result).toBe(denormalizeSpy.calls.all()[0].returnValue as any);
        });

    });

});

class EntityResolverWithAccessibleNormalizer extends EntityResolver {
    normalizer: GcmsNormalizer;
}
