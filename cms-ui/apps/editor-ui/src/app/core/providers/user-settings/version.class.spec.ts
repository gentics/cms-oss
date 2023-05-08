import {Version} from './version.class';


describe('Version', () => {

    let version: Version;

    describe('constructing', () => {

        it('builds correctly from valid versions', () => {
            version = new Version(1, 2, 3);
            expect(props(version)).toEqual({
                major: 1,
                minor: 2,
                patch: 3,
                meta: '',
                valid: true
            });

            version = new Version(15, 4, 7, '03b0a192');
            expect(props(version)).toEqual({
                major: 15,
                minor: 4,
                patch: 7,
                meta: '03b0a192',
                valid: true
            });
        });

        it('marks 0.0.0 as invalid version', () => {
            version = new Version(0, 0, 0);
            expect(props(version)).toEqual({
                major: 0,
                minor: 0,
                patch: 0,
                meta: '',
                valid: false
            });
        });

    });

    describe('parse()', () => {

        it('can parse valid versions', () => {
            version = Version.parse('0.0.1');
            expect(props(version)).toEqual({
                major: 0,
                minor: 0,
                patch: 1,
                meta: '',
                valid: true
            });

            version = Version.parse('1.17.4-alpha');
            expect(props(version)).toEqual({
                major: 1,
                minor: 17,
                patch: 4,
                meta: 'alpha',
                valid: true
            });

            version = Version.parse('9999.8888.7777-meta');
            expect(props(version)).toEqual({
                major: 9999,
                minor: 8888,
                patch: 7777,
                meta: 'meta',
                valid: true
            });
        });

        it('marks ".1.1" as invalid', () => {
            version = Version.parse('.1.1');
            expect(version.valid).toBe(false);
        });

        it('marks "1.0.0-" as invalid', () => {
            version = Version.parse('1.0.0-');
            expect(version.valid).toBe(false);
        });

        it('marks "1.0" as invalid', () => {
            version = Version.parse('1.0');
            expect(version.valid).toBe(false);
        });

        it('marks "1.0.0- alpha" as invalid', () => {
            version = Version.parse('1.0.0- alpha');
            expect(version.valid).toBe(false);
        });

    });

    describe('isEqualTo()', () => {

        it('returns true for fully equal versions', () => {
            expect(Version.parse('0.0.1').isEqualTo(Version.parse('0.0.1'))).toBe(true);
            expect(Version.parse('1.2.3').isEqualTo(Version.parse('1.2.3'))).toBe(true);
            expect(Version.parse('5.0.0-alpha').isEqualTo(Version.parse('5.0.0-alpha'))).toBe(true);
            expect(Version.parse('9.8.7-f27b2').isEqualTo(Version.parse('9.8.7-f27b2'))).toBe(true);
        });

        it('returns false for different versions', () => {
            expect(Version.parse('1.2.3').isEqualTo(Version.parse('1.2.0'))).toBe(false);
            expect(Version.parse('1.2.3').isEqualTo(Version.parse('1.3.3'))).toBe(false);
            expect(Version.parse('1.2.3').isEqualTo(Version.parse('5.2.3'))).toBe(false);
            expect(Version.parse('1.2.3').isEqualTo(Version.parse('1.2.3-release'))).toBe(false);
            expect(Version.parse('5.0.0-alpha').isEqualTo(Version.parse('5.0.0-beta'))).toBe(false);
            expect(Version.parse('9.8.7-f27b2').isEqualTo(Version.parse('9.8.7-F27B2'))).toBe(false);
        });

        it('returns false for invalid versions', () => {
            expect(Version.parse('1.2').isEqualTo(Version.parse('1.2'))).toBe(false);
            expect(Version.parse('1.2').isEqualTo(Version.parse('1.2.0'))).toBe(false);
            expect(Version.parse('1.2.3-').isEqualTo(Version.parse('1.2.3-'))).toBe(false);
            expect(Version.parse('1.2.3-').isEqualTo(Version.parse('1.2.3'))).toBe(false);
            expect(Version.parse('9.8.7- alpha').isEqualTo(Version.parse('9.8.7- alpha'))).toBe(false);
        });

    });

    describe('isNewerThan()', () => {

        it('returns true for older versions', () => {
            expect(Version.parse('1.2.3').isNewerThan(Version.parse('0.0.1'))).toBe(true, '1.2.3 > 0.0.1');
            expect(Version.parse('1.2.3').isNewerThan(Version.parse('0.1.0'))).toBe(true, '1.2.3 > 0.1.0');
            expect(Version.parse('1.2.3').isNewerThan(Version.parse('1.1.3'))).toBe(true, '1.2.3 > 1.1.3');
            expect(Version.parse('1.2.3').isNewerThan(Version.parse('0.1.0'))).toBe(true, '1.2.3 > 0.1.0');
        });

        it('returns false for equal versions', () => {
            expect(Version.parse('1.2.3').isNewerThan(Version.parse('1.2.3'))).toBe(false, 'not(1.2.3 > 1.2.3)');
            expect(Version.parse('0.1.1').isNewerThan(Version.parse('0.1.1'))).toBe(false, '0.1.1 > 0.1.1');
        });

        it('ignores the meta part', () => {
            expect(Version.parse('2.0.0-alpha.5').isNewerThan(Version.parse('2.0.0-beta.2'))).toBe(false, '2.0.0-alpha.5, 2.0.0-beta.2');
            expect(Version.parse('2.2.0').isNewerThan(Version.parse('2.2.0-beta'))).toBe(false, '2.2.0, 2.2.0-beta');
        });

        it('returns false for newer versions', () => {
            expect(Version.parse('1.2.3').isNewerThan(Version.parse('2.0.0'))).toBe(false, '1.2.3, 2.0.0');
            expect(Version.parse('0.1.1').isNewerThan(Version.parse('0.3.1'))).toBe(false, '0.1.1, 0.3.1');
        });

        it('returns false for invalid versions', () => {
            expect(Version.parse('10.0.0').isNewerThan(Version.parse('buzz'))).toBe(false, '10.0.0, buzz');
            expect(Version.parse('5-1-1').isNewerThan(Version.parse('4.0.0'))).toBe(false, '5-1-1, 4.0.0');
        });

    });

    describe('satisfiesMinimum()', () => {

        it('returns true for newer versions', () => {
            expect(Version.parse('1.2.4').satisfiesMinimum(Version.parse('1.2.3'))).toBe(true);
            expect(Version.parse('1.3.1').satisfiesMinimum(Version.parse('1.2.3'))).toBe(true);
            expect(Version.parse('2.0.0').satisfiesMinimum(Version.parse('1.2.3'))).toBe(true);
        });

        it('returns true for equal versions', () => {
            expect(Version.parse('1.2.3').satisfiesMinimum(Version.parse('1.2.3'))).toBe(true);
            expect(Version.parse('1.2.3-beta.1').satisfiesMinimum(Version.parse('1.2.3'))).toBe(true, 'ignores meta data');
        });

        it('returns false for older versions', () => {
            expect(Version.parse('1.2.2').satisfiesMinimum(Version.parse('1.2.3'))).toBe(false);
            expect(Version.parse('1.1.6').satisfiesMinimum(Version.parse('1.2.3'))).toBe(false);
            expect(Version.parse('0.2.4').satisfiesMinimum(Version.parse('1.2.3'))).toBe(false);
            expect(Version.parse('0.6.4').satisfiesMinimum(Version.parse('1.2.3'))).toBe(false);
        });

        it('returns false for invalid versions', () => {
            expect(Version.parse('10.0.0').isNewerThan(Version.parse('buzz'))).toBe(false, '10.0.0, buzz');
            expect(Version.parse('5-1-1').isNewerThan(Version.parse('4.0.0'))).toBe(false, '5-1-1, 4.0.0');
        });
    });

    describe('toString()', () => {

        it('works correctly for simple versions', () => {
            expect(new Version(0, 0, 1).toString()).toBe('0.0.1');
            expect(new Version(1, 2, 3).toString()).toBe('1.2.3');
        });

        it('works correctly for version with meta string', () => {
            expect(new Version(0, 0, 1, 'alpha').toString()).toBe('0.0.1-alpha');
            expect(new Version(1, 2, 3, '2b4ac').toString()).toBe('1.2.3-2b4ac');
        });

    });

});

function props(obj: any): any {
    let result: any = {};
    for (let key of Object.keys(obj)) {
        result[key] = obj[key];
    }
    return result;
}
