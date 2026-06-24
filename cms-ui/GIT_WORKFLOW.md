# Git workflow for working on the Gentics CMS UI

1.  Fix or feature?

    First, identify if what you are about to implement is a new feature or a fix for an existing one.

2.  Check out a new branch

    For **features**, branch off dev:
    ```sh
    git checkout -b f-ABC-123 origin/dev
    ```

    For **fixes**, check out a branch at the oldest supported hotfix branch.

    ```sh
    git checkout -b hotfix-6.4.x-ABC-123 origin/hotfix-6.4.x
    ```

3.  Do your changes and commit them appropriately

    In short: Do small, independent commits with the feature/fix, test(s),
    and changelog entries.
    Avoid committing multiple unrelated changes in one big commit.
    Use a descriptive subject line in imperative, no ending period.
    If necessary, add more details in the commit body.
    Always add the ticket number in the commit subject beginning.
    (source: [How to Write a Git Commit Message](https://chris.beams.io/posts/git-commit/))

    Example:
    ```text
    ABC-123: Add support for ABC in the XYZ module

    We needed to add ABC to support upcoming changes in DEF,
    to fix the performance bottleneck in GHI when using JKL.

    Fixes ABC-123
    Related Issue GCU-9876
    ```

    Creating the Changelog entry:

    1.  Run the [Number Cruncher](https://dev.gentics.com/numbercruncher/) to get a unique changelog number
        (let's assume we received `6789`.)
    2.  Create a file in the appropriate year/month `cms-oss-changelog` subdirectory, e.g.
        `cms-oss-changelog/src/changelog/entries/2026/01/6789.ABC-123.bugfix`
    3.  Describe what you changed in one or a few sentences.
        Please keep in mind that this is a public, customer-facing changelog.
        Internal changes don't need to be in the public changelog.

4.  Build all projects, and run the tests (for e2e tests see [README.md](./README.md#E2E+Integration_Tests))

    ```sh
    npm run many -- -t=build,test,component-test,e2e
    npm run lint
    ```

5.  Push your changes

    ```sh
    git push -u origin your-branch-name
    ```

    Now create a [Pull Request](https://github.com/gentics/cms-oss/pulls) and request a review from someone.
    Please mention the request for the review in a JIRA ticket, or directly to the person.

6.  Changes are requested in the code review review

    When code review feedback requests changes, checkout your branch again and either amend & force-push your commit (for small changes that only you work on)
    or create a second commit with your fixes (for larger changes or when working on a fix/feature together).

    After pushing, the merge request is updated, ask for review again.

7.  Your merge request is accepted

    Please make sure that your branch has been properly deleted when it has been merged.

8.  Merging between branches

    During the release process, older hotfix branches are merged to the newer hotfix branches:

    ```
          • Merge hotfix-6.3.x into hotfix-6.4.x
         ╱│
        ╱
       • Merge hotfix-6.4.x into hotfix-6.5.x
      ╱│
     ╱
    • Merge hotfix-6.5.x-ABC-123 into hotfix-6.5.x
    │╲
    │ ╲
    │  • Some change you did for hotfix-6.5.x
    │ ╱
    │╱
    •
    │
    ```

    You can also merge "older to newer" manually, but do not merge "newer to older".
    This would include all newer features and fixes for the newer version in the hotfix release.
    All hotfix branches can be merged to `dev`, but ` dev` should never be merged into a hotfix branch.
