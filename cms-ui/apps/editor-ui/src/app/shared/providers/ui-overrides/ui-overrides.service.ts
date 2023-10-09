import { HttpClient } from '@angular/common/http';
import { Injectable, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { CloseToolAction, OpenToolAction, SetUIOverridesAction } from '@editor-ui/app/state';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { CUSTOMER_CONFIG_PATH } from '../../../common/config/config';
import { ApplicationStateService } from '../../../state';
import { UIOverrideParameters, UIOverrides, UIToolOverride } from './ui-overrides.model';

@Injectable()
export class UIOverridesService implements OnDestroy {

    private subscriptions = new Subscription();

    constructor(
        private http: HttpClient,
        private router: Router,
        private state: ApplicationStateService,
    ) { }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    loadCustomerConfiguration(): void {
        const uiOverridesPath = `${CUSTOMER_CONFIG_PATH}config/ui-overrides.json?t=${Date.now()}`;

        this.http.get(uiOverridesPath, { responseType: 'text' }).pipe(
            filter(text => !!text),
        ).subscribe(jsonResponse => {
            try {
                const uiOverrides = JSON.parse(jsonResponse);
                this.state.dispatch(new SetUIOverridesAction(uiOverrides));
            } catch (err) {
                console.error('Invalid JSON in ui-overrides.json!');
            }
        }, () => {
            this.state.dispatch(new SetUIOverridesAction({}));
        });
    }

    runOverride(slotName: keyof UIOverrides, params: UIOverrideParameters = {}): void {
        const override = this.state.now.ui.overrides[slotName] as UIToolOverride;
        if (override && override.openTool) {
            const tool = this.state.now.tools.available.find(tool => tool.key === override.openTool);
            if (!tool) {
                console.error(`"${slotName}" is configured to open tool "${override.openTool}",` +
                    ' but it is not available. Check configuration and permissions.');
                return;
            }

            const pathInTool = this.replacePlaceholders(override.toolPath || '', params);
            const openTool = () => {
                if (tool.newtab) {
                    this.state.dispatch(new OpenToolAction(tool.key, pathInTool));
                } else {
                    const url = `/tools/${tool.key}${pathInTool ? '/' + pathInTool : ''}`;
                    this.router.navigateByUrl(url);
                }
            };

            if (override.restartTool) {
                this.state.dispatch(new CloseToolAction(tool.key));
                const timeout = setTimeout(openTool, 100);
                this.subscriptions.add(() => clearTimeout(timeout));
            } else {
                openTool();
            }
        }
    }

    /** Relaces placeholders in a string, e.g. "Madness? This is {{COUNTRY}}!!!"" */
    replacePlaceholders(input: string, variables: UIOverrideParameters = {}): string {
        return input.replace(/\{\{\s*([a-zA-Z0-9\-_]+)\s*\}\}/g, (fullMatch: string, varName: string) => {
            if (Object.prototype.hasOwnProperty.call(variables, varName)) {
                return String(variables[varName]);
            } else {
                return '';
            }
        });
    }

}
