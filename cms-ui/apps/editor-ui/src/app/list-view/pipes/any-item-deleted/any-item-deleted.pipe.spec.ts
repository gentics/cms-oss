import { AnyItemDeletedPipe } from './any-item-deleted.pipe';


describe('AnyItemDeletedPipe', () => {

    describe('AnyItemDeletedPipe', () => {
        let pipe: AnyItemDeletedPipe;
        let input: any;

        beforeEach(() => {
            pipe = new AnyItemDeletedPipe();
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

        it('returns false if no passed item is deleted', () => {
            input = [
                { deleted: { at: 0 } },
                { deleted: { at: 0 } },
            ];
            expect(pipe.transform(input)).toBe(false);
        });

        it('returns true if any of the passed items is deleted', () => {
            input = [
                { deleted: { at: 1594652421 } },
                { deleted: { at: 0 } },
            ];
            expect(pipe.transform(input)).toBe(true);
        });

        it('returns true if all of the passed items are deleted', () => {
            input = [
                { deleted: { at: 1594652421 } },
                { deleted: { at: 1594652421 } },
            ];
            expect(pipe.transform(input)).toBe(true);
        });

    });

});
