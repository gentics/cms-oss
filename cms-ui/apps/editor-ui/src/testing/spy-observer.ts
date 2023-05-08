import {Observer} from 'rxjs';

export class SpyObserver<T> implements Observer<T> {
    next = jasmine.createSpy(this.name + '.next');
    error = jasmine.createSpy(this.name + '.error');
    complete = jasmine.createSpy(this.name + '.complete');

    constructor(private name: string = '<observer>') { }
    toString(): string { return `SpyObserver("${name}")`; }
}
