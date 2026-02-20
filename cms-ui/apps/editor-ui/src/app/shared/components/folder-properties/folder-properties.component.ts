import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnDestroy,
    OnInit
} from '@angular/core';
import {
    FormControl,
    FormGroup,
    Validators,
} from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { EditableFolderProps, Feature, Folder, GtxI18nProperty, Language } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ChangesOf, FormProperties, generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { BehaviorSubject, forkJoin, of, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, map, mergeMap, switchMap } from 'rxjs/operators';
import { ApplicationStateService } from '../../../state';

export enum FolderPropertiesMode {
    CREATE = 'create',
    EDIT = 'edit',
}

const ERROR_NAME_DUPLICATE = 'folderNameIsDuplicate';
const ERROR_DIRECTORY_DUPLICATE = 'folderDirectoryIsDuplicate';
const ERROR_DIRECTORY_PATTERN = 'pattern';

/** Allowed characters for input `directory` if "pub_dir_segment" is disabled */
const CHARS_ALLOWED_DEFAULT = [ '/', '_', '.', '-', '~' ];
/** Allowed characters for input `directory` if "pub_dir_segment" is enabled */
const CHARS_ALLOWED_PUB_DIR_SEGMENT = [ '_', '.', '-', '~' ];
/** Characters for the other CHARS constants which need escaping when placed into a regexp. */
const CHARS_REQUIRE_ESCAPE = new Set([ '\\', '/', '{', '}', '[', ']', '(', ')', '.', '^', '$', '*', '+', '-', '?', '!', '=' ]);

const CONTROLS_I18N: (keyof EditableFolderProps)[] = ['nameI18n', 'descriptionI18n', 'publishDirI18n'];
const CONTROLS: (keyof EditableFolderProps)[] = ['name', 'description', 'publishDir'];

