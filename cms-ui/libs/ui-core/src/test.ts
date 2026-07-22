import { NgModule, provideZoneChangeDetection } from '@angular/core';
import { getTestBed } from '@angular/core/testing';
import {
    BrowserTestingModule,
    platformBrowserTesting,
} from '@angular/platform-browser/testing';
import 'hammerjs';
import 'moment-timezone';
import 'zone.js';
import 'zone.js/testing';
import { getInstance, setInstance } from './lib/common';

// Correct the timezone, as in the CI it might be different
setInstance(getInstance().tz('Europe/Vienna'));

@NgModule({
    providers: [provideZoneChangeDetection()],
})
class AppTestingModule {}

// First, initialize the Angular testing environment.
getTestBed().initTestEnvironment(
    [BrowserTestingModule, AppTestingModule],
    platformBrowserTesting(), {
        teardown: { destroyAfterEach: false },
    },
);
