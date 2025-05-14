import { AfterViewInit, Component, ElementRef, Input, OnInit } from '@angular/core';
import hljs from 'highlight.js/lib/core';

/**
 * Code highlighting via highlight.js. To include interpolated vars in the code, use double
 * parens so that Angular does not think the whole thing is a binding. Double parens will get
 * replaced with double curlies in the final output.
 *
 * ```
 * <gtx-highlighted-code language="HTML" code='
 *      My item is (( item ))
 * '></gtx-highlighted-code>
 * ```
 *
 * The above will output "My item is {{ item }}"
 */
@Component({
    selector: 'gtx-highlighted-code',
    template: '<pre><code [ngClass]="language">{{ formattedCode }}</code></pre>',
    standalone: false
})
export class HighlightedCodeComponent implements OnInit, AfterViewInit {

    @Input() language: string;
    @Input() code: string;

    formattedCode: string;

    constructor(private elementRef: ElementRef<HTMLElement>) { }

    ngOnInit(): void {
        this.formattedCode = this.formatCodeContents(this.code);
        this.formattedCode = this.replaceDoubleCurlies(this.formattedCode);
    }

    ngAfterViewInit(): void {
        const codeEl = this.elementRef.nativeElement.querySelector('code');
        hljs.highlightBlock(codeEl);
    }

    /**
     * Remove extra whitespace from start and end, and automatically detect the
     * extra indentation and remove it from each line.
     */
    private formatCodeContents(contents: string): string {
        // Remove "ind" characters on whitespace from the start of the line.
        const removeIndentation = (line: string, ind: number) => {
            if (line.substring(0, ind).trim() === '') {
                return line.substring(ind, line.length);
            } else {
                return line;
            }
        };

        const lines: string[] = contents
            .replace(/^(\s*[\r\n]+)+|([\r\n]+\s*)+$/g, '')
            .split(/\r?\n|\r/);
        const indentation: number = (/^\s*/.exec(lines[0]))[0].length;

        return lines.map((line: string) => removeIndentation(line, indentation)).join('\n');
    }

    /**
     * Find any interpolations with `(( var ))` and replace the parens with curlies
     * so it matches the Angular syntax.
     */
    private replaceDoubleCurlies(source: string): string {
        return source.replace(/\(\(\s*(.+?)\s*\)\)/g, '{{ $1 }}');
    }

}
