import React from 'react';
import { TextStyle, View, ViewStyle, StyleSheet } from 'react-native';
import theme from '../styles/theme';

// Icons
import SearchIcon from '../assets/icons/magnifying-glass-solid.svg';
import UnreadMessageIcon from '../assets/icons/unread-message.svg';
import VerticalDotsIcon from '../assets/icons/ellipsis-vertical-solid.svg';
import TrashCanIcon from '../assets/icons/trash-solid.svg';
import OpenMessageIcon from '../assets/icons/envelope-open-solid.svg';
import PDFFIleIcon  from '../assets/icons/file-pdf-solid.svg';
import FileIcon from '../assets/icons/file-solid.svg';
import ExcelFileIcon from '../assets/icons/file-excel-solid.svg';
import PowerPointIcon from '../assets/icons/file-powerpoint-solid.svg';
import ImageFileIcon from '../assets/icons/file-image-solid.svg';
import TextFileIcon from '../assets/icons/file-lines-solid.svg';
import WordFileIcon from '../assets/icons/file-word-solid.svg';
import CloseIcon from '../assets/icons/xmark-solid.svg';
import PrinterIcon from '../assets/icons/print-solid.svg';
import CheckMarkIcon from '../assets/icons/check-solid.svg';
import ArrowLeftIcon from '../assets/icons/arrow-left-solid.svg';
import FolderIcon from '../assets/icons/folder-solid.svg';
import InfoIcon from '../assets/icons/circle-info-solid.svg';
import EarthIcon from '../assets/icons/earth-europe-solid.svg';
import PenToSquareIcon from '../assets/icons/pen-to-square-solid.svg';
import HomeIcon from '../assets/icons/house-solid.svg';
import ArrowDownIcon from '../assets/icons/angle-down-solid.svg'
import ArrowUpIcon from '../assets/icons/angle-up-solid.svg'
import TriangleExclamationIcon from '../assets/icons/triangle-exclamation-solid.svg';
import CircleExclamationIcon from '../assets/icons/circle-exclamation-solid.svg';
import OpenLinkIcon from '../assets/icons/open-link-solid.svg'
import CheckBoxIcon from '../assets/icons/square-check-regular.svg'
import FilterLinesIcon from '../assets/icons/filter-lines-solid.svg'
import CaretUp from '../assets/icons/caret-up-solid.svg'
import CaretDown from '../assets/icons/caret-down-solid.svg'
import Pencil from '../assets/icons/pencil-solid.svg'
import PlusSquare from '../assets/icons/square-plus-solid.svg'
import Plus from '../assets/icons/plus-solid.svg'
import Copy from '../assets/icons/copy-solid.svg'
import UnCheckedSquare from '../assets/icons/square-regular.svg';
import GearSolid from '../assets/icons/gear-solid.svg';
import GenticsLogo from '../assets/icons/gentics-logo-coloured.svg';
import BarsSolidIcon from '../assets/icons/bars-solid.svg';
import DownloadIcon from '../assets/icons/download-solid.svg';
import WifiIcon from '../assets/icons/wifi-solid.svg';
import CircleCheckIcon from '../assets/icons/circle-check-solid-full.svg';

// Definiere deine verfügbaren SVG-Icons
const iconSet = {
  search: SearchIcon,
  unreadMessage: UnreadMessageIcon,
  verticalDots: VerticalDotsIcon,
  trashCan: TrashCanIcon,
  openMessageIcon: OpenMessageIcon,
  pdfFile: PDFFIleIcon,
  file: FileIcon,
  excelFile: ExcelFileIcon,
  powerPointFile: PowerPointIcon,
  imageFile: ImageFileIcon,
  textFile: TextFileIcon,
  wordFile: WordFileIcon,
  close: CloseIcon,
  printer: PrinterIcon,
  checkMark: CheckMarkIcon,
  folder: FolderIcon,
  arrowLeft: ArrowLeftIcon,
  info: InfoIcon,
  earth: EarthIcon,
  form: PenToSquareIcon,
  home: HomeIcon,
  arrowDown: ArrowDownIcon,
  arrowUp: ArrowUpIcon,
  warning: TriangleExclamationIcon,
  circleWarning: CircleExclamationIcon,
  openLink: OpenLinkIcon,
  checkBox: CheckBoxIcon,
  filter: FilterLinesIcon,
  caretUp: CaretUp,
  caretDown: CaretDown,
  pencil: Pencil,
  plusSquare: PlusSquare,
  plus: Plus,
  copy: Copy,
  uncheckedSquare: UnCheckedSquare,
  settings: GearSolid,
  appsTabLogo: GenticsLogo,
  bars: BarsSolidIcon,
  download: DownloadIcon,
  wifi: WifiIcon,
  circleCheck: CircleCheckIcon
};

const iconSizeSet = {
  sm: 12,
  md: 18,
  lg: 24,
}

export type IconName = keyof typeof iconSet;
type IconSize = keyof typeof iconSizeSet;

type CustomIconProps = {
  name: IconName;
  size?: IconSize | number;
  color?: string;
  style?: ViewStyle | TextStyle | any[];
};

const CustomIcon: React.FC<CustomIconProps> = ({ name, size = 'md', color = theme.colors.textOnGray, style }) => {
  const styleObj = StyleSheet.flatten(style) || {};
  const IconComponent = iconSet[name];
  //@ts-ignore
  const iconSize = styleObj?.fontSize !== undefined ? styleObj.fontSize :
    typeof size === 'number' ? size : iconSizeSet[size];
  //@ts-ignore
  const fillColor = styleObj?.color !== undefined ? styleObj.color : color;
  return <View style={style}>
    <IconComponent width={iconSize} height={iconSize} fill={fillColor}/>
  </View>;
  
};

export default CustomIcon;
