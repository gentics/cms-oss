import {
  FormState,
  IDataFieldProperties,
  IBinary,
  CommonValidation,
  IFormGridOptions,
  ISchemaFieldProperties,
} from "../types/interfaces"; // Pfad anpassen
import { TFunction } from "i18next";
import * as utils from "./Utils"; // Pfad zu deinem bestehenden Utils-File
import * as validationUtils from "./ValidationUtils";
import { PluginNameEnum } from "../constants/enums/PluginNameEnum";

/**
 * Returns false for new (not yet uploaded) or Mesh-linked files,
 * so they are skipped during ActaNova-specific validation.
 */
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

/**
 * Validates file MIME type and filename against regex patterns defined in the CMS Forms schema.
 */
function cmsHasAllowedFileExtension(
  formState: FormState,
  jsonpath: string,
  elemState: IDataFieldProperties,
  schemaProperty: ISchemaFieldProperties,
  updateErrorMessages: boolean,
  value: IBinary | IBinary[],
  t: TFunction,
): boolean {
  const activeTypeRegexes = schemaProperty.validation?.binary?.allowedFileTypes;
  const activeNameRegexes = schemaProperty.validation?.binary?.allowedFileNames;

  if (!activeTypeRegexes?.length && !activeNameRegexes?.length) return true;

  const files: IBinary[] = Array.isArray(value) ? value : [value];

  for (const file of files) {
    if (activeTypeRegexes?.length) {
      const mimeType = file.type ?? "";
      const typeAllowed = activeTypeRegexes.some((pattern) => {
        try {
          return new RegExp(pattern).test(mimeType);
        } catch {
          return false;
        }
      });
      if (!typeAllowed) {
        if (updateErrorMessages) {
          elemState.validationMessage = `${t("form_fileTypeError")} ${activeTypeRegexes.join(", ")}`;
          utils.setStateElementByJsonpath(formState.data, jsonpath, elemState);
        }
        return false;
      }
    }

    if (activeNameRegexes?.length) {
      const nameAllowed = activeNameRegexes.some((pattern) => {
        try {
          return new RegExp(pattern).test(file.fileName);
        } catch {
          return false;
        }
      });
      if (!nameAllowed) {
        if (updateErrorMessages) {
          elemState.validationMessage = `${t("form_fileTypeError")} ${activeNameRegexes.join(", ")}`;
          utils.setStateElementByJsonpath(formState.data, jsonpath, elemState);
        }
        return false;
      }
    }
  }

  return true;
}
/**
 * Validates file extension against the global feature_upload_constraints allowlist, optionally excluding specific extensions.
 */
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

/**
 * Delegates file extension validation to the plugin-specific implementation,
 * as each plugin (ActaNova, CMS Forms) defines allowed types differently.
 */
function hasAllowedFileExtension(
  formState: FormState,
  jsonpath: string,
  elemState: IDataFieldProperties,
  schemaProperty: ISchemaFieldProperties,
  updateErrorMessages: boolean,
  value: IBinary | IBinary[],
  t: TFunction,
  fileExtensionsNotAllowed?: string[],
): boolean {
  switch (formState.pluginName) {
    case PluginNameEnum.ACTA_NOVA:
      return anHasAllowedFileExtension(
        formState,
        jsonpath,
        elemState,
        updateErrorMessages,
        value,
        t,
        fileExtensionsNotAllowed,
      );
    case PluginNameEnum.CMS_FORMS:
      return cmsHasAllowedFileExtension(
        formState,
        jsonpath,
        elemState,
        schemaProperty,
        updateErrorMessages,
        value,
        t,
      );
    default:
      return true;
  }
}

/**
 * Checks that no file exceeds the plugin-specific max size — ActaNova reads
 * it from schema directly, CMS Forms stores it in MB and converts to bytes.
 */
export function hasAllowedUploadSize(
  formState: FormState,
  jsonpath: string,
  elemState: IDataFieldProperties,
  schemaProperty: ISchemaFieldProperties,
  updateErrorMessages: boolean,
  value: IBinary | IBinary[],
  t: TFunction,
): boolean {
  /** Value in Bytes */
  let maxSize: number | undefined;

  switch (formState.pluginName) {
    case PluginNameEnum.ACTA_NOVA:
      maxSize = schemaProperty.maxSize;
      break;
    case PluginNameEnum.CMS_FORMS: {
      const sizeInMb: number | undefined =
        schemaProperty.validation?.binary?.maxFilesize;
      maxSize = sizeInMb !== undefined ? sizeInMb * 1_024 * 1_024 : undefined;
      break;
    }
    default:
      maxSize = undefined;
  }

  if (maxSize === undefined) return true;

  const files: IBinary[] = Array.isArray(value) ? value : [value];

  for (const file of files) {
    if (file.size != null) {
      if (file.size > maxSize) {
        if (updateErrorMessages) {
          elemState.validationMessage = `${t(
            "form_maxFileUploadSizeSingleError",
          ).replace(
            "%size%",
            (maxSize / 1_024 / 1_024).toString(),
          )}: ${file.fileName}`;
          utils.setStateElementByJsonpath(formState.data, jsonpath, elemState);
        }
        return false;
      }
    }
  }

  return true;
}

/**
 * Runs file extension and size validation for a binary
 * field, skipping both checks if no file is present.
 */
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
  const schemaProperty: ISchemaFieldProperties = utils.getSchemaForElement(
    state.schema,
    id,
  );

  if (
    elemState &&
    (value.fileName || (Array.isArray(value) && value[0]?.fileName))
  ) {
    if (
      !hasAllowedFileExtension(
        state,
        jsonpath,
        elemState,
        schemaProperty,
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
        schemaProperty,
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

/**
 * Main entry point: runs common field validation first,
 * then binary-specific checks (extension + size).
 */
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
