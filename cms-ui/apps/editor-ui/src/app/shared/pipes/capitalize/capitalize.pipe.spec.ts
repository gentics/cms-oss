import {CapitalizePipe} from './capitalize.pipe';

describe('CapitalizePipe', () => {

    describe('CapitalizePipe', () => {
        let pipe: CapitalizePipe;

        beforeEach(() => {
            pipe = new CapitalizePipe();
        });

        describe('transform', () => {
            it('should transform string to Capitalized versions', () => {
                const result = pipe.transform('yolo');
                expect(result).toBe('Yolo');
            });

            it('should transform all strings to Capitalized versions', () => {
                const input = 'what does the scouter say about its power level';
                const result = pipe.transform(input, true);
                expect(result).toBe('What Does The Scouter Say About Its Power Level');
            });

        });

        describe('#capitalizeWord', () => {
            it('should capitalized a word', () => {
                const result = pipe.capitalizeWord('something');
                expect(result).toBe('Something');
            });

            it('should only capitalized first char', () => {
                const result = pipe.capitalizeWord('something something something');
                expect(result).toBe('Something something something');
            });

        });

    });

});
