# Git workflow for working on the Gentics CMS UI in the contentnode repository

1.  Fix or feature?

    First, identify if what you are about to implement is a new feature or a fix for an existing one.

2.  Check out a new branch

    For **features**, branch off dev:
    ```sh
    git checkout -b f-myfeature-gcu-1234 origin/dev
    ```

    For **fixes**, check out a branch at the oldest supported branch.

    You can find a list of supported branches at [dev.gentics.com](https://dev.gentics.com/).
    The first branch in the list is the oldest supported branch, `hotfix-haymo` at the time of writing.
    If the feature you are fixing only exists in a newer release,
    branch off the hotfix branch for that release (e.g. `hotfix-klaus`).

    ```sh
    git checkout -b hotfix-haymo-gcu-1234 origin/hotfix-haymo
    ```

3.  Do your changes and commit them appropriately

    In short: Do small, independent commits with the feature/fix, unit test(s)
    and [changelog entry](https://collaboration.apa.at/pages/viewpage.action?pageId=18650517).
    Avoid commiting multiple unrelated changes in one big commit.
    Use a descriptive subject line in imperative, no ending period.
    If necessery, add more details in the commit body.
    Always add the ticket number in the commit body.
    (source: [How to Write a Git Commit Message](https://chris.beams.io/posts/git-commit/))

    Example:
    ```text
    Add support for ABC in the XYZ module

    We needed to add ABC to support upcoming changes in DEF,
    to fix the performance bottleneck in GHI when using JKL.

    Fixes GCU-1234
    Related Issue GCU-9876
    ```

    Creating the Changelog entry: [(more detailed guide in Confluence)](https://collaboration.apa.at/pages/viewpage.action?pageId=18650517)

    1.  Run the [Number Cruncher](https://dev.gentics.com/numbercruncher/) to get a unique changelog number
        (let's assume we received `6789`.)
    2.  Create a file in the appropriate year/month `contentnode-changelog` subdirectory, e.g.
        `contentnode-changelog/src/changelog/entries/2018/01/6789.GCU-1234.bugfix`
    3.  Describe what you changed in one or a few sentences.
        Please keep in mind that this is a public, customer-facing changelog.
        Internal changes don't need to be in the public changelog.

    Further developer documentation is available in Confluence:
    * [Gentics GIT Guidelines](https://collaboration.apa.at/display/ITG/GIT+Guidelines)
    * [Gentics Coding Guidelines](https://collaboration.apa.at/display/ITG/Coding+Guidelines)
    * [Gentics Definition of Done](https://collaboration.apa.at/display/ITG/Definition+of+done)

4.  Run the unit tests and lint your code

    ```sh
    npm test
    npm run lint
    ```

5.  Push your changes to GitLab

    ```sh
    git push -u origin your-branch-name
    ```

    This will create a [merge request in GitLab](https://git.gentics.com/psc/contentnode/merge_requests/).
    To get your changes merged, ask another team member to review your code,
    either mention them in the merge request, in the JIRA ticket
    or ask at the daily standup.
    Feel free to delete your local branch, it can be retrieved from the remote.

    You are (almost) done at this point.

6.  Changes are requested in the code review review

    When code review feedback requests changes, checkout your branch again and either amend & force-push your commit (for small changes that only you work on)
    or create a second commit with your fixes (for larger changes or when working on a fix/feature together).

    After pushing, the merge request is updated, mention your reviewer again in the pull request or tell them in Mattermost to let them know.

7.  Your merge request is accepted

    Please update your ticket to "work done" (GTXPE) or "done" (GCU).

8.  Merging between branches

    During the release process, older hotfix branches are merged to the newer hotfix branches:

    ```
             • Merge hotfix-klaus into hotfix-clemens
            ╱│
           ╱
          • Merge hotfix-alexander into hotfix-klaus
         ╱│
        ╱
       • Merge hotfix-haymo into hotfix-alexander
      ╱│
     ╱
    • Merge your-feature into hotfix-haymo
    │╲
    │ ╲
    │  • Some change you did for hotfix-haymo
    │ ╱
    │╱
    •
    │
    ```

    You can also merge "older to newer" manually, but do not merge "newer to older".
    This would include all newer features and fixes for the newer version in the hotfix release.
    All hotfix branches can be merged to `dev`, but ` dev` should never be merged into a hotfix branch.

9.  Pushing other branches

    If you want to push a "work in progress" branch that should not yet be reviewed,
    just name it any other name that does not end with an issue number (e.g. `wip-my-feature`)
    to prevent GiLab from creating a merge request (which could be reviewed/merged accidentally).
