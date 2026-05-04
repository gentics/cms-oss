import { StyleSheet } from 'react-native';

const baseStyles = StyleSheet.create({
  formContainer: {},
  formContainerScreenMaxMedium: {},
  row: {},
  h1: {},
  h2: {},
  h3: {},
  h4: {},
  h5: {},
  h6: {},
  p: {},
  ul: {},
  ulItem: {},
  ol: {},
  olItem: {},
  li: {},
  a: {},
  blockquote: {},
  code: {},
  pre: {},
  hr: {},
  b: {},
  strong: {},
  i: {},
  em: {},
  u: {},
});

const utilStyles = StyleSheet.create({
  visualyHidden: {},
});

const summaryStyles = StyleSheet.create({
  sectionHeader: {},
  summarySection: {},
  elementWrapper: {},
  elementLabel: {},
  elementValue: {},
  summaryDetailSection: {},
  subSection: {},
  subSectionHeader: {},
  detailDefinitionList: {},
  detailDefinitionRow: {},
  detailDefinitionLabel: {},
  detailDefinitionValue: {},
});

const successStyles = StyleSheet.create({
  successText: {},
  successIconBadge: {},
  successIcon: {},
});

const formElementWrapperStyles = StyleSheet.create({
  wrapper: {},
  formGroup: {},
  tooltipButton: {},
  tooltipButtonActive: {},
  tooltipIcon: {},
  tooltipIconActive: {},
  tooltipContainer: {},
  tooltipText: {},
  toolTipContent: {},
  toolTip: {},
  bottomMessages: {},
  needSpacing: {},
  inputItemsRight: {},
  validInputIcon: {},
  deleteContentBtnIcon: {},
  anFieldLoadErrorIcon: {},
  formElementWrapperLoader: {},
  // +--------------------+
  // | Form Element Error |
  // +--------------------+
  formFeedbackContainer: {},
  errorText: {},
  errorIcon: {},
  actaNovaErrorText: {},
  actaNovaErrorIcon: {},
  elementLoader: {},
});

const aggregateElementStyles = StyleSheet.create({
  // Container
  container: {},
  uploadButtonContainer: {},
  uploadButton: {},
  uploadButtonText: {},
  uploadButtonOnHover: {},
  uploadButtonTextOnHover: {},

  expandedAggregateItemIdicator: {},
  blackBarMobile: {},

  singleAggregateContainer: {},
  borderLine: {},

  actionButtons: {},
  // ---- Toggle Detail Button ----
  aggToggleDetailButton: {},
  aggToggleDetailButtonOnHover: {},
  aggToggleDetailButtonActive: {},
  aggToggleDetailButtonText: {},
  aggToggleDetailButtonTextOnHover: {},
  aggToggleDetailButtonTextActive: {},
  aggToggleDetailButtonIcon: {},
  aggToggleDetailButtonIconOnHover: {},
  aggToggleDetailButtonIconActive: {},
  aggToggleDetailButtonSmallView: {},
  aggToggleDetailButtonTextSmallView: {},
  // -----------------------------
  // ------ Aggregate Delete Button -----
  aggDeleteButton: {},
  aggDeleteButtonOnHover: {},
  aggDeleteButtonContent: {},
  aggDeleteButtonContentOnHover: {},
  aggDeleteButtonMobile: {},
  aggDeleteButtonMobileContent: {},
  // -----------------------------
  // ------ Aggregate Add Button -----
  aggAddButtonContainer: {},
  aggAddButton: {},
  aggAddButtonOnHover: {},
  aggAddButtonContent: {},
  aggAddButtonContentOnHover: {},
  aggAddButtonMobile: {},
  aggAddButtonMobileContent: {},
  singleDetailAggregateContainerMobile: {},
  // -----------------------------

  //+---------------------------------+
  //| Filter Section of the Aggregate |
  //+---------------------------------+
  filter: {},
  filterSettingsContainer: {},
  filterSettings: {},
  filterSettingsMobile: {},
  filterDivider: {},
  filterDropdownContainer: {},
  filterInputAndBtnContainer: {},
  filterInputContainer: {},
  filterInput: {},
  filterInputFocused: {},
  filterPlaceholder: {},
  appliedFiltersContainer: {},
  appliedFilterCard: {},
  appliedFilterCardTypeText: {},
  appliedFilterCardText: {},
  aggAddFilterButton: {},
  aggAddFilterPressable: {},
  addFilterButtonActive: {},
  addFilterButtonActiveOnHover: {},
  noEntriesContainer: {},
  noEntriesText: {},
  //+---------------------------------+
  //| /////////////////////////////// |
  //+---------------------------------+

  //+---------------------------------+
  //| ------ Tableview Styling ------ |
  //+---------------------------------+
  tableHeader: {},
  tableHeaderCell: {},
  tableAggHeaderText: {},
  tableRow: {},
  tableCell: {},
  tableAggActionButton: {},
  deleteButton: {},
  addButton: {},
  editButton: {},
  copyButton: {},
  tableRowHover: {},

  // Mobile (Card)Table View
  mobileCardList: {},
  mobileCard: {},
  mobileCardHover: {},
  mobileFieldRow: {},
  mobileFieldLabel: {},
  mobileFieldValue: {},
  mobileValidation: {},
  mobileActionsRow: {},
  mobileActionButton: {},
  mobileActionButtonContent: {},
  mobileDeleteButton: {},
  mobileDeleteButtonContent: {},
  //+---------------------------------+
  //| /////////////////////////////// |
  //+---------------------------------+

  // ------- Pagination Styles -------
  paginationContainer: {},
  pageItem: {},
  pageItemText: {},
  activePageItem: {},
  activePageItemText: {},
  disabledButton: {},
  disabledButtonText: {},
  breakContainer: {},
  breakText: {},

  //+-------------------+
  //| Summary Container |
  //+-------------------+
  summaryContainer: {},
  summaryRow: {},
  rowEven: {},
  rowOdd: {},
  summaryCell: {},
  labelText: {},
  valueText: {},
});

