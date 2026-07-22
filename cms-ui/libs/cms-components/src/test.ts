// This file is required by karma.conf.js and loads recursively all the .spec and framework files
import { NgModule, provideZoneChangeDetection } from '@angular/core';
import { getTestBed } from '@angular/core/testing';
import {
    BrowserTestingModule,
    platformBrowserTesting,
} from '@angular/platform-browser/testing';
import 'zone.js';
import 'zone.js/testing';

(window as any).global = window;

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
