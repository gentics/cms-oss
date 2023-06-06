/**
 * A Date which is relative to the current time in years.
 *
 * @param years How many years should be added/substracted from the current date
 */
export function dateInYears(years: number) {
    const date = new Date();
    date.setFullYear(date.getFullYear() + years);
    return date;
}
