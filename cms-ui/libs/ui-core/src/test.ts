import 'zone.js';
import 'zone.js/testing';
import { getTestBed } from '@angular/core/testing';
import {
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import 'hammerjs';
import { getInstance, setInstance } from './lib/common';
import 'moment-timezone';

// Correct the timezone, as in the CI it might be different
setInstance(getInstance().tz('Europe/Vienna'));

// First, initialize the Angular testing environment.
getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting(), {
        teardown: { destroyAfterEach: false },
    },
);
