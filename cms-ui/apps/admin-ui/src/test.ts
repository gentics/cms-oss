import { NgModule, provideZoneChangeDetection } from '@angular/core';
import { getTestBed } from '@angular/core/testing';
import {
    BrowserTestingModule,
    platformBrowserTesting,
} from '@angular/platform-browser/testing';
import 'zone.js';
import 'zone.js/testing';

@NgModule({
    providers: [provideZoneChangeDetection()],
})
class AppTestingModule {}

getTestBed().initTestEnvironment(
    [BrowserTestingModule, AppTestingModule],
    platformBrowserTesting(), {
        teardown: { destroyAfterEach: false },
    },
);
