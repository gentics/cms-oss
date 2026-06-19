import * as fs from 'node:fs';
import * as path from 'node:path';

function DefineVersionPlugin(pluginOptions) {
    return {
        name: 'define-version',
        setup(build) {
            if (
                pluginOptions == null
                || typeof pluginOptions.packageFile !== 'string'
                || !pluginOptions.packageFile
            ) {
                throw new Error('Missing required option "packageFile" for plugin "DefineVersionPlugin"!');
            }

            let pkgPath = pluginOptions.packageFile;
            if (!path.isAbsolute(pluginOptions.packageFile)) {
                pkgPath = path.normalize(path.resolve(__dirname, '../..', pkgPath));
            }

            let rawPkg;
            try {
                rawPkg = fs.readFileSync(pkgPath);
            } catch (err) {
                throw new Error(`Could not read package file: ${pkgPath}!`, { cause: err });
            }

            let pkgContent;
            try {
                pkgContent = JSON.parse(rawPkg);
                if (pkgContent == null) {
                    throw new Error('Parsed package is null!');
                } else if (typeof pkgContent !== 'object' || Array.isArray(pkgContent)) {
                    throw new Error('Parsed package is not a object!');
                }
            } catch (err) {
                throw new Error(`Could not parse package file: ${pluginOptions.packageFile}!`, { cause: err });
            }

            if (typeof pkgContent.version !== 'string' || !pkgContent) {
                throw new Error('Package has an no or an invalid version set!');
            }

            const options = build.initialOptions;
            options.define = options.define || {};
            /*
             * FIXME: Figure out why this doesn't properly work.
             * According to the documentation:
             *  - https://nx.dev/docs/technologies/angular/guides/use-environment-variables-in-angular#using-a-custom-esbuild-plugin
             *  - https://esbuild.github.io/plugins/#build-options
             * this is exactly how you do it, but it is simply never set.
             */
            options.define['GCMSUI_VERSION'] = `"${pkgContent.version}"`;
        },
    };
}

module.exports = DefineVersionPlugin;
