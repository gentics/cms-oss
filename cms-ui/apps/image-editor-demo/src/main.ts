import { provideZoneChangeDetection } from "@angular/core";
import { platformBrowser } from '@angular/platform-browser';
import { PlaygroundModule } from './app/playground.module';

platformBrowser().bootstrapModule(PlaygroundModule, { applicationProviders: [provideZoneChangeDetection()], });
