import React, { useEffect, useMemo, useRef, useState } from 'react';
import { View, TouchableOpacity, Platform, Linking } from 'react-native';
import {
  FormState,
  IFormGridOptions,
  IFormComponentAttributes,
  IDataFieldProperties,
  SummaryOptions,
  IBinary,
  CommonValidation,
  IBinaryFile,
  BinarySource,
} from '../../../../types/interfaces';
import { useTranslation } from 'react-i18next';
import { binaryElementStyles } from '../../../../styles/common/common-style';
import { binaryElementStyles as customBinaryElementStyles } from '../../../../styles/custom/custom-style';
import { TFunction } from 'i18next';
import * as validationUtils from '../../../../utils/ValidationUtils';
import * as formEventsUtils from '../../../../utils/FormEventsUtils';
import * as utils from '../../../../utils/Utils';
import { SummaryElement } from '../../helper/SummaryElement';
import { FormElementWrapper } from '../../helper/FormElementWrapper';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { RestService } from '../../../../services/RestService';
import FileCard from './parts/FileCard/FileCard.web';
import * as DocumentPicker from 'expo-document-picker';
import { DocumentPickerAsset } from 'expo-document-picker';
import { v4 as uuidv4 } from 'uuid';
import { IOverlayFullScreenTextEnum } from '../../../../constants/enums/IOverlayFullScreenTextEnum';
import { OverlayTypeEnum } from '../../../../custom/OverlayTypes';
import * as overlayUtils from '../../../../utils/OverlayUtils';


/**
 * In-memory per-field cache to keep local (unuploaded) files across unmount/remount
 * cycles (e.g., responsive layout switches). Keyed by formid+jsonpath+id.
 * Files are kept only for the life of the browser tab (no disk persistence).
 */
type BinaryFileWithUid = IBinaryFile & { uid: string };

const binaryLocalCache: Map<string, BinaryFileWithUid[]> = new Map();



