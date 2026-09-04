/**
 * Base class for all Keycloak releated errors
 */
export abstract class KeycloakError extends Error {}

/**
 * Error for when the configuration could not be loaded (Usually indicates that it's not enabled)
 */
export class KeycloakConfigLoadError extends Error {}

/**
 * Error for when the configuration for keycloak is invalid.
 */
export class KeycloakInvalidConfigError extends Error {}

/**
 * Error for when the Keycloak instance is unreachable.
 */
export class KeycloakUnreachableError extends Error {}

/**
 * Error for when the Keycloak-Adapter couldn't be initialized
 */
export class KeycloakInitializationError extends Error {}
