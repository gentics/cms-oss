import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './search-bar-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class SearchBarDemoPage {

    @InjectDocumentation('search-bar.component')
    documentation: IDocumentation;

    changeCount = 0;
    searchCount = 0;
    clearCount = 0;

    term = 'search term';
    hideClearButton = false;
}