function BinaryElement(props: IFormComponentAttributes) {
  const { t } = useTranslation();
  const [files, setFiles] = useState<BinaryFileWithUid[]>([]);
  const labelText =
    utils.getI18nText(props.formState, props.label) +
    (props.required ? ' *' : '');
  const errorMessageElementId = `${props.id}_errorMessage`;
  const [localErrMsg, setLocalErrMsg] = useState(props.error);

  const styles = useMemo(
    () => mergeStyles(binaryElementStyles, customBinaryElementStyles),
    [],
  );

  const dropRef = useRef<View>(null);
  const elementRef = useRef<View>(null);
  const [isDragOver, setIsDragOver] = useState(false);

  // Keep latest props for window event handlers to avoid stale closures
  const latestRef = useRef({
    formState: props.formState,
    uiState: props.uiState,
    jsonpath: props.jsonpath,
    id: props.id,
    updateState: props.updateState,
    restService: props.restService,
    isLoggedIn: props.isLoggedIn,
    formGridOptions: props.formGridOptions,
    editable: props.editable,
    isList: props.isList,
  });

  useEffect(() => {
    latestRef.current = {
      formState: props.formState,
      uiState: props.uiState,
      jsonpath: props.jsonpath,
      id: props.id,
      updateState: props.updateState,
      restService: props.restService,
      isLoggedIn: props.isLoggedIn,
      formGridOptions: props.formGridOptions,
      editable: props.editable,
      isList: props.isList,
    };
  }, [
    props.formState,
    props.uiState,
    props.jsonpath,
    props.id,
    props.updateState,
    props.restService,
    props.isLoggedIn,
    props.formGridOptions,
    props.editable,
    props.isList,
  ]);


  const cacheKey = useMemo(() => {
    const formid = props.formState?.formid ?? 'noform';
    const jsonpath = String(props.jsonpath ?? '');
    const id = String(props.id ?? '');
    return `${formid}::${jsonpath}::${id}`;
  }, [props.formState?.formid, props.jsonpath, props.id]);
  // Load files from cache on mount or cacheKey change
  useEffect(() => {
    const cached = binaryLocalCache.get(cacheKey);
    if (cached && cached.length && files.length === 0) {
      setFiles(cached);
    }
  }, [cacheKey]); // intentionally not depending on `files` to avoid overriding active changes

  // Save files to cache when files or cacheKey changes
  useEffect(() => {
    binaryLocalCache.set(cacheKey, files);
  }, [cacheKey, files]);


  // Sync local error
  useEffect(() => {
    setLocalErrMsg(props.error);
  }, [props.error]);

  // Accessibility focus ref registration
  useEffect(() => {
    props.updateFirstErrorElementRef &&
      props.updateFirstErrorElementRef(props, elementRef);
  });

  // Helper: normalize form value into IBinary[]
  function normalizeFiles(value: any): IBinary[] {
    if (!value) return [];
    if (Array.isArray(value)) return value.filter((v) => !!v?.fileName);
    if (typeof value === 'object' && value.fileName) return [value];
    return [];
  }

  // --- V2 helpers -------------------------------------------------------------
  const readExt = (name: string) =>
    name?.split('.').pop()?.toLowerCase() ?? '';

  const classifySource = (b: IBinary): BinarySource => {
    if (!b?.value) return BinarySource.LOCAL;
    if (String(b.value).startsWith('FormInstanceContent|')) return BinarySource.ACTANOVA;
    if (String(b.value).startsWith('mesh(not uploaded yet)')) return BinarySource.LOCAL;
    return BinarySource.LOCAL;
  };

  const makeBinaryFromFile = (file: File): IBinary => ({
    fileName: file.name,
    type: readExt(file.name),
    uri: URL.createObjectURL(file),
    value: '',
  });

  const toIBinaryFileFromServer = (formFile: IBinary): BinaryFileWithUid => {
    const fileType = readExt(formFile.fileName || '');
    const source = classifySource(formFile);
    const withUri: IBinary = {
      ...formFile,
      uri:
        props.formState?.formid && formFile.value
          ? props.restService.ANGetBinaryURL(
            props.formState.formid,
            formFile.value,
            props.formState.businessObjectID,
            350,
            200
          )
          : formFile.uri,
      type: fileType,
    };
    return {
      binary: withUri,
      file: null,
      source,
      fileType,
      uid: uuidv4(),
    };
  };

  // Stable key for list items (server vs local) based on a random UID
  const getFileKey = (it: BinaryFileWithUid): string => {
    return it.uid;
  };


  useEffect(() => {
    if (Platform.OS !== 'web') return;

    // RN Web >= 0.20 removed findNodeHandle; refs on web resolve to the host element
    const node = dropRef.current as unknown as HTMLElement | null;
    if (!node) return;

    const rect = () => node.getBoundingClientRect();
    const inside = (e: DragEvent) => {
      const r = rect();
      const { clientX: x, clientY: y } = e;
      return x >= r.left && x <= r.right && y >= r.top && y <= r.bottom;
    };

    const onDragOver = (e: DragEvent) => {
      if (inside(e)) {
        e.preventDefault();
        setIsDragOver(true);
        e.dataTransfer && (e.dataTransfer.dropEffect = 'copy');
      } else {
        setIsDragOver(false);
      }
    };

    const onDrop = async (e: DragEvent) => {
      const wasInside = inside(e);
      setIsDragOver(false);
      const current = latestRef.current;
      if (!wasInside || !current.editable) return;

      e.preventDefault();
      const dropped = Array.from(e.dataTransfer?.files ?? []);
      if (!dropped.length) return;

      // In single-file mode, only keep the latest file and replace previous selection
      const files = current.isList ? dropped : [dropped[0]];

      formEventsUtils.handleBinaryAdd(
        current.formState,
        current.uiState,
        current.jsonpath,
        current.id,
        current.updateState,
        current.restService,
        files,
        current.isLoggedIn,
        current.formGridOptions,
        current.editable,
      );

      const additions: BinaryFileWithUid[] = files.map((f) => ({
        binary: makeBinaryFromFile(f),
        file: f,
        source: BinarySource.LOCAL,
        fileType: readExt(f.name),
        uid: uuidv4(),
      }));

      // Append in list mode, replace in single-file mode
      setFiles((prev) => (current.isList ? [...prev, ...additions] : additions));
    };

    const onDragLeave = () => setIsDragOver(false);
    window.addEventListener('dragover', onDragOver);
    window.addEventListener('drop', onDrop);
    window.addEventListener('dragleave', onDragLeave);

    return () => {
      window.removeEventListener('dragover', onDragOver);
      window.removeEventListener('drop', onDrop);
      window.removeEventListener('dragleave', onDragLeave);
    };
  }, []);


  // Reconcile props.value (server) with local state:
  // - Convert server IBinary[] to IBinaryFile[]
  // - Remove any local entries that have the same fileName as a server entry
  // - Keep remaining locals (still uploading or not yet persisted)
  useEffect(() => {
    if (!props.formState?.formid) return;

    const serverListRaw = normalizeFiles(props.value).filter(
      (f) =>
        !!f?.fileName &&
        !!f?.value &&
        !String(f.value).startsWith('mesh(not uploaded yet)'),
    );

    const serverList: BinaryFileWithUid[] = serverListRaw.map(toIBinaryFileFromServer);

    setFiles((prev) => {
      const serverNames = new Set(serverList.map((s) => s.binary.fileName));
      const remainingLocals = prev.filter(
        (x) => x.source === BinarySource.LOCAL && !serverNames.has(x.binary.fileName),
      );
      return [...serverList, ...remainingLocals];
    });
  }, [props.value, props.formState?.formid]);

  // In summary mode, reuse existing summary logic
  if (props.summary > 0) {
    let summaryValue = BinaryElement.getSummaryValue(
      props.formState,
      props.jsonpath,
      props.id,
      t,
      undefined,
      props.restService,
    );
    return (
      <SummaryElement
        t={props.t}
        initUrl={props.restService.initUrl}
        formState={props.formState}
        uiState={props.uiState}
        summaryOption={SummaryOptions.SummaryText}
        label={props.label}
        value={summaryValue as any}
        dangerouslySetInnerHTML={true}
      />
    );
  }

  const renderItems = files.map((item, index) => {
    const key = getFileKey(item);
    let binaryLink = "";
    if (item.source === BinarySource.LOCAL) {
      binaryLink = item.binary.uri || "";
    } else if (item.source === BinarySource.ACTANOVA) {
      binaryLink = props.restService.ANGetBinaryURL(
        props.formState.formid,
        item.binary.value,
        props.formState.businessObjectID,
      );
    }
    return (
      <FileCard
        index={index}
        key={key}
        t={props.t}
        file={item}
        onLink={() => {
          console.log(binaryLink);
          Linking.openURL(binaryLink);
        }}
        showDownload={(binaryLink !== '')}
        onRemove={async () => {
          await formEventsUtils.handleBinaryDelete(
            props.formState,
            props.uiState,
            props.restService,
            props.updateState,
            props.jsonpath,
            props.id,
            index,
            item.binary.fileName,
            props.isLoggedIn,
            props.formGridOptions,
            props.editable
          );
          // Immediately reflect removal in local UI state (covers local files that server doesn't know about yet)
          setFiles(prev => prev.filter(f => getFileKey(f) !== key));
        } } 
        parentJsonPath={props.jsonpath}
      />
    );
  });

  return (
    <FormElementWrapper
      formState={props.formState}
      formComponentAttributes={props}
      uiState={props.uiState}
      numberOfColumns={props.formGridOptions?.numberOfColumns}
      description={props.description}
      additionalClass="field-file"
      localErrorMsg={localErrMsg}
      jsonPath={props.jsonpath}
      objectId={props.id}
      selectedElementId={props.selectedElementId}
    >
      <View ref={dropRef}>
        <View ref={elementRef}>
          <TouchableOpacity
            accessible
            accessibilityRole="button"
            accessibilityLabel={t('form_accessibility_binary_file_chooser', {
              label: labelText,
            })}
            accessibilityHint={
              props.error
                ? t('form_accessibility_binary_file_chooser_error_hint', {
                  error: props.error,
                })
                : t('form_accessibility_binary_file_chooser_hint')
            }
            accessibilityState={{ disabled: !props.editable }}
            accessibilityLiveRegion="polite"
            importantForAccessibility="yes"
            {...(Platform.OS === 'web'
              ? {
                'aria-invalid': !!props.error,
                'aria-haspopup': 'false',
              }
              : {})}
            style={[
              styles.fileChooser,
              isDragOver && styles.dragOverFileChooser,
              !props.editable && styles.disabledFileChooser,
              props.error && styles.errorFileChooser,
              files.length > 0 && styles.fileChooserWithFiles,
            ]}
            onPress={async () => {
              overlayUtils.setOverlay(
                props.uiState,
                props.formState,
                OverlayTypeEnum.FULL_SCREEN_LOADING,
                props.updateState,
                { loadingText: IOverlayFullScreenTextEnum.FileUploadClientProcess },
              );
              const newFiles = await onBinaryPress(props);
              overlayUtils.hideOverlay(props.uiState, props.updateState);
              if (newFiles && newFiles.length > 0) {
                setFiles((prev) => {
                  const additions: BinaryFileWithUid[] = [];
                  for (const f of newFiles) {
                    additions.push({
                      binary: makeBinaryFromFile(f),
                      file: f,
                      source: BinarySource.LOCAL,
                      fileType: readExt(f.name),
                      uid: uuidv4(),
                    });
                  }
                  // Single-file upload: replace previous selection
                  if (!props.isList) {
                    return additions.slice(0, 1);
                  }
                  return [...prev, ...additions];
                });
              }
            }}
            disabled={!props.editable}
          >
            <AndpText
              style={[
                styles.fileChooserLabel,
                !props.editable && styles.fileChooserLabelDisabled,
                props.error && styles.fileChooserLabelError,
              ]}
            >
              {labelText}
            </AndpText>
          </TouchableOpacity>
        </View>
      </View>
      {/* Dateiliste anzeigen, falls Dateien ausgewählt wurden */}
      {files.length > 0 && (
        <View style={styles.fileContainer}>
          <View style={styles.fileContainerInner}>
            {files.length > 0 && (
              <View
                style={[
                  styles.fileList
                ]}
              >
                {renderItems}
              </View>
            )}
          </View>
        </View>
      )}
    </FormElementWrapper>
  );

}

