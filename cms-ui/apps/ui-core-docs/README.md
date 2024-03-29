# Gentics UI-Core Documentation

This Application shows a basic implementation and examples of the Gentics UI-Core Components and Services.
Additionally renders out all available Inputs, Outputs and utility properties/methods to be used in day-to-day usage.

## Requirements

The documentation for most components is generated by the JSDoc of the components source files.
This is done via the typescript-compiler, by parsing the file and parsing the JSDoc to HTML.

To prevent including the typescript compiler and all the heavy lifing to be done on each page load,
the compilation/document parsing has to be done before hand.

1. Compile the `./compile-docs.ts` file
2. Make sure, all required components/services are registered in `./docs.input.json`
3. Run the `./compile-docs.js` file
4. All the generated documentation is now saved as json in `./docs.output.json`

**Only after building the documentation can you properly build/service this application.**
