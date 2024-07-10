import { AccessModifer, DocBlock, IDocumentation, PropertyGroup } from './src/app/common/docs';
import * as ts from 'typescript';
import { marked } from 'marked';
import hljs from 'highlight.js';
import { basename, resolve } from 'path';
import { readFileSync, writeFileSync } from 'fs';

marked.setOptions({
    highlight: (code: string, lang: string): string => {
        if (lang && hljs.getLanguage(lang)) {
            return hljs.highlight(lang, code, true).value;
        } else {
            return hljs.highlightAuto(code).value;
        }
    },
});

interface InputFile {
    [id: string]: SourceFile;
}

interface SourceFile {
    type: 'component' | 'service';
    sourceFile: string;
}

function main(): void {
    const INPUT_FILE = 'docs.input.json';
    const OUTPUT_FILE = 'docs.output.json';

    const data: InputFile = JSON.parse(readFileSync(resolve(__dirname, INPUT_FILE)).toString());
    const output: Record<string, IDocumentation> = {};

    Object.entries(data).forEach(([id, value]) => {
        const fileName = basename(value.sourceFile);
        const source = readFileSync(resolve(__dirname, value.sourceFile)).toString();
        const docs = createDocsFromAST(value.type, fileName, source);
        output[id] = docs;
    });

    writeFileSync(resolve(__dirname, OUTPUT_FILE), JSON.stringify(output, null, 4));
}

main();

function createDocsFromAST(type: 'component' | 'service', fileName: string, sourceCode: string): IDocumentation {
    /* eslint-disable-next-line @typescript-eslint/no-unsafe-assignment */
    const sourceFile = ts.createSourceFile(baseName(fileName), sourceCode, ts.ScriptTarget.ES2015, true, ts.ScriptKind.TS);

    function traverseChildren(parent: ts.Node): IDocumentation | null {
        const count = parent.getChildCount();
        for (let i = 0; i < count; i++) {
            const node = parent.getChildAt(i);

            // Only generate a Documentation for exported classes
            if (isNodeExported(node) && (ts.isClassDeclaration(node) || node.kind === ts.SyntaxKind.ClassDeclaration)) {
                return getClassDocumentation(type, sourceFile, node as ts.ClassDeclaration);
            }

            if (ts.isImportDeclaration(node) || ts.isExportDeclaration(node)) {
                continue;
            }

            const res = traverseChildren(node);
            if (res == null) {
                continue;
            }

            return res;
        }

        return null;
    }

    return traverseChildren(sourceFile);
}

function baseName(filePath: string): string {
    const idx = filePath.lastIndexOf('/');
    return idx > -1 ? filePath.substring(idx + 1) : filePath;
}

/** True if this is visible outside this file, false otherwise */
function isNodeExported(node: ts.Node): boolean {
    return (
        (ts.getCombinedModifierFlags(node as ts.Declaration) & ts.ModifierFlags.Export) !== 0
        || (!!node.parent && node.parent.kind === ts.SyntaxKind.SourceFile)
    );
}

function getClassDocumentation(
    type: 'component' | 'service',
    sourceFile: ts.SourceFile,
    classNode: ts.ClassDeclaration,
): IDocumentation {
    const docs: IDocumentation = {
        type,
        main: marked(getCommentFromNode(sourceFile, classNode)),
        inputs: [],
        methods: [],
        outputs: [],
        properties: [],
    };

    for (let i = 0; i < classNode.members.length; i++) {
        const member = classNode.members[i];

        if (ts.isPropertyDeclaration(member) || (ts.isFunctionLike(member) && !ts.isSetAccessorDeclaration(member))) {
            const propType = getPropertyGroup(member);
            const propDocs = getPropertyDocumentation(sourceFile, member as any);

            switch (propType) {
                case 'property':
                    if (ts.isFunctionLike(member) && !ts.isGetAccessorDeclaration(member)) {
                        docs.methods.push(propDocs);
                    } else {
                        docs.properties.push(propDocs);
                    }
                    break;
                case 'input':
                    docs.inputs.push(propDocs);
                    break;
                case 'output':
                    docs.outputs.push(propDocs);
                    break;
            }

            continue;
        }
    }

    return docs;
}