@Component({
    selector: 'gtx-folder-properties',
    templateUrl: './folder-properties.component.html',
    styleUrls: ['./folder-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(FolderPropertiesComponent),
        generateValidatorProvider(FolderPropertiesComponent),
    ],
    standalone: false,
})
export class FolderPropertiesComponent
    extends BasePropertiesComponent<EditableFolderProps>
    implements OnChanges, OnInit, OnDestroy {

    public readonly FolderPropertiesMode = FolderPropertiesMode;
    public readonly ERROR_NAME_DUPLICATE = ERROR_NAME_DUPLICATE;
    public readonly ERROR_DIRECTORY_DUPLICATE = ERROR_DIRECTORY_DUPLICATE;
    public readonly ERROR_DIRECTORY_PATTERN = ERROR_DIRECTORY_PATTERN;

    @Input()
    public mode: FolderPropertiesMode = FolderPropertiesMode.EDIT;

    /** The currently being edited item id. */
    @Input()
    public itemId: number;

    /** The node-id in which the folder is being edited or created in. */
    @Input()
    public nodeId: number;

    /** The languages in which properties can be translated to. */
    @Input()
    public languages: Language[] = [];

    /** The Folder-ID in which this folder currently resides in or is to be created in. */
    @Input()
    public folderId: number;

    // node feature
    public pubDirSegmentEnabled = false;
    // global feature
    public folderAutocompleteEnabled = false;

    public allowedCharacters = '';

    public activeTabI18nLanguage: Language;

    /** The parent-folder based on `folderId` or on the active folder if not provided. */
    private parentFolder: Folder;
    /** Sibiling folders to check properties against. */
    private sibilingFolders: Folder[] = [];

    private pubDirPattern: RegExp;
    private parentFolderId$ = new BehaviorSubject<number>(null);

    private autocompleteSubscription: Subscription;

    constructor(
        changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
        private client: GCMSRestClientService,
    ) {
        super(changeDetector);
    }

    override ngOnInit(): void {
        this.updateAllowedCharacters();

        super.ngOnInit();

        this.activeTabI18nLanguage = this.languages?.[0];

        this.parentFolderId$.next(this.folderId);
        this.subscriptions.push(this.parentFolderId$.asObservable().pipe(
            switchMap(id => id ? of(id) : this.appState.select(state => state.folder.activeFolder)),
            distinctUntilChanged(isEqual),
            switchMap(id => forkJoin([
                this.client.folder.get(id),
                this.client.folder.folders(id),
            ])),
        ).subscribe(([loadRes, listRes]) => {
            this.parentFolder = loadRes.folder;
            this.sibilingFolders = listRes.folders;

            if (this.form) {
                this.form.updateValueAndValidity();
            }

            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.folder.activeNode).pipe(
            mergeMap(nodeId => this.appState.select(state => state.entities.node[nodeId])),
            filter(activeNode => !!activeNode),
            map(activeNode => !!activeNode.pubDirSegment),
        ).subscribe(enabled => {
            this.pubDirSegmentEnabled = enabled;
            this.updateAllowedCharacters();
            this.form.updateValueAndValidity();
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.features[Feature.AUTOCOMPLETE_FOLDER_PATH]).subscribe(enabled => {
            this.folderAutocompleteEnabled = enabled;
            this.autocompletePublishDirectory();
            this.changeDetector.markForCheck();
        }));
    }

    override ngOnChanges(changes: ChangesOf<this>): void {
        super.ngOnChanges(changes);

        if (changes.folderId) {
            this.parentFolderId$.next(this.folderId);
        }

        if (changes.languages && this.activeTabI18nLanguage == null) {
            this.activeTabI18nLanguage = this.languages?.[0];
        }
    }

    override ngOnDestroy(): void {
        super.ngOnDestroy();
        this.clearAutocomplete();
    }

    protected createForm(): FormGroup {
        const form = new FormGroup<FormProperties<EditableFolderProps>>({
            name: new FormControl(this.safeValue('name'), [Validators.required, (ctrl) => {
                return this.sibilingsHaveEqualProperty('name', ctrl.value) ? {
                    [ERROR_DIRECTORY_DUPLICATE]: true,
                } : null;
            }]),
            description: new FormControl(this.safeValue('description')),
            publishDir: new FormControl(this.safeValue('publishDir'), [Validators.required, (ctrl) => {
                const value = ctrl.value;
                let hasErr = false;
                const err = {};

                if (this.sibilingsHaveEqualProperty('name', value)) {
                    err[ERROR_NAME_DUPLICATE] = true;
                    hasErr = true;
                }

                if (!this.pubDirPattern.test(value)) {
                    err[ERROR_DIRECTORY_PATTERN] = true;
                    hasErr = true;
                }

                return hasErr ? err : null;
            }]),

            // I18n properties
            nameI18n: new FormControl(this.safeValue('nameI18n') || {}),
            descriptionI18n: new FormControl(this.safeValue('descriptionI18n') || {}),
            publishDirI18n: new FormControl(this.safeValue('publishDirI18n') || {}),
        });

        this.subscriptions.push(form.controls.name.valueChanges.pipe(
            debounceTime(50),
        ).subscribe(() => {
            this.autocompletePublishDirectory();
        }));

        // Mark this as dirty, as to not suggest file-names automatically
        if (this.value?.publishDir) {
            form.controls.publishDir.markAsDirty();
        }

        return form;
    }

    protected configureForm(_value: EditableFolderProps, loud?: boolean): void {
        const options = { onlySelf: loud, emitEvent: loud };
		setControlsEnabled(this.form, CONTROLS, !this.disabled || (this.mode === FolderPropertiesMode.CREATE), options);
		setControlsEnabled(this.form, CONTROLS_I18N, !this.disabled || (this.mode === FolderPropertiesMode.CREATE), options);
    }

    protected assembleValue(value: EditableFolderProps): EditableFolderProps {
        for (const key of CONTROLS_I18N) {
            if (!value[key]) {
                continue;
            }
            const propValue = value[key] as GtxI18nProperty;
            Object.entries(propValue).forEach(([languageCode, translatedValue]) => {
                if (!translatedValue) {
                    delete propValue[languageCode];
                }
            });
        }

        return value;
    }

    protected override onValueReset(): void {
        if (this.form) {
            this.form.updateValueAndValidity();
        }
    }

    protected sibilingsHaveEqualProperty<T extends keyof Folder>(property: T, value: Folder[T]): boolean {
        for (const sibling of this.sibilingFolders) {
            // Ignore our own folder, as otherwise it might be true all the time
            if (sibling.id === this.itemId) {
                continue;
            }
            if (isEqual(sibling[property], value)) {
                return true;
            }
        }
        return false;
    }

    protected updateAllowedCharacters(): void {
        const chars = this.pubDirSegmentEnabled ? CHARS_ALLOWED_PUB_DIR_SEGMENT : CHARS_ALLOWED_DEFAULT;
        this.allowedCharacters = chars.join(' ');
        const charsAllowedRegexp = chars
            .map(c => CHARS_REQUIRE_ESCAPE.has(c) ? `\\${c}` : c)
            .join('');
        this.pubDirPattern = new RegExp(`|^[A-Za-z0-9${charsAllowedRegexp}]+$`);
    }

    protected clearAutocomplete(): void {
        if (this.autocompleteSubscription != null) {
            this.autocompleteSubscription.unsubscribe();
            this.autocompleteSubscription = null;
        }
    }

    protected autocompletePublishDirectory(): void {
        this.clearAutocomplete();

        if (!this.form) {
            return;
        }

        const ctrl = this.form.controls.publishDir;

        if (
            !this.folderAutocompleteEnabled
            || ctrl.dirty
            || this.mode === FolderPropertiesMode.EDIT
        ) {
            return;
        }

        let publishDirProposal: string = null;
        const dirName = this.form.value.name || '';

        if (this.pubDirSegmentEnabled) {
            publishDirProposal = dirName
        } else if (typeof this.parentFolder?.publishDir === 'string') {
            const parentPubDir = this.parentFolder?.publishDir;
            publishDirProposal = `${parentPubDir.endsWith('/') ? parentPubDir : `${parentPubDir}/`}${dirName}`;
        }

        if (!publishDirProposal) {
            return;
        }

        const nodeId = this.nodeId || this.appState.now.folder.activeNode;
        this.autocompleteSubscription = this.client.folder.sanitizePublshDirectory({
            nodeId: nodeId,
            publishDir: publishDirProposal,
        }).subscribe(res => {
            if (ctrl.pristine) {
                ctrl.setValue(res.publishDir);
                this.changeDetector.markForCheck();
            }
        });
    }
}
