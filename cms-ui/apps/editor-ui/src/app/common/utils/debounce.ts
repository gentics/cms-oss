export function debounce<T>(fn: (args: T) => any, time: number): (args: T) => void {
    let timer = -1;
    const schedule = (args: T) => {
        if (timer !== -1) {
            window.clearTimeout(timer);
        }
        timer = window.setTimeout(() => {
            fn(args);
        }, time);
    }

    return (args) => {
        schedule(args);
    }
}
