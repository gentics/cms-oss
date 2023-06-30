import { Component, Input, OnInit } from '@angular/core';
import { DocBlock } from '../../common/docs';

@Component({
    selector: 'gtx-autodoc-table',
    templateUrl: './autodoc-table.component.html',
    styleUrls: ['./autodoc-table.component.scss'],
})
export class AutodocTableComponent implements OnInit {

    @Input() docBlocks: DocBlock[];
    identifierLabel = 'Name';
    headers: string[];
    props: string[];

    ngOnInit(): void {
        const firstBlock = this.docBlocks[0];
        if (firstBlock.decorator === 'Input') {
            // Inputs
            this.headers = ['Attribute', 'Type', 'Default Value', 'Comments'];
            this.props = ['identifier', 'type', 'defaultValue', 'body'];
        } else if (firstBlock.decorator === 'Output') {
            // Outputs
            this.headers = ['Event', 'Type', 'Default Value', 'Comments'];
            this.props = ['identifier', 'type', 'defaultValue', 'body'];
        } else if (firstBlock.methodArgs) {
            // Methods
            this.headers = ['Method', 'Args', 'Return Value', 'Comments'];
            this.props = ['identifier', 'methodArgs', 'type', 'body'];
        } else {
            // Properties
            this.headers = ['Property', 'Type', 'Default Value', 'Comments'];
            this.props = ['identifier', 'type', 'defaultValue', 'body'];
        }
    }

}
