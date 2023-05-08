import { GetInheritancePipe, InheritanceType } from './get-inheritance.pipe';
import { InheritableItem } from '@gentics/cms-models';

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
                    excluded: false
                } as InheritableItem;

                const result = pipe.transform(item);
                expect(result).toBe(InheritanceType.Inherited);
            });

            it('should transform item into an inheritance type string of Disinherited', () => {
                const item = {
                    disinherited: true,
                    excluded: false
                } as InheritableItem;

                const result = pipe.transform(item);
                expect(result).toBe(InheritanceType.Disinherited);
            });

            it('should transform item into an inheritance type string of Excluded', () => {
                const item = {
                    disinherited: false,
                    excluded: true
                } as InheritableItem;

                const result = pipe.transform(item);
                expect(result).toBe(InheritanceType.Excluded);
            });

        });

    });

});
