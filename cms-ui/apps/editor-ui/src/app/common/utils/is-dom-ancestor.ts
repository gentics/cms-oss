export function isDomAncestor(possibleAncestor: Node, child: Node): boolean {
    let node = child;
    while (node && node.parentNode !== node) {
        if (node === possibleAncestor) {
            return true;
        }

        node = node.parentNode;
    }

    return false;
}