async function onBinaryPress(props: IFormComponentAttributes): Promise<File[] | null> {
  // Open DocumentPicker
  let pickerResult;
  try {
    pickerResult = await DocumentPicker.getDocumentAsync({
      type: '*/*',
      multiple: props.isList,
    });

    if (pickerResult.canceled) {
      return null;
    }

    const selectedFiles: DocumentPickerAsset[] = pickerResult.assets || [];

    let filesToAdd: File[] = [];

    selectedFiles.forEach((filePickerAsset, index) => {
      if (filePickerAsset.file) {
        filesToAdd.push(filePickerAsset.file);
      }
    });

    // in single-file mode, only keep the first file
    if (!props.isList && filesToAdd.length > 1) {
      filesToAdd = [filesToAdd[0]];
    }

    await formEventsUtils.handleBinaryAdd(
      props.formState,
      props.uiState,
      props.jsonpath,
      props.id,
      props.updateState,
      props.restService,
      filesToAdd,
      props.isLoggedIn,
      props.formGridOptions,
      props.editable,
    );
    return filesToAdd;
  } catch (error) {
    console.error('DocumentPicker error: ', error);
    return null;
  }
}


BinaryElement.validate = function (
  state: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
  localValue?: any,
): CommonValidation {
  const commonResult: CommonValidation = validationUtils.commonValidate(
    state,
    jsonpath,
    id,
    updateErrorMessages,
    formGridOptions,
    localValue,
  );
  if (!commonResult.isValid) {
    return commonResult;
  }

  const binaryIsValid = BinaryElement.validateBinary(
    state,
    jsonpath,
    id,
    updateErrorMessages,
    t,
    formGridOptions?.fileExtensionsNotAllowed,
  );

  if (!binaryIsValid) {
    const elemState = utils.getElementByIdOrJsonPath(state.data, jsonpath, id);
    return {
      isValid: false,
      errorMessage: elemState?.validationMessage,
    };
  }
  return { isValid: true };
};

