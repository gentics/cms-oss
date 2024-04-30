# Gentics UI-Core Documentation

This Application shows a basic implementation and examples of the Gentics UI-Core Components and Services.
Additionally renders out all available Inputs, Outputs and utility properties/methods to be used in day-to-day usage.

## Component Documentation

To generate the proper documentation from the JSDocs, and Annotations (`@Input`, `@Output`, ...) of the source-files of the ui-core,
we need a separate script to handle these files.
This can not be done during the regular build step, as it's requiring the typescript compiler, which we don't want to include
into the final application.

Therefore all source-files which are defined in `docs.input.json` are getting analyzed and the final documantion content
is being stored into `docs.output.json`.
The execution for this is defined in the `project.json` file, and will be executed before the `build` command.
