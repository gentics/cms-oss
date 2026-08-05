import { readFileSync } from 'node:fs';
import { isAbsolute, normalize, resolve } from 'node:path';

export default function DefineVersionPlugin(pluginOptions) {
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
            if (!isAbsolute(pluginOptions.packageFile)) {
                pkgPath = normalize(resolve(import.meta.dirname, '../..', pkgPath));
            }

            let rawPkg;
            try {
                rawPkg = readFileSync(pkgPath);
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
            options.define['GCMSUI_VERSION'] = `"${pkgContent.version}"`;
        },
    };
}
