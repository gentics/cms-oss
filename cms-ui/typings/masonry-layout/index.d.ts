declare namespace masonry {
    interface Options {
        /**
         * Aligns items to a horizontal grid.
         */
        columnWidth?: number;
        /**
         * CSS styles that are applied to the container element.
         */
        containerStyle?: { [cssKey: string]: any };
        /**
         * Sets the width of the container to fit the available number of columns, based the size of container's parent element. When enabled, you can center the container with CSS.
         */
        fitWidth?: boolean;
        /**
         * Adds horizontal space between item elements.
         */
        gutter?: number;
        /**
         * Specifies which child elements will be used as item elements in the layout.
         */
        itemSelector?: string;
        /**
         * Enables layout on initialization. Enabled by default.
         * Set `initLayout: false` to disable layout on initialization, so you can use methods or add events before the initial layout.
         */
        initLayout?: boolean;
        /**
         * Controls the horizontal flow of the layout. By default, item elements start positioning at the left, with originLeft: true. Set originLeft: false for right-to-left layouts.
         */
        originLeft?: boolean;
        /**
         * Controls the vertical flow of the layout. By default, item elements start positioning at the top, with originTop: true. Set originTop: false for bottom-up layouts.
         */
        originTop?: boolean;
        /**
         * Sets item positions in percent values, rather than pixel values. Works well with percent-width items, as items will not transition their position on resize.
         */
        percentPosition?: boolean;
        /**
         * Adjusts sizes and positions when window is resized. Enabled by default.
         */
        resize?: boolean;
        /**
         * Staggers item transitions, so items transition incrementally after one another. Set as a CSS time format, '0.03s', or as a number in milliseconds, 30.
         */
        stagger?: number | string;
        /**
         * Specifies which elements are stamped within the layout. Masonry will layout items below stamped elements.
         * The stamp option stamps elements only when the Masonry instance is first initialized. You can stamp additional elements afterwards with the stamp method.
         */
        stamp?: string;
        /**
         * Duration of the transition when items change position or appearance, set in a CSS time format. Default: '0.4s'
         */
        transitionDuration?: number | string;
    }

    interface Item {
        element: Element;
        layout: Layout;
        position: { x: number, y: number };
    }

    interface Size {
        borderBottomWidth: number;
        borderLeftWidth: number;
        borderRightWidth: number;
        borderTopWidth: number;
        height: number;
        innerHeight: number;
        innerWidth: number;
        isBorderBox: number;
        marginBottom: number;
        marginLeft: number;
        marginRight: number;
        marginTop: number;
        outerHeight: number;
        outerWidth: number;
        paddingBottom: number;
        paddingLeft: number;
        paddingRight: number;
        paddingTop: number;
        width: number;
    }

    interface LayoutConstructor {
        new(elementOrSelector: Element | string, options: Options): Layout;

        /**
         * Get the Masonry instance via its element. Masonry.data() is useful for getting the Masonry instance in JavaScript, after it has been initalized in HTML.
         */
        data(elementOrSelector: Element | string): Layout;
    }

    interface Layout {
        /**
         * Adds item elements to the Masonry instance. addItems does not lay out items like appended or prepended.
         */
        addItems(elements: Element | ArrayLike<Element>): void;
        /**
         * Adds and lays out newly appended item elements to the end of the layout.
         */
        appended(elements: Element | ArrayLike<Element>): void;
        /**
         * Removes the Masonry functionality completely. destroy will return the element back to its pre-initialized state.
         */
        destroy(): void;
        /**
         * Returns an array of item elements.
         */
        getItemElements(): Element[];
        /**
         * Lays out all item elements. layout is useful when an item has changed size, and all items need to be laid out again.
         */
        layout(): void;
        /**
         * Lays out specified items.
         */
        layoutItems(items: Item[], isStill: boolean): void;
        /**
         * Adds a Masonry event listener.
         * layoutComplete: Triggered after a layout and all positioning transitions have completed.
         */
        on(eventName: 'layoutComplete', listener: (laidOutItems: Item[]) => void): void;
        /**
         * Adds a Masonry event listener.
         * removeComplete: Triggered after an item element has been removed.
         */
        on(eventName: 'removeComplete', listener: (removedItems: Item[]) => void): void;
        /**
         * Adds a Masonry event listener.
         */
        on(eventName: string, listener: Function): void;
        /**
         * Adds a Masonry event listener to be triggered just once.
         */
        once(eventName: string, listener: Function): void;
        /**
         * Options applicable for Masonry instances.
         */
        options: Options;
        /**
         * Removes a Masonry event listener.
         */
        off(eventName: string, listener: Function): void;
        /**
         * Adds and lays out newly prepended item elements at the beginning of layout.
         */
        prepended(elements: Element | ArrayLike<Element>): void;
        /**
         * Recollects all item elements. For frameworks like Angular and React, reloadItems may be useful to apply changes to the DOM to Masonry.
         */
        reloadItems(): void;
        /**
         * Removes elements from the Masonry instance and DOM.
         */
        remove(elements: Element | ArrayLike<Element>): void;
        /**
         * Stamps elements in the layout. Masonry will lay out item elements around stamped elements.
         */
        stamp(elements: Element | ArrayLike<Element>): void;
        /**
         * Un-stamps elements in the layout, so that Masonry will no longer layout item elements around them.
         */
        unstamp(elements: Element | ArrayLike<Element>): void;
    }
}

declare module 'masonry-layout' {
    var masonryLayout: masonry.LayoutConstructor;
    export = masonryLayout;
}
