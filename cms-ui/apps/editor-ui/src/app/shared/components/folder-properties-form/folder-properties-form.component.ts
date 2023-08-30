import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
} from '@angular/core';
import { AbstractControl, AsyncValidatorFn, UntypedFormControl, UntypedFormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MarkObjectPropertiesAsModifiedAction } from '@editor-ui/app/state';
import { EditableFolderProps, Folder, GtxI18nProperty, Language, Raw } from '@gentics/cms-models';
import { pick } from'lodash-es'
import { BehaviorSubject, Observable, Subscription, combineLatest, of } from 'rxjs';
import { debounceTime, filter, map, mergeMap, switchMap, take, tap } from 'rxjs/operators';
import { deepEqual } from '../../../common/utils/deep-equal';
import { Api } from '../../../core/providers/api/api.service';
import { ApplicationStateService } from '../../../state';

@Component({
    selector: 'folder-properties-form',
    templateUrl: './folder-properties-form.tpl.html',
    styleUrls: ['./folder-properties-form.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    })
export class FolderPropertiesForm implements AfterViewInit, OnChanges, OnDestroy, OnInit {

    @Input()
    nodeId: number;

    @Input()
    folderId: number;

    @Input()
    properties: EditableFolderProps = {};

    @Input()
    disabled = false;

    @Input()
    mode: 'create' | 'edit' = 'edit';

    @Input()
    languages: Language[];

    @Output()
    changes = new EventEmitter<EditableFolderProps>();

    // node feature
    featureEnabledPubDirSegment = false;
    // global feature
    featureEnabledFolderAutocomplete = false;

    form: UntypedFormGroup;
    /** If component used as existing folder editor, store own values to skip on 'is already used' check */
    ownValues: any = {};
    /** If CMS feature "pub_dir_segment" is enabled */
    featurePubDirSegmentIsActive$: Observable<boolean>;
    /** Allowed characters for input `directory` if "pub_dir_segment" is disabled */
    charsAllowedFeaturePubDirSegmentIsNotActive = [ '/', '_', '.', '-', '~' ];
    /** Allowed characters for input `directory` if "pub_dir_segment" is enabled */
    charsAllowedFeaturePubDirSegmentIsActive = [ '_', '.', '-', '~' ];

    changeSubs: Subscription[] = [];

    activeTabI18nLanguage: Language;

    private activeFolder$ = new BehaviorSubject<Folder<Raw>>(null);
    private activeFolderPublishDir: string = null;
    /** children folder of active folder */
    private childFoldersOfActiveFolder$ = new BehaviorSubject<Folder<Raw>[]>([]);

    constructor(
        private appState: ApplicationStateService,
        private api: Api,
        private changeDetectorRef: ChangeDetectorRef,
    ) { }

    ngOnInit(): void {

        this.updateFolderId();

        this.initFolderPropertiesForm();

        this.featurePubDirSegmentIsActive$ = this.appState.select(state => state.folder.activeNode).pipe(
            mergeMap(nodeId => this.appState.select(state => state.entities.node[nodeId])),
            filter(activeNode => !!activeNode),
            map(activeNode => !!activeNode.pubDirSegment),
            tap((isEnabled: boolean) => {
                this.featureEnabledPubDirSegment = isEnabled;
                this.updateFormValidators(isEnabled);
            }),
        );

        this.changeSubs.push(
            combineLatest([
                this.featurePubDirSegmentIsActive$,
                this.appState.select(state => state.features.autocomplete_folder_path).pipe(
                    tap((isEnabled: boolean) => this.featureEnabledFolderAutocomplete = isEnabled),
                ),
            ]).pipe(
                filter(([featureEnabledPubDirSegmentEnabled, featureEnabledFolderAutocompleteEnabled]: [boolean, boolean]) => {
                    // we only continue after the state of both features has been checked.
                    // autocompletion depends on knowing whether it as well as whether pub dir segment are enabled
                    return featureEnabledFolderAutocompleteEnabled && this.mode === 'create';
                }),
                switchMap(_ => this.activeFolder$), // can be improved by only fetching active folder if pub dir segment is disabled
                filter((folder: Folder<Raw>) => !!folder),
                tap((folder: Folder<Raw>) => this.activeFolderPublishDir = folder.publishDir),
            ).subscribe((_: Folder<Raw>) => {
                this.suggestFolderDirectoryName(this.form.get('name').value);
            }),
        );
    }

    ngAfterViewInit(): void {
        if (!this.isModeCreate() && Array.isArray(this.languages) && this.languages.length > 0) {
            setTimeout(() => {
                this.setActiveI18nTab(this.languages[0].id);
                this.changeDetectorRef.markForCheck();
            }, 0);
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['properties']) {
            this.updateFormValues(this.properties);
        }

        if (changes['folderId']) {
            this.updateFolderId();
        }
    }

    ngOnDestroy(): void {
        if (this.changeSubs) {
            this.changeSubs.forEach(s => {
                if (s) {
                    s.unsubscribe();
                }
            });
        }
    }

    isMultiLang(): boolean {
        return Array.isArray(this.languages) && this.languages.length > 1;
    }

    isModeCreate(): boolean {
        return this.mode === 'create';
    }

    hasError(formControlname: string, errorKey: string): ValidationErrors {
        if (!this.form) {
            return;
        }
        const errors = this.form.get(formControlname).errors;
        return errors ? errors[errorKey] ? errors[errorKey] : null : null;
    }

    getCharsAllowedDirectoryReadable(featurePubDirSegmentIsActive: boolean): string {
        const charsAllowed = featurePubDirSegmentIsActive
            ? this.charsAllowedFeaturePubDirSegmentIsActive
            : this.charsAllowedFeaturePubDirSegmentIsNotActive;
        return charsAllowed.join(' ');
    }

    asyncValidatorFolderNameIsDuplicate(control: AbstractControl): Observable<ValidationErrors | null> {
        const folderName = control.value;
        if (!folderName) {
            return of(null);
        } else {
            return this.folderPropertyIsDuplicate$('name', 'name', folderName).pipe(
                // Observable must complete
                take(1),
                map(isDuplicate => isDuplicate ? { folderNameIsDuplicate: true } : null),
            );
        }
    }

    asyncValidatorFolderDirectoryIsDuplicate(control: AbstractControl): Observable<ValidationErrors | null> {
        const folderDirectory = control.value;
        if (!folderDirectory) {
            return of(null);
        } else {
            return this.folderPropertyIsDuplicate$('publishDir', 'directory', folderDirectory).pipe(
                // Observable must complete
                take(1),
                map(isDuplicate => isDuplicate ? { folderDirectoryIsDuplicate: true } : null),
            );
        }
    }

    updateForm(properties: EditableFolderProps): void {
        if (!this.form) {
            return;
        }
        this.form.get('name').setValue(properties.name, { emitEvent: false });
        this.form.get('directory').setValue(properties.directory, { emitEvent: false });
        this.form.get('description').setValue(properties.description, { emitEvent: false });
        this.form.get('nameI18n').setValue(properties.nameI18n, { emitEvent: false });
        this.form.get('publishDirI18n').setValue(properties.publishDirI18n, { emitEvent: false });
        this.form.get('descriptionI18n').setValue(properties.descriptionI18n, { emitEvent: false });
    }

    setActiveI18nTab(languageId: number): void {
        this.activeTabI18nLanguage = this.languages.find(l => l.id === languageId);
    }

    activeI18nTabValueExists(languageCode: string): boolean {
        return [
            this.form.get('nameI18n').value as GtxI18nProperty,
            this.form.get('publishDirI18n').value as GtxI18nProperty,
            this.form.get('descriptionI18n').value as GtxI18nProperty,
        ].some(data => !!data[languageCode]);
    }

    private updateFolderId(): void {
        // if no folderId provided, fallback to state
        const folderId = this.folderId || this.appState.now.folder.activeFolder;
        // fetch folder data
        this.api.folders.getItem(folderId, 'folder').pipe(
            map(response => response.folder),
        ).toPromise()
            .then((activeFolder: Folder<Raw>) => this.activeFolder$.next(activeFolder));
        // fetch child folders
        this.api.folders.getFolders(folderId).pipe(
            map(response => response.folders),
            filter(folders => Array.isArray(folders)),
        ).toPromise()
            .then((childFoldersOfActiveFolder: Folder<Raw>[]) => this.childFoldersOfActiveFolder$.next(childFoldersOfActiveFolder));
    }

    private folderPropertyIsDuplicate$(entityKey: string, ownKey: string, checkValue: string): Observable<boolean> {
        const areEqual = (a: string, b: string): boolean => a.toLowerCase() === b.toLowerCase();
        return this.childFoldersOfActiveFolder$.pipe(
            map(childFoldersOfActiveFolder => childFoldersOfActiveFolder.find(folder => {
                const entityValue = folder[entityKey];
                // if own value, ignore own value
                if (this.ownValues[ownKey] && areEqual(this.ownValues[ownKey], entityValue)) {
                    return false;
                } else {
                    // compare with folder
                    return typeof entityValue === 'string' && areEqual(entityValue, checkValue);
                }
            }) ? true : false,
            ),
        );
    }

    private initFolderPropertiesForm(): void {
        // if no nodeId provided, fallback to state
        const activeNodeId = this.nodeId || this.appState.now.folder.activeNode;
        const activeNode = this.appState.now.entities.node[activeNodeId];
        const featurePubDirSegmentIsActive = activeNode && activeNode.pubDirSegment;
        /** validators for input 'name' */
        const inputNameValidators: ValidatorFn[] = [
            Validators.required,
        ];
        const inputNameAsyncValidators: AsyncValidatorFn[] = [
            this.asyncValidatorFolderNameIsDuplicate.bind(this),
        ];

        // initialize form
        const formObj: { [key in keyof EditableFolderProps]: UntypedFormControl } = {
            name: new UntypedFormControl(this.properties.name || '', inputNameValidators, inputNameAsyncValidators),
            directory: new UntypedFormControl(
                this.properties.directory || '',
                this.getValidatorsInputDirectory(featurePubDirSegmentIsActive),
                this.getAsyncValidatorsInputDirectory(featurePubDirSegmentIsActive),
            ),
            description: new UntypedFormControl(this.properties.description || ''),
        };
        if (!this.isModeCreate()) {
            formObj.nameI18n = new UntypedFormControl({ ...this.properties.nameI18n });
            formObj.publishDirI18n = new UntypedFormControl({ ...this.properties.publishDirI18n });
            formObj.descriptionI18n = new UntypedFormControl({ ...this.properties.descriptionI18n });
        }
        this.form = new UntypedFormGroup(formObj);
        this.changeSubs.push(this.form.valueChanges.subscribe(changes => {
            changes = this.sanitizeFolderData(changes);

            // notify state about entity properties validity -> relevant for `ContentFrame.modifiedObjectPropertyValid`
            const isModified = !deepEqual(this.properties, changes);
            this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(isModified, this.form.valid));

            this.changes.emit(changes);
        }));
        // Subscribe to changes for folder path synchronization
        this.changeSubs.push(this.form.get('name').valueChanges.pipe(
            debounceTime(400),
        ).subscribe(folderName => {
            this.suggestFolderDirectoryName(folderName);
        }));
    }

    private deepStringify(data: EditableFolderProps): string {
        const stringifySort = (d: Record<any, any>): string => {
            if (d && Object.keys(d).length > 0) {
                return JSON.stringify(d, Object.keys(d).sort());
            } else {
                return JSON.stringify(d);
            }
        };
        return stringifySort(data)
            + stringifySort(data.nameI18n)
            + stringifySort(data.publishDirI18n)
            + stringifySort(data.descriptionI18n);
    }

    /**
     * @param data to be sanitized.
     * @returns sanitized data.
     * @description If sent to backend, empty string will be interpreted as existing value which would lead to unintended behavior.
     * As values of I18n-properties of null or undefined will be correctly interpreted as not set, we need to ensure empty strings to be null.
     */
    private sanitizeFolderData(data: EditableFolderProps): EditableFolderProps {
        Object.values(data).forEach(v => {
            if (v instanceof Object) {
                const i18nProperty = v as GtxI18nProperty;
                Object.entries(i18nProperty).forEach(([languageCode, vI18n]) => {
                    if (!vI18n) {
                        delete i18nProperty[languageCode];
                    }
                });
            }
        });
        return data;
    }

    private updateFormValues(properties: EditableFolderProps): void {
        // store
        this.ownValues.name = properties.name;
        this.ownValues.directory = properties.directory;

        if (!this.form) {
            return;
        }
        // set form input values
        this.form.patchValue(pick(properties, [
            'name',
            'directory',
            'description',
            'nameI18n',
            'publishDirI18n',
            'descriptionI18n',
        ]), { emitEvent: false });
    }

    private updateFormValidators(featurePubDirSegmentIsActive: boolean): void {
        if (!this.form) {
            return;
        }
        this.form.get('directory').setValidators( this.getValidatorsInputDirectory(featurePubDirSegmentIsActive) );
        this.form.get('directory').setAsyncValidators( this.getAsyncValidatorsInputDirectory(featurePubDirSegmentIsActive) );

        this.form.updateValueAndValidity();
    }

    private getValidatorsInputDirectory(featurePubDirSegmentIsActive: boolean): ValidatorFn[] {
        return [
            Validators.required,
            Validators.pattern(this.getPatternInputDirectory(featurePubDirSegmentIsActive)),
        ];
    }

    private getAsyncValidatorsInputDirectory(featurePubDirSegmentIsActive: boolean): AsyncValidatorFn[] {
        const validatorsInputDirectory: AsyncValidatorFn[] = [];
        // if CR Mesh OR currentNode.pubDirSegment
        // and if any other folder in active folder has same directory property (`publishDir`)
        // TODO: ask Norbert Pomaroli if checking for both would affect existing custom customer implementations (e. g. client CinePlexx)
        if (featurePubDirSegmentIsActive) {
            validatorsInputDirectory.push(this.asyncValidatorFolderDirectoryIsDuplicate.bind(this));
        }
        return validatorsInputDirectory;
    }

    private getPatternInputDirectory(featurePubDirSegmentIsActive: boolean): string {
        const charsAllowed = featurePubDirSegmentIsActive
            ? this.charsAllowedFeaturePubDirSegmentIsActive
            : this.charsAllowedFeaturePubDirSegmentIsNotActive;
        const charsAllowedRegexp = this.escapeChars(charsAllowed).join('');
        return `[A-Za-z0-9${charsAllowedRegexp}]+`;
    }

    private escapeChars(chars: string[]): string[] {
        const charsNeedEscaping = [ '\\', '/', '{', '}', '[', ']', '(', ')', '.', '^', '$', '*', '+', '-', '?', '!', '=' ];
        return chars.map(char => charsNeedEscaping.includes(char) ? '\\' + char : char);
    }

    private getFolderDirectoryControl(): AbstractControl {
        return this.form.get('directory');
    }

    // append folder name to path and clean folder name from unwanted chars
    private suggestFolderDirectoryName(fname: string): void {
        if (!this.featureEnabledFolderAutocomplete || this.getFolderDirectoryControl().dirty || this.mode === 'edit') {
            return;
        }
        let publishDirProposal: string = null;
        if (this.featureEnabledPubDirSegment) {
            publishDirProposal = fname;
        } else if (typeof this.activeFolderPublishDir === 'string') {
            publishDirProposal = `${this.activeFolderPublishDir.endsWith('/') ? this.activeFolderPublishDir : `${this.activeFolderPublishDir}/`}${fname}`;
        }
        if (publishDirProposal !== null) {
            this.changeSubs.push(this.api.folders.sanitizeFolderPath({
                nodeId: this.appState.now.folder.activeNode,
                publishDir: publishDirProposal,
            }).pipe(
                map(response => response.publishDir),
            ).subscribe((publishDir: string) => {
                if (!this.getFolderDirectoryControl().dirty) {
                    this.getFolderDirectoryControl().patchValue(publishDir);
                }
            }));
        }
    }

}
