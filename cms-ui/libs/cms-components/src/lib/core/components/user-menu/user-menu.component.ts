import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output,
} from '@angular/core';
import { GcmsUiLanguage } from '@gentics/cms-integration-api-models';
import { I18nLanguage, Normalized, User } from '@gentics/cms-models';

/**
 * The right-hand side menu for user information and settings.
 * Optionally you can provide this with tabs using the Gentics UI Core tabs/tab components, which
 * must then be noted with the content projection selector `top` in order to preserve styling.
 * All documented properties of gentics-ui-core/gtx-tabs apply.
 * Also optionally, additional elements can be inserted in content projection selectors `top` and `bottom`.
 * @example
 * ```html
 * <gtx-user-menu>
 *       <gtx-tabs
 *           top
 *           [activeId]="userMenuActiveTab"
 *           hideTitle
 *       >
 *           <gtx-tab
 *               [id]="userMenuTabIdMessages"
 *               icon="mail"
 *               [title]="'dashboard.messages_title' | i18n"
 *           >
 *               <gtx-message-inbox>
 *               </gtx-message-inbox>
 *           </gtx-tab>
 *
 *           <gtx-tab
 *               [id]="userMenuTabIdActivities"
 *               icon="autorenew"
 *               [title]="'dashboard.activities_title' | i18n"
 *           >
 *               <gtx-activity-manager>
 *               </gtx-activity-manager>
 *           </gtx-tab>
 *       </gtx-tabs>
 *
 *       <gtx-app-version-label
 *           bottom
 *           [versionData]="cmpVersion$ | async"
 *           [uiVersion]="uiVersion$ | async"
 *       ></gtx-app-version-label>
 * </gtx-user-menu>
 * ```
 */
@Component({
    selector: 'gtx-user-menu',
    templateUrl: './user-menu.tpl.html',
    styleUrls: ['./user-menu.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class GtxUserMenuComponent {
    /** If TRUE, this user menu is rendered as open. */
    @Input() opened = false;
    /** The current user whose name is dispalyed. */
    @Input() user: User<Normalized>;
    /** All languages available to choose from in language switcher. */
    @Input() supportedLanguages: I18nLanguage[] = [];
    /** The language currently selected */
    @Input() currentlanguage: GcmsUiLanguage;
    /** On event user clicks logout button. */
    @Output() logout = new EventEmitter<void>();
    /** On event user clicks setLanguage button. */
    @Output() setLanguage = new EventEmitter<GcmsUiLanguage>();
    /** On event user clicks user-menu button in upper right screen corner. */
    @Output() toggle = new EventEmitter<boolean>();
    /** On event user clicks showPasswordModal button. */
    @Output() showPasswordModal = new EventEmitter<void>();

    getUserName(): string {
        return this.user ? this.user.firstName + ' ' + this.user.lastName : '';
    }

    setLanguageClicked(code: GcmsUiLanguage): void {
        // Do nothing if the same language has been clicked again
        if (code === this.currentlanguage) {
            return;
        }
        this.setLanguage.emit(code);
    }

    showPasswordModalClicked(): void {
        this.showPasswordModal.emit();
    }
}