/**
 * Binary-spezifische Validierung:
 * - Dateiendungen
 * - Dateigrößen
 */
BinaryElement.validateBinary = function (
  state: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  t: TFunction,
  fileExtensionsNotAllowed?: string[],
): boolean {
  const value = utils.getValue(state, jsonpath, id);
  const elemState = utils.getElementByIdOrJsonPath(state.data, jsonpath, id);

  if (
    elemState &&
    (value.fileName || (Array.isArray(value) && value[0]?.fileName))
  ) {
    if (
      !BinaryElement.hasAllowedFileExtension(
        state,
        jsonpath,
        elemState,
        updateErrorMessages,
        value,
        t,
        fileExtensionsNotAllowed,
      )
    ) {
      return false;
    }
    if (
      !hasAllowedUploadSize(
        state,
        jsonpath,
        elemState,
        updateErrorMessages,
        value,
        t,
      )
    ) {
      return false;
    }
  }

  return true;
};

/**
 * Zusammenfassungswert für die Summary-Anzeige
 */
BinaryElement.getSummaryValue = function (
  state: FormState,
  jsonpath: string,
  id: string,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
  restService?: RestService,
) {
  const value = utils.getValue(state, jsonpath, id);
  const schemaState = utils.getSchemaForElement(state.schema, id);

  let summaryValue: any = [];
  let fileArray: any = [];
  if (schemaState.isList && !value.hasOwnProperty('fileName')) {
    if (Array.isArray(value)) {
      fileArray = value;
    } else {
      fileArray = [];
    }
  } else {
    fileArray.push(value);
  }

  const rows: React.ReactNode[] = fileArray.map(
    (fileObject: any, i: number) => {
      if (!fileObject.fileName) return null;
      const link = BinaryElement.getRenderedBinaryLinkMarkup(
        state,
        fileObject,
        restService!,
      );
      return <View key={i}>{link}</View>;
    },
  );
  return <View style={binaryElementStyles.summaryLinkListRow}>{rows}</View>;
};

