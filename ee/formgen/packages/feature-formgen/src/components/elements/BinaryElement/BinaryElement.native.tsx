import React, { useEffect, useMemo, useRef, useState } from 'react';
import { View, TouchableOpacity, Linking } from 'react-native';
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
} from '../../../types/interfaces';
import { useTranslation } from 'react-i18next';
import { binaryElementStyles } from '../../../styles/common/common-style.native';
import { binaryElementStyles as customBinaryElementStyles } from '../../../styles/custom/custom-style.native';
import { TFunction } from 'i18next';
import * as validationUtils from '../../../utils/ValidationUtils';
import * as formEventsUtils from '../../../utils/FormEventsUtils';
import * as utils from '../../../utils/Utils';
import { SummaryElement } from '../helper/SummaryElement';
import { FormElementWrapper } from '../helper/FormElementWrapper';
import AndpText from '@andp/core/src/components/AndpText';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { RestService } from '../../../services/RestService';
import * as DocumentPicker from 'expo-document-picker';
import { DocumentPickerAsset } from 'expo-document-picker';
import 'react-native-get-random-values';
import { v4 as uuidv4 } from 'uuid';
import FileCard from './parts/FileCard/FileCard.native';

type BinaryFileWithUid = IBinaryFile & { uid: string };

const binaryLocalCache: Map<string, BinaryFileWithUid[]> = new Map();

