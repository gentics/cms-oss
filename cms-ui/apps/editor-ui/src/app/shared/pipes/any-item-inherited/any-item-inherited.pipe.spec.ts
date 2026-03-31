import {AnyItemInheritedPipe} from './any-item-inherited.pipe';


describe('AnyItemInheritedPipe', () => {

    describe('AnyItemInheritedPipe', () => {
        let pipe: AnyItemInheritedPipe;
        let input: any;

        beforeEach(() => {
            pipe = new AnyItemInheritedPipe();
        });

        it('returns false if input is not an array of items', () => {
            input = [];
            expect(pipe.transform(input)).toBe(false);

            input = null;
            expect(pipe.transform(input)).toBe(false);

            input = undefined;
            expect(pipe.transform(input)).toBe(false);

            input = ['a', 'b'];
            expect(pipe.transform(input)).toBe(false);

            input = [{ type: 'not an item ' }];
            expect(pipe.transform(input)).toBe(false);

            input = 15;
            expect(pipe.transform(input)).toBe(false);
        });

        it('returns false if no passed item is inherited', () => {
            input = [
                { inherited: false },
                { inherited: false }
            ];
            expect(pipe.transform(input)).toBe(false);
        });

        it('returns true if any of the passed items is inherited', () => {
            input = [
                { inherited: true },
                { inherited: false }
            ];
            expect(pipe.transform(input)).toBe(true);
        });

        it('returns true if all of the passed items are inherited', () => {
            input = [
                { inherited: true },
                { inherited: true }
            ];
            expect(pipe.transform(input)).toBe(true);
        });

    });

});
