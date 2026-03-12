import { ChangeDetectionStrategy, Component, Input } from "@angular/core";
import { Language } from "@gentics/cms-models";
import { LanguageState, UIMode } from "../../../common/models";

@Component({
    selector: 'gtx-language-state',
    templateUrl: './language-state.component.html',
    styleUrls: ['./language-state.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class LanguageStateComponent {

    public readonly UIMode = UIMode;

    @Input({ required: true })
    public language: Language;

    @Input({ required: true })
    public activeLanguage: Language;

    @Input({ required: true })
    public state: LanguageState;

    @Input()
    public mode: UIMode = UIMode.EDIT;

    @Input()
    public showIndicators = false;

    @Input()
    public showDeleted = false;
}
