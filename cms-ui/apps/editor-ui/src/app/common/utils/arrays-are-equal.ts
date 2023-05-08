export function arraysAreEqual<T>(left: T[], right: T[]): boolean {
    return left.length === right.length &&
        left.every((leftval, index) => leftval === right[index]);
}
