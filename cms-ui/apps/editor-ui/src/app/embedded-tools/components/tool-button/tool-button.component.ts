import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { EmbeddedTool } from '@gentics/cms-models';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApplicationStateService } from '../../../state';
import { EmbeddedToolsService } from '../../providers/embedded-tools/embedded-tools.service';

@Component({
    selector: 'tool-button',
    templateUrl: './tool-button.component.html',
    styleUrls: ['./tool-button.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ToolButtonComponent implements OnChanges {

    @Input() tool: EmbeddedTool;

    iconType: 'url' | 'font' | 'fallback';
    fallbackChar$: Observable<string>;
    name$: Observable<string>;
    isActive$: Observable<boolean>;
    href$: Observable<string | undefined>;
    target: '_blank' | '';

    constructor(
        private state: ApplicationStateService,
        private toolsService: EmbeddedToolsService,
    ) { }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.tool) {
            const tool = this.tool;
            if (typeof tool.name === 'string') {
                this.name$ = of(tool.name);
            } else {
                const nameByLanguage = tool.name;
                this.name$ = this.state.select(state => state.ui.language).pipe(
                    map(language => nameByLanguage[language] || titleCase(this.tool.key)),
                );
            }

            this.isActive$ = this.state.select(state => state.tools.active).pipe(
                map(activeTools => activeTools.indexOf(this.tool.key) >= 0),
            );

            this.iconType = determineIconType(this.tool.iconUrl);
            this.fallbackChar$ = this.name$.pipe(map(name => name.substr(0, 1)));

            if (this.tool.newtab) {
                this.target = '_blank';
                this.href$ = of(this.tool.toolUrl);
            } else {
                this.target = '';
                this.href$ = this.state.select(state => state.tools.subpath[this.tool.key]).pipe(
                    map(subpath => '#/tools/' + this.tool.key + (subpath ? '/' + subpath : '')),
                );
            }
        }
    }

    iconClicked(event: Event): void {
        if (!event.defaultPrevented && this.tool.newtab) {
            this.toolsService.openOrFocus(this.tool.key);
            event.preventDefault();
        }
    }

    closeTool(event: Event): void {
        event.preventDefault();
        this.toolsService.close(this.tool.key);
    }

    useFallbackIcon(): void {
        this.iconType = 'fallback';
    }
}

/**
 * Transforms a string to title case.
 *
 * @example
 *    "my example" => "My Example"
 *    "some_name" => "Some Name"
 */
const titleCase = (input: string): string => input.replace(/(?:^|\b|_)\w/g, str => str.toUpperCase());

/**
 * Icons can be provided as an URL or a font icon glyph ("library_books").
 * Otherwise they are displayed as their first letter ("L")
 */
function determineIconType(input: string): 'url' | 'font' | 'fallback' {
    input = input || '';
    if (/^https?:\/\/|^\//.test(input)) {
        return 'url';
    } else if (/^[a-z0-9_]+$/.test(input)) {
        return 'font';
    } else {
        return 'fallback';
    }
}
