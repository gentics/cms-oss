/**
 * An Api Token object as returned from the /admin/token endpoint:
 */

export interface ApiToken {
    token: string;
    data: ApiTokenData;
}

export interface ApiTokenData {
    token: string;
    id: number;
    userId: number;
    name: string;
    cdate: number;
    expires: number;
    lastUsed: number;
    valid: boolean;
}

export type EditableApiToken = {
    name: ApiTokenData['name'];
    expires?: string;
};