const dateTimeElementStyles = StyleSheet.create({
  // Styling des Pickers befindet sich unter ./custom-datepicker.css
  datePickerInput: {},
  disabledDatePickerInput: {},
  focusedDatePickerInput: {},
  errorDatePickerInput: {},
  datePickerInputText: {},
  datePickerLabel: {},
  errorDatePickerLabel: {},
});

const binaryElementStyles = StyleSheet.create({
  srOnly: {},
  fileChooser: {},
  fileChooserLabel: {},
  fileChooserLabelError: {},
  fileChooserLabelDisabled: {},
  fileChooserWithFiles: {},
  dragOverFileChooser: {},
  disabledFileChooser: {},
  errorFileChooser: {},
  fileContainer: {},
  fileList: {},
  fielListItem: {},
  fileListItemLinkText: {},
  fileListItemText: {},
  fileContainerInner: {},
  fileListRow: {},
});

const binaryFileCardStyles = StyleSheet.create({
  card: {},
  cardShadow: {},
  cardShadowHover: {},
  cardmobile: {},
  cardHovered: {},
  header: {},
  headerActions: {},
  iconButton: {},
  deleteIcon: {},
  downloadIcon: {},
  bodyThumbnailContainer: {},
  bodyIconContainer: {},
  bodyIcon: {},
  previewLabel: {},
  footer: {},
  fileName: {},
  fileType: {},
});

const customPdfPreviewStyles = StyleSheet.create({
  pdfPreview: {},
});

const baseDropDownInputElement = StyleSheet.create({
  dropdown: {},
  dropdownInputContainer: {},
  dropdownPlaceholderLabel: {},
  dropdownLabel: {},
  dropdownLabelActive: {},
  placeholderLabelError: {},
  inputElement: {},
  inputField: {},
  errorContainer: {},
  errorDropdown: {},
  dropdownContainer: {},
  dropdownItemsSeparator: {},
  dropdownOption: {},
  dropdownOptionHover: {},
  dropdownOptionError: {},
  dropdownOptionErrorHover: {},
  chipsWrapper: {},
  chip: {},
  chipText: {},
  errorChip: {},
  errorText: {},
  disabledInput: {},
  disabledDropDown: {},
});

const autocompleteInputElementStyle = StyleSheet.create({
  dropdownListItemText: {},
  dropdownListItemTextError: {},
});

