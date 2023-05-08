import {AbstractControl} from '@angular/forms';
import {numberBetween} from './custom-validators';

describe('custom validators', () => {

    describe('numberBetween', () => {

        const controlMock = <AbstractControl> { value: null, setValue(val: any) { this.value = val } };

        it('returns a function', () => {
            expect(typeof numberBetween(1, 5)).toBe('function');
        });

        it('validates the control value to be a number', () => {
            let validator = numberBetween(1, 10);

            controlMock.setValue('a string value');
            expect(validator(controlMock)['valid']).toEqual(false);

            controlMock.setValue(null);
            expect(validator(controlMock)['valid']).toEqual(false);
        });

        it('validates the control value to be above a minimum', () => {
            let validator = numberBetween(5, 10);

            [-100, -20, -5, 0, 1, 2, 3, 4].forEach(tooLowValue => {
                controlMock.setValue(tooLowValue);
                expect(validator(controlMock)).toEqual({ valid: false, belowMinimum: true });
            });

            [5, 6, 7, 8, 9, 10].forEach(validValue => {
                controlMock.setValue(validValue);
                expect(validator(controlMock)).not.toEqual({ valid: false, belowMinimum: true });
            });

            [11, 12, 15, 30, 100, 9999].forEach(tooHighValue => {
                controlMock.setValue(tooHighValue);
                expect(validator(controlMock)).not.toEqual({ valid: false, belowMinimum: true });
            });
        });

        it('validates the control value to be below a maximum', () => {
            let validator = numberBetween(5, 10);

            [11, 12, 15, 30, 100, 9999].forEach(tooHighValue => {
                controlMock.setValue(tooHighValue);
                expect(validator(controlMock)).toEqual({ valid: false, aboveMaximum: true });
            });

            [5, 6, 7, 8, 9, 10].forEach(validValue => {
                controlMock.setValue(validValue);
                expect(validator(controlMock)).not.toEqual({ valid: false, aboveMaximum: true });
            });

            [-100, -20, -5, 0, 1, 2, 3, 4].forEach(tooLowValue => {
                controlMock.setValue(tooLowValue);
                expect(validator(controlMock)).not.toEqual({ valid: false, aboveMaximum: true });
            });
        });

        it('return null when the control value is in the specified range', () => {
            let validator = numberBetween(1, 5);

            [1, 2, 3, 4, 5].forEach(validPositiveValue => {
                controlMock.setValue(validPositiveValue);
                expect(validator(controlMock)).toBe(null);
            });

            validator = numberBetween(-8, -5);
            [-5, -6, -7, -8].forEach(validNegativeValue => {
                controlMock.setValue(validNegativeValue);
                expect(validator(controlMock)).toBe(null);
            });

            validator = numberBetween(9999, 9999);
            controlMock.setValue(9999);
            expect(validator(controlMock)).toBe(null);
        });

        it('throws an exception when created with a minimum that is above the maximum', () => {
            expect(() => numberBetween(5, 4)).toThrow();
            expect(() => numberBetween(-10, -99)).toThrow();
        });

    });

});
