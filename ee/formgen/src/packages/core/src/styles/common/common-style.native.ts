import { Platform, StyleSheet } from 'react-native';
import theme from '../theme'
import { dropdownInputTheme, renderHTMLTheme } from '../theme';
import { colors } from '../variables';

export const loadingComponentsStyles = StyleSheet.create({
  loaderContainer: {
    flex: 1,
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 16,
  },
  loaderText: {
    marginTop: 8,
    fontSize: 14,
    fontWeight: theme.fontWeight.regular,
    backgroundColor: 'transparent',
    color: theme.colors.textPrimary,
  },
  fullScreenContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    alignItems: 'center',
    justifyContent: 'center'
  },
  screenText: {
    marginTop: 12,
    fontSize: 18,
    color: theme.colors.textPrimary,
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
});

export const messagesStyle = StyleSheet.create({
  messageWrapper: {
    flex: 1,
  },
  messagesView: {
    flex: 1,
  },
  messageListView: {
    flex: 1,
  },
  messageListHeaderContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: theme.space.large,
    paddingBottom: theme.space.large,
    borderColor: theme.colors.backgroundGray,
    borderBottomWidth: theme.border.width.regular,
  },
  messageSearchFilterContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  messageSearchFilterInputContainer:{},
  unreadMessageFilterButton: {
    padding: theme.space.extraSmall,
    borderRadius: 999,
    marginLeft: theme.space.regular
  },
  toggleOn: {
    backgroundColor: theme.colors.backgroundAccent,
    color: theme.colors.textAccent
  },
  messageSearchIcon: {
    width: 18,
    height: 18,
  },
  messageSearchFilterInput: {},
  unreadMessageIcon: {
    width: 24,
    height: 24,
  },
  messageLoadingContainer: {
    flexDirection: "row", 
    marginTop: theme.space.extraLarge, 
    justifyContent: "center"
  },
  // ----- Message Widget -----
  messageListItemButton:{
    backgroundColor: theme.colors.backgroundPrimary,
    flexDirection: 'row',
    borderBottomWidth: theme.border.width.regular,
    borderBottomColor: theme.colors.backgroundGray,
  },
  messageListItemContainer: {
    flexDirection: 'column',
    justifyContent: 'space-between',
    padding: theme.space.large,
    flex: 1
  },
  messageListItemDetailsText: {
    color: theme.colors.textMuted
  },
  messageListItemUnreadIndicator: {
    width: 10, 
    height: 10, 
    backgroundColor: theme.colors.colorAccent, 
    borderRadius: 999, 
    marginLeft: theme.space.extraLarge, 
    marginTop: theme.space.extraLarge
  },
  messageListItemTitle: {
    fontWeight: theme.fontWeight.black, 
    fontSize: theme.fontSize.extraLarge
  },
  messageListItemDate: {
    fontSize: theme.fontSize.small
  },
  messageListItemSender: {
  },
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
    marginTop: theme.space.regular
  },
  messageListItemDeleteButton: {},
  // ----- Message Detail Screen -----
  messageDetailAreaContainer: {},
  messageDetailScrollView: {},
  messageDetailScrollViewSmallScreen: {},
  messagePrintButton: {},
  messageMetadataSummaryContainer: {
    backgroundColor: theme.colors.backgroundAccent,
    borderRadius: theme.border.borderRadius.regular,
    padding: theme.space.regular,
    marginBottom: theme.space.extraLarge
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
    alignItems: 'center'
  },
  messageDividerLine: {
    borderBottomWidth: theme.border.width.regular,
    borderColor: theme.colors.backgroundGray,
    paddingBottom: theme.space.extraLarge, 
    marginBottom: theme.space.large
  },
  messageAttachmentTitle: {
    fontWeight: theme.fontWeight.bold,
    fontSize: theme.fontSize.large,
    marginBottom: theme.space.regular
  },
  messagesAttachmentName: {
    fontWeight: theme.fontWeight.bold,
    color: theme.colors.textOnGray
  },
  messagesAttachmentDetail: {
    fontWeight: theme.fontWeight.light,
    color: theme.colors.textOnGray
  }
});

export const buttonsStyle = StyleSheet.create({
  // +----+ Responsive Button Styles +----+
  smallBtn: {
    height: 45,
    paddingHorizontal: 5,
  },
  smallText: {
    fontSize: theme.fontSize.small
  },

  // +----+ Basic Button Styles +----+
  btnWrapper:{
    width: '25%',
  },
  btn: {
    borderRadius: theme.border.borderRadius.small,
    paddingHorizontal: theme.space.regular,
    height: 50,
  },
  btnText: {
    textAlign: 'center',
    fontWeight: theme.fontWeight.bold,
    marginVertical: 'auto',
    marginBlock: 'auto'
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
  
  ul: {
    paddingLeft: 22,
    marginVertical: 8,
  },
  ol: {
    paddingLeft: 22,
    marginVertical: 8,
  },
  li: {
    marginBottom: 6,
  },
  blockquote: {
    marginLeft: 16,
    paddingLeft: 12,
    borderLeftWidth: 4,
    borderLeftColor: theme.border.color.gray,
    color: theme.colors.textMuted,
    fontStyle: 'italic' as const,
  },
  code: {
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    backgroundColor: theme.colors.backgroundGray,
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: theme.border.borderRadius.small,
  },
  pre: {
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    backgroundColor: theme.colors.backgroundGray,
    padding: 10,
    borderRadius: theme.border.borderRadius.small,
  },
});