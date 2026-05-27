import {
  FormState,
  IDataFieldProperties,
  IBinary,
  CommonValidation,
  IFormGridOptions,
} from "../types/interfaces"; // Pfad anpassen
import { TFunction } from "i18next";
import * as utils from "./Utils"; // Pfad zu deinem bestehenden Utils-File
import * as validationUtils from "./ValidationUtils";
import { PluginNameEnum } from "../constants/enums/PluginNameEnum";

// Hilfsfunktion: Prüfen, ob es eine bereits existierende ActaNova-Datei ist
export function isActaNovaFile(file: IBinary): boolean {
  if (
    !file.value ||
    file.value === "" ||
    file.value.startsWith("mesh:") ||
    file.value.startsWith("mesh(not uploaded yet):")
  ) {
    return false;
  }
  return true;
}

function cmsHasAllowedFileExtension(): boolean {
  // TODO - implement
  return false;
}

function anHasAllowedFileExtension(
  formState: FormState,
  jsonpath: string,
  elemState: IDataFieldProperties,
  updateErrorMessages: boolean,
  value: IBinary | IBinary[],
  t: TFunction,
  fileExtensionsNotAllowed?: string[],
): boolean {
  const fileExtensionsAllowedDefault =
    formState.features?.feature_upload_constraints?.fileExtensionsAllowed || [];

  const allowedExtensions = fileExtensionsNotAllowed
    ? fileExtensionsAllowedDefault.filter(
        (ext) => fileExtensionsNotAllowed.indexOf(ext) === -1,
      )
    : fileExtensionsAllowedDefault;

  let hasAllowedExtension = true;

  const files: IBinary[] = Array.isArray(value) ? value : [value];

  for (const file of files) {
    if (isActaNovaFile(file)) continue;

    const fileExt = file.fileName.split(".").pop()?.toLowerCase();

    if (!allowedExtensions.some((ext) => ext.toLowerCase() === fileExt)) {
      hasAllowedExtension = false;
      break;
    }
  }

  if (!hasAllowedExtension && updateErrorMessages) {
    elemState.validationMessage = `${t("form_fileTypeError")} ${allowedExtensions.join(", ")}`;
    utils.setStateElementByJsonpath(formState.data, jsonpath, elemState);
  }

  return hasAllowedExtension;
}

export function anHasAllowedUploadSize(
  formState: FormState,
  jsonpath: string,
  elemState: IDataFieldProperties,
  updateErrorMessages: boolean,
  value: IBinary | IBinary[],
  t: TFunction,
): boolean {
  const maxSingleUploadSizeByte =
    formState.features?.feature_upload_constraints?.maxSizePerFile ?? 5_242_880;

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
            "form_maxFileUploadSizeSingleError",
          ).replace("%size%", maxSingleUploadSizeMB.toString())}: ${
            file.fileName
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
        "form_maxFileUploadSizeMultipleError",
      )}: ${maxMultipleUploadSizeMB} MB`;
      utils.setStateElementByJsonpath(formState.data, jsonpath, elemState);
    }
    return false;
  }

  return true;
}

// 2. Gemeinsame Funktion für die Dateigröße
export function hasAllowedUploadSize(
  formState: FormState,
  jsonpath: string,
  elemState: IDataFieldProperties,
  updateErrorMessages: boolean,
  value: IBinary | IBinary[],
  t: TFunction,
): boolean {
  let maxSize: number | undefined;
  const elementFromSchema = utils.getSchemaForElement(
    formState.schema,
    elemState.id!,
  );
  switch (formState.pluginName) {
    case PluginNameEnum.ACTA_NOVA:
      maxSize = elementFromSchema.maxSize;
      break;
    case PluginNameEnum.CMS_FORMS:
      maxSize = elementFromSchema.validation?.binary?.max_filesize;
      break;
    default:
      maxSize = undefined;
  }

  if (maxSize === undefined) return true;

  const files: IBinary[] = Array.isArray(value) ? value : [value];

  for (const file of files) {
    if (file.size) {
      if (file.size > maxSize) {
        if (updateErrorMessages) {
          elemState.validationMessage = `${t(
            "form_maxFileUploadSizeSingleError",
          ).replace("%size%", maxSize.toString())}: ${file.fileName}`;
          utils.setStateElementByJsonpath(formState.data, jsonpath, elemState);
        }
        return false;
      }
    }
  }

  return true;
}

export function validateBinary(
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
      !hasAllowedFileExtension(
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
}

// 4. Gemeinsame validate-Funktion (der Haupt-Einstiegspunkt)
export function validate(
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

  const binaryIsValid = validateBinary(
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
}
