import { Component, ElementRef, Input, OnInit } from '@angular/core';
import { IDocumentation } from '../../common/docs';

/**
 * Accepts a string of the component's source code, parses it and renders the
 * resulting documentation.
 */
@Component({
    selector: 'gtx-autodocs',
    templateUrl: './autodocs.component.html',
    styleUrls: ['./autodocs.component.scss'],
    standalone: false
})
export class AutodocsComponent implements OnInit {

    @Input()
    docs: IDocumentation;

    constructor(private elementRef: ElementRef<HTMLElement>) {}

    ngOnInit(): void {
        setTimeout(() => this.addClassToElements('pre>code', 'hljs'));
    }

    private addClassToElements(selector: string, className: string): void {
        const elements = this.elementRef.nativeElement.querySelectorAll(selector);
        elements.forEach(el => el.classList.add(className));
    }
}
