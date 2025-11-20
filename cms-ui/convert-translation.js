const { opendirSync, readFileSync, writeFileSync } = require("node:fs");
const { basename, join } = require("node:path");
const { fileExistsSync } = require("tsconfig-paths/lib/filesystem");

const sourceFolder = process.argv[2];
const targetFolder = process.argv[3];
const EXT = '.translations.json';

/*
([\s|])i18n([:\s\}\)"])
$1translate$2
*/

const source = opendirSync(sourceFolder);
let srcFile;

while ((srcFile = source.readSync()) != null) {
    if (!srcFile.isFile() || !srcFile.name.endsWith(EXT)) {
        continue;
    }

    const sourceFilePath = join(sourceFolder, srcFile.name);
    console.log({ sourceFilePath });
    const content = JSON.parse(readFileSync(sourceFilePath).toString());
    const based = basename(srcFile.name);
    const group = based.substring(0, based.length - EXT.length);
    // console.log({
    //     sourceFilePath,
    //     group,
    //     content,
    // });

    const langContent = {};

    Object.keys(content).forEach(id => {
        const data = content[id];
        Object.entries(data).forEach(([key, value]) => {
            if (!langContent[key]) {
                langContent[key] = {};
            }
            const langData = langContent[key];
            if (!langData[group]) {
                langData[group] = {};
            }
            const groupData = langData[group];
            groupData[id] = value;
        });
    });

    // console.log({ langContent});

    Object.entries(langContent).forEach(([lang, data]) => {
        const targetFile = join(targetFolder, `${lang}.json`);
        console.log({ lang, targetFile });
        if (!fileExistsSync(targetFile)) {
            writeFileSync(targetFile, '');
        }
        const langFileContent = readFileSync(targetFile);
        let output = {};
        try {
            output = JSON.parse(langFileContent.toString());
        } catch (err) {}
        output = {
            ...output,
            ...data,
        };
        writeFileSync(targetFile, JSON.stringify(output, null, 4));
    });
}
