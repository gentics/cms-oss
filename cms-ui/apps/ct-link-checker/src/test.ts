// This file is required by karma.conf.js and loads recursively all the .spec and framework files

// zone.js/testing must the the very first import in this file,
// otherwise it can cause strange behaviours
import 'zone.js/testing';

// tslint:disable-next-line: ordered-imports
import { getTestBed } from '@angular/core/testing';
import {
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing'; /* eslint-disable-line @typescript-eslint/naming-convention */

// First, initialize the Angular testing environment.
getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting(), {
        teardown: { destroyAfterEach: false },
    },
);
