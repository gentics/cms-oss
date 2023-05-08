export interface DateTimePickerStrings {
    hours: string;
    minutes: string;
    seconds: string;
    okay: string;
    cancel: string;
    months: string[];
    monthsShort?: string[];
    weekdays: string[];
    weekdaysShort?: string[];
    weekdaysMin?: string[];
}

export const DEFAULT_DATE_TIME_PICKER_STRINGS: DateTimePickerStrings = {
    hours: 'Hours',
    minutes: 'Minutes',
    seconds: 'Seconds',
    okay: 'Okay',
    cancel: 'Cancel',
    months: [
        'January',
        'February',
        'March',
        'April',
        'May',
        'June',
        'July',
        'August',
        'September',
        'October',
        'November',
        'December'
    ],
    monthsShort: [
        'Jan',
        'Feb',
        'Mar',
        'Apr',
        'May',
        'Jun',
        'Jul',
        'Aug',
        'Sep',
        'Oct',
        'Nov',
        'Dec'
    ],
    weekdays: [
        'Sunday',
        'Monday',
        'Tuesday',
        'Wednesday',
        'Thursday',
        'Friday',
        'Saturday'
    ],
    weekdaysShort: [
        'Sun',
        'Mon',
        'Tue',
        'Wed',
        'Thu',
        'Fri',
        'Sat'
    ],
    weekdaysMin: [
        'Su',
        'Mo',
        'Tu',
        'We',
        'Th',
        'Fr',
        'Sa'
    ]
};