/**
 * Render binary link uniformly in edit and summary mode
 * Links are rendered for logged in users only
 * when a user is not logged in, only the file name is displayed
 */
BinaryElement.getRenderedBinaryLinkMarkup = function (
  formState: FormState,
  fileObject: IBinary,
  restService: RestService,
) {
  const styles = mergeStyles(binaryElementStyles, customBinaryElementStyles);
  let binaryLink = '';
  if (formState?.formid && fileObject?.value) {
    binaryLink = restService.ANGetBinaryURL(
      formState.formid,
      fileObject.value,
      formState.businessObjectID,
    );
  }
  let notUploadedYet = fileObject.value.startsWith('mesh(not uploaded yet):');

  if (binaryLink !== '' && formState.isLoggedIn && !notUploadedYet) {
    return (
      <AndpText
        numberOfLines={1}
        ellipsizeMode="tail"
        style={[styles.fileListItemText, styles.fileListItemLinkText]}
        onPress={() => Linking.openURL(binaryLink)}
      >
        {fileObject.fileName}
      </AndpText>
    );
  } else {
    return (
      <AndpText style={styles.fileListItemText}>{fileObject.fileName}</AndpText>
    );
  }
};

/**
 * Hilfsfunktion, um zu erkennen, ob es sich um eine bereits in ActaNova
 * gespeicherte Datei handelt (dann müssen wir sie nicht erneut validieren).
 */
function isActaNovaFile(file: IBinary) {
  if (
    !file.value ||
    file.value === '' ||
    file.value.startsWith('mesh:') ||
    file.value.startsWith('mesh(not uploaded yet):')
  ) {
    return false;
  }
  return true;
}

BinaryElement.hasAllowedFileExtension = function (
  formState: FormState,
  jsonpath: string,
  elemState: IDataFieldProperties,
  updateErrorMessages: boolean,
  value: IBinary | IBinary[],
  t: TFunction,
  fileExtensionsNotAllowed?: string[],
) {
  const fileExtensionsAllowedDefault = formState.features
    ?.feature_upload_constraints?.fileExtensionsAllowed
    ? formState.features.feature_upload_constraints.fileExtensionsAllowed
    : [];
  // are there any file extensions restrictions?
  // if so reduce the list of allowed extensions by default by the not allowed extensions
  const allowedExtensions = fileExtensionsNotAllowed
    ? fileExtensionsAllowedDefault.filter(
      (extension) => fileExtensionsNotAllowed.indexOf(extension) === -1,
    )
    : fileExtensionsAllowedDefault;
  let hasAllowedExtension = false;
  // multi file upload

  if (Array.isArray(value)) {
    // loop through all files
    // check if the filename ends with an allowed extension
    for (let fileIndex = 0; fileIndex < value.length; fileIndex++) {
      const file = value[fileIndex];
      const currentFileName = file.fileName;
      let isCurrentFileAllowed = false;
      // acta nova files don't need to be checked
      if (isActaNovaFile(value[fileIndex])) {
        isCurrentFileAllowed = true;
      } else {
        for (let index = 0; index < allowedExtensions.length; index++) {
          const currentFileExtension = allowedExtensions[index];
          if (
            currentFileName
              .substr(
                currentFileName.length - currentFileExtension.length,
                currentFileExtension.length,
              )
              .toLowerCase() === currentFileExtension.toLowerCase()
          ) {
            isCurrentFileAllowed = true;
            break;
          }
        }
      }
      // exit if there is one file with a not allowed extension
      if (!isCurrentFileAllowed) {
        hasAllowedExtension = false;
        break;
      } else {
        hasAllowedExtension = true;
      }
    }
    // now all files have been checked
  } else {
    // single file upload
    // check if filename does not end with one of the allowed extensions
    // acta nova files don't need to be checked
    if (isActaNovaFile(value)) {
      hasAllowedExtension = true;
    } else {
      hasAllowedExtension = allowedExtensions.some(
        (currentFileExtension) =>
          value.fileName
            .substr(
              value.fileName.length - currentFileExtension.length,
              currentFileExtension.length,
            )
            .toLowerCase() === currentFileExtension.toLowerCase(),
      );
    }
  }

  if (!hasAllowedExtension && updateErrorMessages) {
    elemState.validationMessage = `${t(
      'form_fileTypeError',
    )} ${allowedExtensions?.join(', ')}`;
    utils.setStateElementByJsonpath(formState.data, jsonpath, elemState);
  }

  return hasAllowedExtension;
};

