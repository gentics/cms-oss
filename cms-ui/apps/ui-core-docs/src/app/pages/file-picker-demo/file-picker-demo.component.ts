import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './file-picker-demo.component.html',
    styleUrls: ['./file-picker-demo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class FilePickerDemoPage {

    @InjectDocumentation('file-picker.component')
    documentation: IDocumentation;

    isDisabled = false;
    isMultiple = true;
    onlyImages = false;
    selectedFiles: File[] = [];
    selectedFilesB: any;
    rejectedFilesB: any;
    selectedFilesC: any;
    rejectedFilesC: any;

    onFilesSelected(files: File[]): void {
        console.log('onFilesSelected: ' + JSON.stringify(files.map(f => ({ name: f.name, type: f.type }))));
        this.selectedFiles = [...files];
    }
}
