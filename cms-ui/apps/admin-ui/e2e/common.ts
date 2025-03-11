export const AUTH_ADMIN = 'admin';
export const AUTH_KEYCLOAK = 'keycloak';
export const AUTH_MESH = 'mesh';

export interface LoginData {
    username: string;
    password: string;
}

export const AUTH = {
    [AUTH_ADMIN]: {
        username: 'node',
        password: 'node',
    },
    [AUTH_KEYCLOAK]: {
        username: 'node',
        password: 'node',
    },
    [AUTH_MESH]: {
        username: 'admin',
        password: 'admin',
        newPassword: 'admin-test',
    },
};
