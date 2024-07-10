import { InheritableItem } from '@gentics/cms-models';
import { GetInheritancePipe, InheritanceType } from './get-inheritance.pipe';

describe('GetInheritancePipe', () => {

    describe('GetInheritancePipe', () => {
        let pipe: GetInheritancePipe;

        beforeEach(() => {
            pipe = new GetInheritancePipe();
        });

        describe('transform', () => {
            it('should transform item into an inheritance type string of Inherited', () => {
                const item = {
                    disinherited: false,
                    excluded: false,
                    inherited: true,
                } as InheritableItem;

                const result = pipe.transform(item);
                expect(result).toBe(InheritanceType.INHERITED);
            });

            it('should transform item into an inheritance type string of Master', () => {
                const item = {
                    disinherited: false,
                    excluded: false,
                    inherited: false,
                } as InheritableItem;

                const result = pipe.transform(item);
                expect(result).toBe(InheritanceType.MASTER);
            });

            it('should transform item into an inheritance type string of Localized', () => {
                const item = {
                    disinherited: false,
                    excluded: false,
                    inherited: false,
                    inheritedFromId: 2,
                    masterNodeId: 1,
                } as InheritableItem;

                const result = pipe.transform(item);
                expect(result).toBe(InheritanceType.LOCALIZED);
            });

            it('should transform item into an inheritance type string of Disinherited', () => {
                const item = {
                    disinherited: true,
                    excluded: false,
                    inherited: false,
                } as InheritableItem;

                const result = pipe.transform(item);
                expect(result).toBe(InheritanceType.DISINHERITED);
            });

            it('should transform item into an inheritance type string of Excluded', () => {
                const item = {
                    disinherited: false,
                    excluded: true,
                    inherited: false,
                } as InheritableItem;

                const result = pipe.transform(item);
                expect(result).toBe(InheritanceType.EXCLUDED);
            });

        });

    });

});