const dropdownLazyCatalogElementStyles = StyleSheet.create({
  dropdownListItemText: {},
  activityText: {},
  activityTextError: {},
  dropdownListItemTextError: {},
});

const dropdownReferenceElementStyles = StyleSheet.create({
  dropdownListItem: {},
  dropdownListItemText: {},
  activityText: {},
  errorText: {},
  dropdownListItemTextError: {},
});

const dropdownCatalogElementStyles = StyleSheet.create({
  dropdownListItem: {},
  dropdownListItemText: {},
  activityText: {},
  errorText: {},
  dropdownListItemTextError: {},
});

const booleanElementStyles = StyleSheet.create({
  radioButtonGroupContainer: {},
  radioButtonGroupLabel: {},
  radioButtonGroupLabelError: {},
  radioButtonGroup: {},
  radioButtonRow: {},
  booleanElementContainer: {},
  disabledBooleanElementContainer: {},

  // -------- Radio Button ----------
  radioButtonElement: {},
  radioButtonDot: {},
  selectedRadioButton: {},
  errorRadioButton: {},
  errorRadioButtonDot: {},
  focusedRadioButton: {},
  radioButtonLabel: {},
  selectedRadioButtonLabel: {},
  errorRadioButtonLabel: {},
  focusedRadioButtonLabel: {},

  // -------- Checkbox --------------
  checkboxElement: {},
  checkboxIcon: {},
  disabledCheckboxIcon: {},
  errorCheckboxIcon: {},
  disabledCheckbox: {},
  selectedCheckbox: {},
  selectedDisabledCheckbox: {},
  errorCheckbox: {},
  selectedErrorCheckbox: {},
  focusedCheckbox: {},
  checkboxLabel: {},
  selectedCheckboxLabel: {},
  errorCheckboxLabel: {},
  focusedCheckboxLabel: {},
  disabledCheckboxLabel: {},
  selectedDisabledCheckboxLabel: {},

  // -------- Boolean Button --------
  booleanButton: {},
  booleanButtonDisabled: {},
  booleanButtonError: {},
  booleanButtonText: {},
  booleanButtonTextDisabled: {},
  booleanButtonTextError: {},
  booleanButtonFocused: {},
});

const inputElementStyles = StyleSheet.create({
  input: {},
  inputError: {},
  disabledInput: {},
  inputFocused: {},
  placeholder: {},
  placeholderError: {},
  placeholderFocused: {},
  placeholderText: {},
  placeholderTextFocused: {},
  inputNumber: {},
  inputNumberWithSpace: {},
  numberBtns: {},
  numberBtnUp: {},
  numberBtnDown: {},
  inputAreaFocused: {},
  textarea: {},
});

const pagingStyles = StyleSheet.create({
  formPagingContainer: {},
  dialogFormHeader: {},
  formPagingContainerScreenMaximumMedium: {},
  pagination: {},
  pageHeaderContainer: {},
  pageLabel: {},
  pageLabelContainerMaximumMedium: {},
  pageLabelScreenMaximumMedium: {},
  pageTitle: {},
  pageHeaderHyphenText: {},
  pageTitleScreenMaxiumMedium: {},
  pageItem: {},
  currentPageItem: {},
  disabledPageItem: {},
  hasErrorPageItem: {},
  number: {},
  currentNumber: {},
  disabledNumber: {},
  hasErrorNumber: {},
});

const stickyLayerStyles = StyleSheet.create({
  combinedStickyLayerContainer: {},
  stickylayerContainer: {},
  buttonContainerWrapper: {},
  buttonContainer: {},
  buttonContainerScreenMaxSmall: {},
  expertSearchCheckMarksContainer: {},
  expertSearchCheckMarksMultiButtonContainer: {},
});

const buttonStyles = StyleSheet.create({
  smallBtn: {},
  smallText: {},
  btnWrapperScreenMaxSmall: {},
  btnWrapper: {},
  btnDialogFormBack: {},
  btn: {},
  btnText: {},
  btnPrimary: {},
  btnPrimaryHover: {},
  btnPrimaryText: {},
  btnPrimaryTextHover: {},
  btnPrimaryDisabled: {},
  btnSecondary: {},
  btnSecondaryText: {},
  btnSecondaryHover: {},
  btnSecondaryTextHover: {},
  btnSecondaryDisabled: {},
  btnDialogFormBackStyle: {},
  btnDialogFormBackText: {},
  btnDialogFormBackStyleHover: {},
  btnDialogFormBackTextHover: {},
});

