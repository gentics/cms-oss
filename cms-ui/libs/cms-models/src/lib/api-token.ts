
/**
 * An Api Token object as returned from the /admin/token endpoint:
 */

export interface ApiTokenBo {
    tmpId: string;
    name: string;
    cdate: string;
    expires: string;
    lastUsed: string;
    valid: boolean;
}

export interface ApiToken {
    token: string;
    data: TokenData
}

interface TokenData {
    id: number;
    userId: number;
    name: string;
    cdate: string;
    expires: string;
    lastUsed: string;
    valid: boolean;
}

export type EditableApiTokenPackage = Pick<TokenData, 'name' | 'expires'>;