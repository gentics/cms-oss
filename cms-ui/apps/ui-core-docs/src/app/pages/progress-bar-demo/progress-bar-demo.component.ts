import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Observable, Observer } from 'rxjs';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './progress-bar-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProgressBarDemoPage {

    @InjectDocumentation('progress-bar.component')
    documentation: IDocumentation;

    fileUploading = false;
    fileProgress = 0;
    loading = false;
    progressSpeed = 500;
    observableProgress: Observable<number>;

    startFileUpload(): void {
        this.fileProgress = 0;
        this.fileUploading = true;
        setTimeout(() => this.simulateProgress(), 300);
    }

    simulateProgress(): void {
        this.fileProgress = Math.min(1, this.fileProgress + Math.random() * 0.1);
        if (this.fileProgress === 1) {
            setTimeout(() => this.fileUploading = false, 300);
        } else {
            setTimeout(() => this.simulateProgress(), Math.random() * 200 + 200);
        }
    }

    waitPromise(milliseconds: number): Promise<void> {
        return new Promise<void>( (done: () => void) => {
            setTimeout(done, milliseconds);
        });
    }

    createObservable(): void {
        this.observableProgress = new Observable<number>((observer: Observer<number>) => {
            observer.next(0);
            setTimeout(() => observer.next(0.2), 300);
            setTimeout(() => observer.next(0.5), 800);
            setTimeout(() => observer.next(0.6), 1000);
            setTimeout(() => observer.next(0.75), 1200);
            setTimeout(() => observer.complete(), 2000);
        });
    }
}
