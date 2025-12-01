import { Component } from '@angular/core';

/**
 * An icon representing the start page of a folder.
 */
@Component({
    selector: 'start-page-icon',
    template: `
        <span class="startpage-icon"
              [title]="'editor.start_page_tooltip' | gtxI18n">
            <icon>home</icon>
        </span>
    `,
    styleUrls: ['./start-page-icon.scss'],
    standalone: false,
})
export class StartPageIcon {
}
