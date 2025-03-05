/*
 * This config is only needed for tests, which defaults imports to node packages,
 * which are wrongly required/loaded, due to the keycloak-js import.
 */
module.exports = (config, options, targetOptions) => {
    config.resolve.fallback = {
        ...config.resolve.fallback,
        'buffer': false,
        'crypto': false,
        'stream': false,
    };

    return config;
}