/**
 * Check auf erlaubte Dateigröße (Einzel- und Gesamtgrößenbeschränkung)
 */
function hasAllowedUploadSize(
  formState: FormState,
  jsonpath: string,
  elemState: IDataFieldProperties,
  updateErrorMessages: boolean,
  value: IBinary | IBinary[],
  t: TFunction,
) {
  // use config from feature if available or fall back to 5 MB per file / 20 MB overall
  const maxSingleUploadSizeByte = formState.features?.feature_upload_constraints
    ?.maxSizePerFile
    ? formState.features.feature_upload_constraints.maxSizePerFile
    : 5_242_880;
  const maxMultipleUploadSizeByte = formState.features
    ?.feature_upload_constraints?.maxSizeOverall
    ? formState.features.feature_upload_constraints.maxSizeOverall
    : 20_971_520;
  const byteToMBFactor = 1_048_576; // 1024 * 1024
  const maxSingleUploadSizeMB = Math.floor(
    maxSingleUploadSizeByte / byteToMBFactor,
  );
  const maxMultipleUploadSizeMB = Math.floor(
    maxMultipleUploadSizeByte / byteToMBFactor,
  );

  let sumSizes = 0;
  let failedCheckForOneFile = false;

  if (Array.isArray(value)) {
    // multiple files
    value.forEach((singleElement) => {
      // only consider non-ActaNova files
      if (!isActaNovaFile(singleElement)) {
        if (singleElement.size) {
          sumSizes = sumSizes + singleElement.size;
          // if (singleElement.size > maxSingleUploadSizeByte) {
          //   if (updateErrorMessages) {
          //     elemState.validationMessage = `${t(
          //       'form_maxFileUploadSizeSingleError',
          //     ).replace('%size%', maxSingleUploadSizeMB.toString())}: ${singleElement.fileName
          //       }`;
          //   }
          //   utils.setStateElementByJsonpath(
          //     formState.data,
          //     jsonpath,
          //     elemState,
          //   );
          //   failedCheckForOneFile = true;
          //   return false;
          // }
        }
      }
    });
    if (failedCheckForOneFile) return false;

    // overall
    // if (sumSizes > maxMultipleUploadSizeByte) {
    //   if (updateErrorMessages) {
    //     elemState.validationMessage = `${t(
    //       'form_maxFileUploadSizeMultipleError',
    //     )}: ${maxMultipleUploadSizeMB} MB`;
    //   }
    //   utils.setStateElementByJsonpath(formState.data, jsonpath, elemState);
    //   return false;
    // }
  } else if (
    // single file
    !isActaNovaFile(value) &&
    value.size &&
    value.size > maxSingleUploadSizeByte
  ) {
    if (updateErrorMessages) {
      elemState.validationMessage = `${t(
        'form_maxFileUploadSizeSingleError',
      ).replace('%size%', maxSingleUploadSizeMB.toString())}: ${maxMultipleUploadSizeMB} MB`;
      utils.setStateElementByJsonpath(formState.data, jsonpath, elemState);
    }
    return false;
  }
  return true;
}

export default BinaryElement;