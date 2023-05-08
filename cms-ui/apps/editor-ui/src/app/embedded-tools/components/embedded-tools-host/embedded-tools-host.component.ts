import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { EmbeddedTool } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { ApplicationStateService } from '../../../state';

/**
 * Host component for {@link ToolIframeComponent} instances.
 */
@Component({
    selector: 'embedded-tools-host',
    templateUrl: './embedded-tools-host.html',
    styleUrls: ['./embedded-tools-host.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmbeddedToolsHostComponent implements OnInit {

    displayedTool$: Observable<string>;
    toolIframes$: Observable<Array<EmbeddedTool>>;

    constructor(
        private state: ApplicationStateService,
    ) { }

    ngOnInit(): void {
        this.displayedTool$ = this.state.select(state => state.tools.visible);

        this.toolIframes$ = this.state.select(state => state.tools).pipe(
            distinctUntilChanged((a, b) => a.active === b.active),
            map(tools =>
                tools.active
                    .map(key => tools.available.find(tool => tool.key === key))
                    .filter(tool => !tool.newtab),
            ),
        );
    }

    trackToolByKey(index: number, tool: EmbeddedTool): string {
        return tool.key;
    }
}
