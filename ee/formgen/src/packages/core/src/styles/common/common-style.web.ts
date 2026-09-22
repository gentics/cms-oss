import { Platform, StyleSheet } from 'react-native';
import theme from '../theme';
import { dropdownInputTheme, loaderTheme, renderHTMLTheme } from '../theme.web';

export const loadingComponentsStyles = StyleSheet.create({
  loaderText: {
    fontSize: loaderTheme.fontSize,
    color: loaderTheme.fontColor,
    fontFamily: loaderTheme.fontFamily,
  },
  loaderContainer: {
    width: '100%',
    flexDirection: 'row',
    alignItems: 'center',
    gap: theme.space.regular,
    justifyContent: 'center',
    marginVertical: theme.space.extraLarge,
  },
  fullScreenContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    alignItems: 'center',
    justifyContent: 'center',
    gap: theme.space.large,
  },
});

export const messagesStyle = StyleSheet.create({
  messagesView: {
    flexDirection: 'row',
    flex: 1,
  },
  messageListView: {
    flex: 1,
    width: '100%',
  },
  messageWrapper: {
    flex: 1,
    flexDirection: 'row',
  },
  messageListHeaderContainer: {
    alignItems: 'center',
    alignContent: 'flex-start',
    flexWrap: 'wrap',
    width: '30%',
    minWidth: 500,
    borderColor: theme.border.color.gray,
    borderRightWidth: theme.border.width.regular,
    shadowColor: theme.shadow.color.primary,
    shadowOpacity: theme.shadow.opacity.regular,
    shadowOffset: { width: 0, height: 20 },
    shadowRadius: theme.shadow.radius.regular,
    zIndex: 2,
  },
  messageSearchFilterContainer: {
    padding: theme.space.large,
    paddingRight: 0,
    flexDirection: 'row',
    width: '100%',
    borderColor: theme.border.color.gray,
    borderBottomWidth: theme.border.width.regular,
  },
  unreadMessageFilterButton: {
    borderRadius: 999,
    margin: theme.space.small,
    padding: theme.space.small,
  },
  toggleOn: {
    backgroundColor: theme.colors.backgroundAccent,
    color: theme.colors.textAccent,
  },
  messageSearchIcon: {
    width: 18,
    height: 18,
  },
  messageSearchFilterInputContainer: {},
  messageSearchFilterInput: {},
  unreadMessageIcon: {
    width: 24,
    height: 24,
    color: theme.colors.textOnGray,
  },
  messageLoadingContainer: {
    flexDirection: 'row',
    marginTop: theme.space.extraLarge,
    justifyContent: 'center',
  },
  // ----- Message Widget -----
  messageListItemButton: {
    backgroundColor: theme.colors.backgroundPrimary,
    flexDirection: 'row',
    borderBottomWidth: theme.border.width.regular,
    borderBottomColor: theme.border.color.gray,
  },
  messageListItemContainer: {
    flexDirection: 'column',
    justifyContent: 'space-between',
    padding: theme.space.large,
    flex: 1,
  },
  messageListItemDetailsText: {
    color: theme.colors.textMuted,
  },
  messageListItemUnreadIndicator: {
    width: 10,
    height: 10,
    backgroundColor: theme.colors.colorAccent,
    borderRadius: 999,
    marginLeft: theme.space.extraLarge,
    marginTop: theme.space.extraLarge,
  },
  messageListItemTitle: {
    fontWeight: theme.fontWeight.black,
    fontSize: theme.fontSize.extraLarge,
  },
  messageListItemDate: {
    fontSize: theme.fontSize.small,
  },
  messageListItemSender: {},
  messageListItemRightAction: {
    backgroundColor: theme.colors.colorDeleteRed,
    height: '100%',
    justifyContent: 'center',
    textAlign: 'center',
  },
  messageListItemLeftAction: {
    backgroundColor: theme.colors.colorAccent,
    height: '100%',
    justifyContent: 'center',
    textAlign: 'center',
  },
  messageListItemLeftActionContainer: {
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
    width: 90,
  },
  messageListItemActionText: {
    fontWeight: theme.fontWeight.bold,
    color: theme.colors.textOnColor,
    textAlign: 'center',
    marginTop: theme.space.regular,
  },
  messageListItemDeleteButton: {
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: theme.border.borderRadius.small,
    padding: theme.space.small,
    height: '90%',
  },
  // ----- Message Detail Screen -----
  messageDetailAreaContainer: {
    height: '100%',
    justifyContent: 'center',
    flexWrap: 'wrap',
    alignItems: 'center',
  },
  messageDetailScrollView: {
    margin: theme.space.large,
    backgroundColor: theme.colors.backgroundPrimary,
    borderRadius: theme.border.borderRadius.extraLarge,
    shadowColor: theme.shadow.color.primary,
    shadowOpacity: theme.shadow.opacity.regular,
    shadowOffset: { width: 0, height: 0 },
    shadowRadius: theme.shadow.radius.regular,
  },
  messageDetailScrollViewSmallScreen: {
    position: 'absolute',
    zIndex: 5,
    width: '100%',
    margin: 0,
    height: '100%',
    borderRadius: 0,
  },
  messagePrintButton: {
    padding: theme.space.regular,
    borderRadius: theme.border.borderRadius.small,
    backgroundColor: theme.colors.backgroundGray,
    flexDirection: 'row',
    gap: theme.space.small,
    alignItems: 'center',
  },
  messageMetadataSummaryContainer: {
    backgroundColor: theme.colors.backgroundAccent,
    borderRadius: theme.border.borderRadius.regular,
    padding: theme.space.regular,
    marginBottom: theme.space.extraLarge,
  },
  messageSenderText: {
    fontSize: theme.fontSize.large,
    color: theme.colors.textAccent,
    marginBottom: theme.space.extraSmall,
  },
  messageMetadataText: {
    color: theme.colors.textAccent,
  },
  messageAttachmentItem: {
    flexDirection: 'row',
    backgroundColor: theme.colors.backgroundGray,
    borderRadius: theme.border.borderRadius.regular,
    padding: theme.space.regular,
    alignItems: 'center',
  },
  messageDividerLine: {
    borderBottomWidth: theme.border.width.regular,
    borderColor: theme.colors.backgroundGray,
    paddingBottom: theme.space.extraLarge,
    marginBottom: theme.space.large,
  },
  messageAttachmentTitle: {
    fontWeight: theme.fontWeight.bold,
    fontSize: theme.fontSize.large,
    marginBottom: theme.space.regular,
  },
  messagesAttachmentName: {
    fontWeight: theme.fontWeight.bold,
    color: theme.colors.textOnGray,
  },
  messagesAttachmentDetail: {
    fontWeight: theme.fontWeight.light,
    color: theme.colors.textOnGray,
  },
});

