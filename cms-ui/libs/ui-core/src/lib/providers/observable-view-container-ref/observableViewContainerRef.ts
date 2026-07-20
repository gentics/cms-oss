import { ComponentRef, EmbeddedViewRef, TemplateRef, Type, ViewContainerRef, ViewRef } from '@angular/core';

/**
 * Wrapper around ViewContainerRef that allows tests to observe dynamically created components.
 * (since ComponentFactoryResolver and ComponentFactory are deprecated since Angular 22).
 * The wrapper extends ViewContainerRef and adds a hook after a component is created so
 * tests can set up spies on the ComponentRef.
 */
export class ObservableViewContainerRef extends ViewContainerRef {
    constructor(
        private readonly delegate: ViewContainerRef,
    ) {
        super();
    }

    override get element() {
        return this.delegate.element;
    }

    override get injector() {
        return this.delegate.injector;
    }

    override get parentInjector() {
        return this.delegate.parentInjector;
    }

    override get length() {
        return this.delegate.length;
    }

    override clear(): void {
        this.delegate.clear();
    }

    /**
     * List of component creation listeners registered by tests.
     * When a component is dynamically created the callback will be executed with the ComponentRef.
     */
    private spyConfigs: {
        componentTypes: Type<any>[];
        callback: (
            componentType: Type<any>,
            componentRef: ComponentRef<any>
        ) => void;
    }[] = [];

     /**
     * Registers a callback for specific dynamically created components.
     * Replaces the old ComponentFactoryResolver spying approach.
     * Remember which component types should be tested and handle them when createComponent() is called
     */
    registerComponentSpy(
        componentTypes: Type<any>[],
        callback: (
            componentType: Type<any>,
            componentRef: ComponentRef<any>
        ) => void,
    ): void {
        this.spyConfigs.push({
            componentTypes,
            callback,
        });
    }

    /**
     * Intercepts dynamic component creation.
     * Flow:
     * create the component normally -> 
     * Check if this component type was registered by a test -> 
     * execute the callback so test installs spies ->
     * Return the ComponentRef unchanged.
     *
     * Replacement for ComponentFactoryResolver -> ComponentFactory.create().
     */
    override createComponent<T>(
        componentType: Type<T>,
        options?: Object
    ): ComponentRef<T> {
        // actual component creation
        const componentRef = this.delegate.createComponent(componentType, options);

        // Notify tests that are interested in component type
        this.spyConfigs.forEach(config => {
            if (config.componentTypes.includes(componentType)) {
                config.callback(componentType, componentRef);
            }
        });

        return componentRef;
    }

    override createEmbeddedView<T>(
        templateRef: TemplateRef<T>,
        context?: T,
        options?: Object
    ): EmbeddedViewRef<T> {
        return this.delegate.createEmbeddedView(
            templateRef,
            context,
            options
        );
    }

    override get(index: number): ViewRef | null {
        return this.delegate.get(index);
    }

    override insert(viewRef: ViewRef, index?: number): ViewRef {
        return this.delegate.insert(viewRef, index);
    }

    override move(viewRef: ViewRef, currentIndex: number): ViewRef {
        return this.delegate.move(viewRef, currentIndex);
    }

    override indexOf(viewRef: ViewRef): number {
        return this.delegate.indexOf(viewRef);
    }

    override remove(index?: number): void {
        this.delegate.remove(index);
    }

    override detach(index?: number): ViewRef | null {
        return this.delegate.detach(index);
    }
}