function getPropertyGroup(node: ts.Node): PropertyGroup {
    if (!ts.canHaveModifiers(node)) {
        return null;
    }
    const mods = node.modifiers;
    if (!mods) {
        return null;
    }

    for (const deco of mods) {
        const decoNames = getDecoratorNames(deco);
        if (decoNames.includes('Input')) {
            return 'input';
        } else if (decoNames.includes('Output')) {
            return 'output';
        } else if (decoNames.includes('IncludeToDocs')) {
            return 'property';
        }
    }

    return null;
}

function getDecoratorNames(decorator: ts.Node): string[] {
    const names: string[] = [];

    // Should never happen
    if (decorator.getChildCount() === 0) {
        return names;
    }

    for (const child of decorator.getChildren()) {
        let identifier: ts.Identifier;

        // Should also never happen
        if (child == null) {
            return names;
        }

        if (ts.isCallExpression(child)) {
            if (child.getChildCount() === 0) {
                return names;
            }
            const inner = child.getChildAt(0);
            if (ts.isIdentifier(inner)) {
                identifier = inner;
            }
        } else if (ts.isIdentifier(child)) {
            identifier = child;
        }

        if (identifier != null) {
            names.push(identifier.getText());
        }
    }

    return names;
}

function getPropertyDocumentation(sourceFile: ts.SourceFile, node: ts.PropertyDeclaration | ts.FunctionLikeDeclaration): DocBlock {
    // Ignore anonymous functions/properties
    if (node.name == null) {
        return null;
    }

    const base: Partial<DocBlock> = {
        identifier: node.name.getText(),
        body: marked(getCommentFromNode(sourceFile, node)),
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion, @typescript-eslint/no-unnecessary-type-assertion
        accessModifier: getAccessModifer(node),
    };
    let docs: DocBlock;

    if (ts.isPropertyDeclaration(node)) {
        docs = {
            ...base,
            type: determinePropertyType(node),
        } as DocBlock;

        if (node.initializer) {
            docs.defaultValue = node.initializer.getText();
        }
    } else {
        docs = {
            ...base,
            type: determineFunctionReturnType(node),
            methodArgs: getFunctionArgs(node),
        } as DocBlock;
    }

    return docs;
}

function determineFunctionReturnType(node: ts.FunctionLikeDeclaration): string {
    return node.type != null ? node.type.getText() : 'any';
}

function determinePropertyType(node: ts.PropertyDeclaration): string {
    if (node.type) {
        return node.type.getText();
    }

    if (node.initializer) {
        const type = getPrimitiveTypeName(node.initializer);
        if (type) {
            return type;
        }

        if (ts.isArrayLiteralExpression(node.initializer)) {
            if (node.initializer.getChildCount() === 0) {
                return '[]';
            }

            const arrType = getPrimitiveTypeName(node.initializer.getChildAt(0));
            if (arrType) {
                return arrType + '[]';
            }
        }
    }

    return null;
}

function getPrimitiveTypeName(node: ts.Node): string {
    if (ts.isNumericLiteral(node)) {
        return 'number';
    } else if (ts.isStringLiteral(node)) {
        return 'string';
    } else if (node.kind === ts.SyntaxKind.TrueKeyword || node.kind === ts.SyntaxKind.FalseKeyword) {
        return 'boolean';
    }

    return null;
}

function getFunctionArgs(node: ts.FunctionLikeDeclaration): string[] {
    return node.parameters.map(param => {
        let out = 'any';
        if (param.name != null) {
            out = param.name.getText();
            if (param.type != null ){
                out = `${out}: ${param.type.getText()}`;
            } else {
                out = `${out}: any`;
            }
        } else if (param.type != null) {
            out = param.type.getText();
        }
        return out;
    });
}

function getCommentFromNode(sourceFile: ts.SourceFile, node: ts.Node): string {
    const fullText = sourceFile.getFullText();
    const comment = (ts.getLeadingCommentRanges(sourceFile.getText(), node.getFullStart()) || [])
        .map(range => fullText.slice(range.pos, range.end))
        .join('\n');

    return stripStars(comment);
}

function getAccessModifer(node: ts.Node): AccessModifer {
    for (const child of node.getChildren()) {
        switch (child.kind) {
            case ts.SyntaxKind.ProtectedKeyword:
                return 'protected';
            case ts.SyntaxKind.PrivateKeyword:
                return 'private';
            case ts.SyntaxKind.PublicKeyword:
                return 'public';
        }
    }

    return 'public';
}

/**
 * Remove the `*` and padding from the doc block body.
 */
function stripStars(body: string): string {
    return body.replace(/^\/\*{2}|^\s*\*(\s?)[/]?/mg, '$1').trim();
}