const expertSearchStyles = StyleSheet.create({
  tableContainer: {},
  headerRow: {},
  headerCell: {},
  headerCellOnHover: {},
  headerPressable: {},
  headerText: {},
  resizer: {},
  headerSelectBoxCell: {},
  row: {},
  rowOnHover: {},
  cell: {},
  buttonText: {},
  buttonOnHover: {},
  rowSelect: {},
  cardView: {},
  cardViewHeaderContainer: {},
  cardViewListTitle: {},
  cardSelectAllHeader: {},
  cardSelectAllHeaderText: {},
  cardSeparator: {},
  card: {},
  cardContainerHeader: {},
  cardHeader: {},
  cardTitle: {},
  cardDivider: {},
  cardButton: {},
  cardButtonOnHover: {},
  cardButtonText: {},
  cardRow: {},
  cardLabel: {},
  cardValue: {},
  emptyText: {},
});

const errorBoxStyles = StyleSheet.create({
  errorBoxContainer: {},
  errorBoxDescription: {},
  errorBoxItem: {},
  errorIcon: {},
  errorText: {},
  errorBoxDescriptionContainer: {},
  errorBoxWidthContainer: {},
  errorBoxListWidth: {},
  errorBoxList: {},
});

const defaultOverlayStyles = StyleSheet.create({
  modal: {},
  modalContent: {},
  modalContentWithoutFooter: {},
  modalContentScreenMaxLarge: {},
  modalContentScreenMaxMedium: {},
  modalContentScreenMaxSmall: {},
  body: {},
  bodyScrollView: {},
  bodyText: {},
});

const overlayButtonRowStyles = StyleSheet.create({
  buttonRow: {},
  buttonWrapper: {},
  buttonWrapperScreenMaxSmall: {},
});

const overlayFooterStyles = StyleSheet.create({
  footer: {},
  footerText: {},
});

const overlayHeaderStyles = StyleSheet.create({
  headerContent: {},
  headerIcon: {},
  headerTitle: {},
  closeButton: {},
  closeButtonIcon: {},
});

const overlayPreConditionStyles = StyleSheet.create({
  bodyAlign: {},
  preconditionItem: {},
  preconditionItemLink: {},
  preconditionFirstLineText: {},
  preconditionSecondLineText: {},
});

const debugModeSectionStyles = StyleSheet.create({
  section: {},
  inlineRow: {},
  headerRow: {},
  sectionLabel: {},
  contentText: {},
  scrollableArea: {},
  textarea: {},
  copyButton: {},
  copyButtonText: {},
});

const fullScreenOverlayStyles = StyleSheet.create({
  loadingOverlay: {},
});

const formWrapperLabelStyles = StyleSheet.create({
  labelContainer: {},
  labelText: {},
  labelTextError: {},
});


const formGridImageElementStyles = StyleSheet.create({
  container: {},
  caption: {},
});

export {
  summaryStyles,
  baseStyles,
  utilStyles,
  formElementWrapperStyles,
  pagingStyles,
  stickyLayerStyles,
  buttonStyles,
  errorBoxStyles,
  inputElementStyles,
  booleanElementStyles,
  baseDropDownInputElement,
  autocompleteInputElementStyle,
  dropdownLazyCatalogElementStyles,
  dropdownReferenceElementStyles,
  dropdownCatalogElementStyles,
  binaryElementStyles,
  binaryFileCardStyles,
  customPdfPreviewStyles,
  dateTimeElementStyles,
  aggregateElementStyles,
  defaultOverlayStyles,
  overlayButtonRowStyles,
  overlayFooterStyles,
  overlayHeaderStyles,
  overlayPreConditionStyles,
  debugModeSectionStyles,
  fullScreenOverlayStyles,
  expertSearchStyles,
  formWrapperLabelStyles,
  successStyles,
  formGridImageElementStyles,
};
