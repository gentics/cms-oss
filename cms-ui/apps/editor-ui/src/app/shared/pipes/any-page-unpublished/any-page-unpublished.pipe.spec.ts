import {AnyPageUnpublishedPipe} from './any-page-unpublished.pipe';


describe('AnyPageUnpublishedPipe', () => {

    describe('AnyPageUnpublishedPipe', () => {
        let pipe: AnyPageUnpublishedPipe;
        let input: any;

        beforeEach(() => {
            pipe = new AnyPageUnpublishedPipe();
        });

        it('returns false if input is not an array of pages', () => {
            input = [];
            expect(pipe.transform(input)).toBe(false);

            input = null;
            expect(pipe.transform(input)).toBe(false);

            input = undefined;
            expect(pipe.transform(input)).toBe(false);

            input = ['a', 'b'];
            expect(pipe.transform(input)).toBe(false);

            input = [{ type: 'not a page' }];
            expect(pipe.transform(input)).toBe(false);

            input = 15;
            expect(pipe.transform(input)).toBe(false);
        });

        it('returns false if no passed page is unpublished', () => {
            input = [
                { type: 'page', online: true },
                { type: 'page', online: true },
                { type: 'page', online: true },
                { type: 'page', online: true }
            ];
            expect(pipe.transform(input)).toBe(false);
        });

    });

});
