import {AnyItemPublishedPipe} from './any-item-published.pipe';


describe('AnyItemPublishedPipe', () => {

    describe('AnyItemPublishedPipe', () => {
        let pipe: AnyItemPublishedPipe;
        let input: any;

        beforeEach(() => {
            pipe = new AnyItemPublishedPipe();
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

            input = [{ type: 'not a item' }];
            expect(pipe.transform(input)).toBe(false);

            input = 15;
            expect(pipe.transform(input)).toBe(false);
        });

        it('returns false for the given item', () => {
            input = [
                { type: 'item', online: false },
            ];
            expect(pipe.transform(input)).toBe(false);
        });

        it('returns true for the given item', () => {
            input = [
                { type: 'form', online: true },
                { type: 'page', online: true },
            ];
            expect(pipe.transform(input)).toBe(true);
        });

    });

});
