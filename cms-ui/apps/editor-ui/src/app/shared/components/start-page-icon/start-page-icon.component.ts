import {Component} from '@angular/core';

/**
 * An icon representing the start page of a folder.
 */
@Component({
    selector: 'start-page-icon',
    template: `
        <span class="startpage-icon"
              [title]="'editor.start_page_tooltip' | i18n">
            <i class="material-icons" [attr.translate]="'no'">home</i>
        </span>
    `,
    styleUrls: ['./start-page-icon.scss']
})
export class StartPageIcon {
}