function BinaryElement(props: IFormComponentAttributes) {
  const { t } = useTranslation();
  const [files, setFiles] = useState<BinaryFileWithUid[]>([]);
  const labelText =
    utils.getI18nText(props.formState, props.label) +
    (props.required ? ' *' : '');
  const [localErrMsg, setLocalErrMsg] = useState(props.error);

  const styles = useMemo(
    () => mergeStyles(binaryElementStyles, customBinaryElementStyles),
    [],
  );

  const elementRef = useRef<View>(null);

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
  ]);

  const cacheKey = useMemo(() => {
    const formid = props.formState?.formid ?? 'noform';
    const jsonpath = String(props.jsonpath ?? '');
    const id = String(props.id ?? '');
    return `${formid}::${jsonpath}::${id}`;
  }, [props.formState?.formid, props.jsonpath, props.id]);

  useEffect(() => {
    const cached = binaryLocalCache.get(cacheKey);
    if (cached && cached.length && files.length === 0) {
      setFiles(cached);
    }
  }, [cacheKey]);

  useEffect(() => {
    binaryLocalCache.set(cacheKey, files);
  }, [cacheKey, files]);

  useEffect(() => {
    setLocalErrMsg(props.error);
  }, [props.error]);

  useEffect(() => {
    props.updateFirstErrorElementRef &&
      props.updateFirstErrorElementRef(props, elementRef);
  });

  function normalizeFiles(value: any): IBinary[] {
    if (!value) return [];
    if (Array.isArray(value)) return value.filter((v) => !!v?.fileName);
    if (typeof value === 'object' && value.fileName) return [value];
    return [];
  }

  const readExt = (name: string) =>
    name?.split('.').pop()?.toLowerCase() ?? '';

  const classifySource = (b: IBinary): BinarySource => {
    if (!b?.value) return 'local';
    if (String(b.value).startsWith('mesh:')) return 'mesh';
    if (String(b.value).startsWith('mesh(not uploaded yet)')) return 'local';
    return 'actanova';
  };

  const makeBinaryFromFile = (
    file: { name: string; size?: number | null; uri?: string }
  ): IBinary => ({
    fileName: file.name,
    type: readExt(file.name),
    uri: file.uri || '',
    value: '',
    size: file.size ?? undefined,
  });

  const toIBinaryFileFromServer = (formFile: IBinary): BinaryFileWithUid => {
    const fileType = readExt(formFile.fileName || '');
    const source = classifySource(formFile);
    const withUri: IBinary = {
      ...formFile,
      uri:
        props.formState?.formid && formFile.value
          ? props.restService.getBinaryURL(
            props.formState.formid,
            formFile.value,
            props.formState.businessObjectID,
            350,
            200,
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

  const getFileKey = (it: BinaryFileWithUid): string => it.uid;

  useEffect(() => {
    if (!props.formState?.formid) return;

    const serverListRaw = normalizeFiles(props.value).filter(
      (f) =>
        !!f?.fileName &&
        !!f?.value &&
        !String(f.value).startsWith('mesh(not uploaded yet)'),
    );

    const serverList: BinaryFileWithUid[] = serverListRaw.map(
      toIBinaryFileFromServer,
    );

    setFiles((prev) => {
      const serverNames = new Set(serverList.map((s) => s.binary.fileName));
      let remainingLocals = prev.filter(
        (x) => x.source === 'local' && !serverNames.has(x.binary.fileName),
      );

      // Single-file upload: keep at most one local file (the most recent)
      if (!props.isList && remainingLocals.length > 1) {
        remainingLocals = [remainingLocals[remainingLocals.length - 1]];
      }

      return [...serverList, ...remainingLocals];
    });
  }, [props.value, props.formState?.formid]);

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

    let binaryLink = '';

    // Mesh file => build URL from value
    if (item.source === 'mesh') {
      binaryLink = props.restService.getBinaryURL(
        props.formState.formid,
        item.binary.value,
        props.formState.businessObjectID,
      );
    }
    // Local file => use local URI (from picker)
    else if (item.source === 'local') {
      binaryLink = item.binary.uri || '';
    }
    // Server / generic case => fall back to binary.uri if set by toIBinaryFileFromServer
    else if (item.binary.uri) {
      binaryLink = item.binary.uri;
    }

    const showDownload =
      binaryLink !== '' &&
      !!props.formState &&
      !!props.formState.isLoggedIn;

    return (
      <FileCard
        key={key}
        t={props.t}
        file={item}
        showDownload={showDownload}
        onLink={() => {
          if (binaryLink) {
            Linking.openURL(binaryLink);
          }
        }}
        onRemove={async () => {
          if (!props.editable) {
            return;
          }

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
            props.editable,
          );

          setFiles(prev => prev.filter(f => f.uid !== key));
        }}
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
    >
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
          style={[
            styles.fileChooser,
            !props.editable && styles.disabledFileChooser,
            props.error && styles.errorFileChooser,
            files.length > 0 && styles.fileChooserWithFiles,
          ]}
          disabled={!props.editable}
          onPress={async () => {
            let pickerResult;
            try {
              pickerResult = await DocumentPicker.getDocumentAsync({
                type: '*/*',
                multiple: props.isList,
                // makes sure the picked file is available on disk
                copyToCacheDirectory: true,
              });

              // Newer expo-document-picker API returns { canceled, assets }
              // Older API may return { type, uri, name, size, mimeType, file }
              const anyResult: any = pickerResult;
              console.log('DocumentPicker result (native): ', anyResult);

              const assets: DocumentPickerAsset[] = anyResult.assets || [];

              // Normalize legacy shape (no assets array)
              if (!assets.length && anyResult.uri) {
                assets.push({
                  uri: anyResult.uri,
                  name: anyResult.name,
                  size: anyResult.size,
                  mimeType: anyResult.mimeType,
                  file: anyResult.file,
                } as any);
              }

              const canceled =
                anyResult.canceled === true ||
                anyResult.type === 'cancel';

              if (canceled || !assets.length) {
                return;
              }

              const fileObjs: any[] = [];

              assets.forEach((asset) => {
                // Web: expo-document-picker exposes a real File instance
                if ((asset as any).file) {
                  fileObjs.push((asset as any).file);
                  return;
                }

                // Native (Android / iOS): build a "file-like" object that
                // contains the metadata handleBinaryAdd expects (name, size, uri, type).
                fileObjs.push({
                  name: asset.name ?? 'file',
                  size: asset.size ?? 0,
                  uri: asset.uri,
                  type: asset.mimeType ?? 'application/octet-stream',
                });
              });

              if (!fileObjs.length) {
                return;
              }

              // Single-file upload: keep only the first file
              const filesToAdd = props.isList ? fileObjs : [fileObjs[0]];

              formEventsUtils.handleBinaryAdd(
                props.formState,
                props.uiState,
                props.jsonpath,
                props.id,
                props.updateState,
                props.restService,
                filesToAdd as any,
                props.isLoggedIn,
                props.formGridOptions,
                props.editable,
              );

              setFiles((prev) => {
                const additions: BinaryFileWithUid[] = filesToAdd.map((f: any) => ({
                  binary: makeBinaryFromFile(f),
                  file: f,
                  source: 'local',
                  fileType: readExt(f.name),
                  uid: uuidv4(),
                }));

                // Append in list mode, replace in single-file mode
                return props.isList ? [...prev, ...additions] : additions;
              });
            } catch (e) {
              console.error('DocumentPicker error: ', e);
              return;
            }
          }}
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

      {files.length > 0 && (
        <View style={styles.fileContainer}>
          <View style={styles.fileList}>{renderItems}</View>
        </View>
      )}
    </FormElementWrapper>
  );
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
  if (!commonResult.isValid) return commonResult;

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
    return { isValid: false, errorMessage: elemState?.validationMessage };
  }

  return { isValid: true };
};

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

  let fileArray: any = [];
  if (schemaState.isList && !value.hasOwnProperty('fileName')) {
    fileArray = Array.isArray(value) ? value : [];
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

BinaryElement.getRenderedBinaryLinkMarkup = function (
  formState: FormState,
  fileObject: IBinary,
  restService: RestService,
) {
  const styles = mergeStyles(binaryElementStyles, customBinaryElementStyles);

  let binaryLink = '';
  if (formState?.formid && fileObject?.value) {
    binaryLink = restService.getBinaryURL(
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
  }

  return (
    <AndpText style={styles.fileListItemText}>{fileObject.fileName}</AndpText>
  );
};

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
  const fileExtensionsAllowedDefault =
    formState.features?.feature_upload_constraints
      ?.fileExtensionsAllowed || [];

  const allowedExtensions = fileExtensionsNotAllowed
    ? fileExtensionsAllowedDefault.filter(
      (ext) => fileExtensionsNotAllowed.indexOf(ext) === -1,
    )
    : fileExtensionsAllowedDefault;

  let hasAllowedExtension = true;

  const files: IBinary[] = Array.isArray(value) ? value : [value];

  for (const file of files) {
    if (isActaNovaFile(file)) continue;

    const fileExt = file.fileName
      .split('.')
      .pop()
      ?.toLowerCase();

    if (!allowedExtensions.some((ext) => ext.toLowerCase() === fileExt)) {
      hasAllowedExtension = false;
      break;
    }
  }

  if (!hasAllowedExtension && updateErrorMessages) {
    elemState.validationMessage = `${t('form_fileTypeError')} ${allowedExtensions.join(', ')}`;
    utils.setStateElementByJsonpath(formState.data, jsonpath, elemState);
  }

  return hasAllowedExtension;
};

function hasAllowedUploadSize(
  formState: FormState,
  jsonpath: string,
  elemState: IDataFieldProperties,
  updateErrorMessages: boolean,
  value: IBinary | IBinary[],
  t: TFunction,
) {
  const maxSingleUploadSizeByte =
    formState.features?.feature_upload_constraints?.maxSizePerFile ??
    5_242_880;

  const maxMultipleUploadSizeByte =
    formState.features?.feature_upload_constraints?.maxSizeOverall ??
    20_971_520;

  const byteToMBFactor = 1_048_576;
  const maxSingleUploadSizeMB = Math.floor(
    maxSingleUploadSizeByte / byteToMBFactor,
  );
  const maxMultipleUploadSizeMB = Math.floor(
    maxMultipleUploadSizeByte / byteToMBFactor,
  );

  const files: IBinary[] = Array.isArray(value) ? value : [value];
  let sumSizes = 0;

  for (const file of files) {
    if (isActaNovaFile(file)) continue;

    if (file.size) {
      sumSizes += file.size;
      if (file.size > maxSingleUploadSizeByte) {
        if (updateErrorMessages) {
          elemState.validationMessage = `${t(
            'form_maxFileUploadSizeSingleError',
          ).replace('%size%', maxSingleUploadSizeMB.toString())}: ${file.fileName
            }`;
          utils.setStateElementByJsonpath(formState.data, jsonpath, elemState);
        }
        return false;
      }
    }
  }

  if (sumSizes > maxMultipleUploadSizeByte) {
    if (updateErrorMessages) {
      elemState.validationMessage = `${t(
        'form_maxFileUploadSizeMultipleError',
      )}: ${maxMultipleUploadSizeMB} MB`;
      utils.setStateElementByJsonpath(formState.data, jsonpath, elemState);
    }
    return false;
  }

  return true;
}

export default BinaryElement;