export const fileStyles = StyleSheet.create({
  attachmentItem: {
    flexDirection: 'row',
    backgroundColor: theme.colors.backgroundGray,
    borderRadius: theme.border.borderRadius.regular,
    padding: theme.space.regular,
    alignItems: 'center',
    marginBottom: theme.space.small,
    ...(Platform.OS === 'web'
      ? {
          transitionProperty: 'opacity',
          transitionDuration: '0.3s',
          transitionTimingFunction: 'ease-in-out',
        }
      : {}),
  },
  attachmentIcon: {
    marginLeft: theme.space.regular,
    color: theme.colors.textOnGray,
  },
  attachmentErrorIcon: {
    marginLeft: theme.space.regular,
    color: theme.colors.colorDeleteRed,
  },
  attachmentLoaderContainer: {
    width: 24,
    alignItems: 'center',
    marginLeft: theme.space.regular,
  },
  attachmentLoader: {
    color: theme.colors.textOnGray,
  },
  attachmentName: {
    color: theme.colors.textOnGray,
    fontWeight: theme.fontWeight.bold,
  },
  attachmentDate: {
    color: theme.colors.textOnGray,
    fontWeight: theme.fontWeight.light,
  },
  attachmentNameError: {
    color: theme.colors.colorDeleteRed,
  },
  attachmentDateError: {
    color: theme.colors.colorDeleteRed,
  },
});

export const buttonsStyle = StyleSheet.create({
  // +----+ Responsive Button Styles +----+
  smallBtn: {
    height: 45,
    paddingHorizontal: 5,
  },
  smallText: {
    fontSize: theme.fontSize.small,
  },

  // +----+ Basic Button Styles +----+
  btnWrapper: {
    width: '25%',
    paddingHorizontal: 5,
  },
  btn: {
    borderRadius: theme.border.borderRadius.small,
    paddingHorizontal: theme.space.regular,
    height: 50,
  },
  btnText: {
    textAlign: 'center',
    fontWeight: theme.fontWeight.bold,
    marginBlock: 'auto',
  },

  // +----+ Button Variations Styles +----+
  // Primary Button
  btnPrimary: {
    backgroundColor: theme.colors.colorAccent,
  },
  btnPrimaryHover: {
    backgroundColor: '#278ed7',
  },
  btnPrimaryText: {
    color: theme.colors.textOnColor,
  },
  btnPrimaryTextHover: {
    color: theme.colors.backgroundGray,
  },
  // Secondary Button
  btnSecondary: {
    backgroundColor: theme.colors.backgroundPrimary, // TODO change theme for this
    borderColor: theme.colors.textPrimary, // TODO change theme for this
    borderWidth: theme.border.width.regular,
  },
  btnSecondaryText: {
    color: theme.colors.textPrimary,
  },
  btnSecondaryHover: {
    backgroundColor: '#f4f4f4',
  },
  btnSecondaryTextHover: {
    color: theme.colors.textPrimary,
  },
});


export const renderHTMLStyles = StyleSheet.create({
  scrollContainer: {
    width: 'auto',
  },
  image: {
    width: '100%',
    height: 200,
    marginVertical: 10,
  },
  imageContainer: {
    width: '100%',
  },
  imageError: {
    color: renderHTMLTheme.error.color,
    marginVertical: 10,
  },
  caption: {
    marginTop: 4,
  },
});