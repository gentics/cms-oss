import { License, LicenseStatus } from '@gentics/cms-models';
import { dateInYears } from '@gentics/ui-core';
import { random, shuffle } from 'lodash-es';
import { v4 } from 'uuid';

const FEATURE_MAP: Record<string, string> = {
    CN: 'Gentics Content.Node',
    TTM: 'Feature Tag Type Migration / Template migration',
    MCCR: 'MultiChanneling ContentRepositories',
    MC: 'MultiChanneling',
    CS: 'Content Staging',
    FORMS: 'Mesh Forms Plugin',
    MCMT: 'Mesh Comment Plugin',
    MEXP: 'Mesh Export Plugin',
    MFAV: 'Mesh Favourite Plugin',
    MLKE: 'Mesh Like Plugin',
    MSQL: 'Mesh SQL',
    PJ: 'Portal | java',
    CLUSTER: 'CMS Clustering'
};
const FEATURES = Object.keys(FEATURE_MAP);

export function createMockLicenseInfo(): { status: LicenseStatus, license: License } {
    const earlier = new Date();
    earlier.setDate(earlier.getDate() - 2);

    let mockLicense: License | null = null;

    if (Math.round(Math.random()) === 0) {
        mockLicense = {
            uuid: window.crypto?.randomUUID?.() || v4(),
            features: shuffle(FEATURES)
                .slice(0, random(2, FEATURES.length))
                .reduce((acc, feat) => {
                    acc[feat] = FEATURE_MAP[feat];
                    return acc;
                }, {}),
            issuedAt: dateInYears(-1).toISOString(),
            validUntil: Math.round(Math.random()) === 0 ? earlier.toISOString() : null,
            signature: '',
        };
        mockLicense.signature = mockLicense.uuid;
    }

    return {
        status: mockLicense == null ? LicenseStatus.MISSING : (
            Math.round(Math.random()) === 0 ? LicenseStatus.VALID : LicenseStatus.INVALID
        ),
        license: mockLicense,
    }
}
