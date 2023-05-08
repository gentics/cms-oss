import { FormGroup, Validators } from '@angular/forms';

// *************************************************************************************************
/**
 * In multiple place user handling might require the following methods to use.
 */
// *************************************************************************************************

/**
 * Default password reactive form input validators
 */
export const PASSWORD_VALIDATORS = [
    Validators.required,
    Validators.minLength(4),
    Validators.maxLength(20),
    Validators.pattern('^[\\w\\-\\.@]+$'),
];

/**
 * RegExp pattern encoded in base64
 *
 * This method actually returns a regular expression string which is stored as base64 because it is cumbersome to write it down appropriate escaping.
 */
export function getPatternEmail(): string {
    // tslint:disable-next-line: max-line-length
    return atob('KD86KD86W2EtejAtOSEjJCUmJyorLz0/Xl9ge3x9fi1dKyg/OlwuW2EtejAtOSEjJCUmJyorLz0/Xl9ge3x9fi1dKykqfCIoPzpbXHgwMS1ceDA4XHgwYlx4MGNceDBlLVx4MWZceDIxXHgyMy1ceDViXHg1ZC1ceDdmXXxcXFtceDAxLVx4MDlceDBiXHgwY1x4MGUtXHg3Zl0pKiIpQCg/Oig/OlthLXowLTldKD86W2EtejAtOS1dKlthLXowLTldKT9cLikrW2EtejAtOV0oPzpbYS16MC05LV0qW2EtejAtOV0pP3xcWyg/Oig/OjI1WzAtNV18MlswLTRdWzAtOV18WzAxXT9bMC05XVswLTldPylcLil7M30oPzoyNVswLTVdfDJbMC00XVswLTldfFswMV0/WzAtOV1bMC05XT98W2EtejAtOS1dKlthLXowLTldOig/OltceDAxLVx4MDhceDBiXHgwY1x4MGUtXHgxZlx4MjEtXHg1YVx4NTMtXHg3Zl18XFxbXHgwMS1ceDA5XHgwYlx4MGNceDBlLVx4N2ZdKSspXF0pKT8=');
}
