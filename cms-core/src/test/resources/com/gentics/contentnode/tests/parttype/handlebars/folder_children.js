function resolve(context) {
    var names = [];
    context.folder.children.forEach(function(item) {
        names.push(item.name);
    });
    return names.sort().join(",");
}
