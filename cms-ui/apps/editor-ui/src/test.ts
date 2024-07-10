// This file is required by karma.conf.js and loads recursively all the .spec and framework files

// zone.js needs to be imported first, so we disable automatic import ordering.
import 'zone.js/testing';
import { getTestBed } from '@angular/core/testing';
import {
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';

// First, initialize the Angular testing environment.
getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting(), {
        teardown: { destroyAfterEach: false },
    },
);
