const { resolve } = require('path');
// eslint-disable-next-line @typescript-eslint/naming-convention
const { argv } = require('process');
const { execSync } = require('child_process');
const { readFileSync } = require('fs');

/*
 * This is a rather hacky and annoying fix/workaround to execute the karma tests correctly.
 * We're using the "new" NX project description (`project.json` in each project with definitions),
 * instead of the old/compatibility for the `angular.json` description.
 *
 * However, in NX karma isn't supported as test-runner anymore - or at the very least doesn't
 * properly work any longer - which is why these tests have to be defined in the `angular.json`
 * to actually work.
 *
 * This breaks everything else however, as NX will first try to load the `angular.json` file and
 * check the defintions in there.
 * If you try to run `npm start editor-ui`, it would lookup the start/serve command in the `angular.json`,
 * doesn't find it and give up.
 * That's because it's defined in the new `project.json` file instead.
 * But NX has no fallback for that and simply aborts.
 *
 * This runner will therefore execute the angular tests in the in the `ng-workspace` directory with `ng test`,
 * and all other tests in the regular root with `nx test`.
 */

const WORKSPACE_PATH = resolve(__dirname, '../ng-workspace');
const ANGULAR_JSON = resolve(WORKSPACE_PATH, 'angular.json');
const FORCE_ANGULAR_ARG = '--forceAngular';

(function main() {
    let isArgument = false;
    let projectName = null;
    let forceAngular = false;
    const relevantArguments = argv.slice(2);

    for (const singleArg of relevantArguments) {
        if (isArgument) {
            isArgument = false;
            continue;
        }

        // If it's an option (-f/--foo) without a `=` sign, then the
        // next argument is the option value and has to be skipped as well.
        if (singleArg.startsWith('-')) {
            if (!singleArg.includes('=')) {
                isArgument = true;
            }
            continue;
        }

        projectName = singleArg;
        break;
    }

    if (!projectName) {
        console.log('No Project-Name provided!');
        process.exit(1);
    }

    let idx = relevantArguments.indexOf(FORCE_ANGULAR_ARG);
    if (idx > -1) {
        forceAngular = true;
        relevantArguments.splice(idx, 1);
    }

    const parsed = JSON.parse(readFileSync(ANGULAR_JSON));

    // Check if a test configuration is present in the angular file, then we execute the
    // test as a angular test.
    if (forceAngular || parsed?.projects?.[projectName]?.architect?.test) {
        executeAngularTest(relevantArguments);
        return;
    }

    // Otherwise run the command via NX
    executeNXTest(relevantArguments);
})();

function executeAngularTest(commandArgs) {
    const commandBin = resolve(__dirname, '../node_modules/.bin/ng');

    console.log(`ng-test-runner: Executing angular tests with:\n\t${commandArgs.join('\n\t')}\n\n`);

    execSync(`${commandBin} test ${commandArgs.join(' ')}`, {
        cwd: resolve(__dirname, '../ng-workspace'),
        stdio: 'inherit',
    });
}

function executeNXTest(commandArgs) {
    const commandBin = resolve(__dirname, '../node_modules/.bin/nx');

    console.log(`ng-test-runner: Executing NX tests with:\n\t${commandArgs.join('\n\t')}\n\n`);

    execSync(`${commandBin} test ${commandArgs.join(' ')}`, {
        cwd: resolve(__dirname, '..'),
        stdio: 'inherit',
    });
}
