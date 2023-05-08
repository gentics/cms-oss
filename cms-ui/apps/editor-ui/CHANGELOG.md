# Gentics CMS UI Changelog

## 1.15.0 (2017-12-20)

### Features

* Allow switching editor to fullscreen (GCU-304)
* Add ability to localize more items at once (GCU-389)
* Improve undo delete with restoring localized items of child node and show combined notifications (GCU-287)
* Add Keycloak SSO support (GCU-387)
* Add API for customer-defined JavaScript (see DEVELOPER_GUIDE.md) (GCU-402)
* Add paging support to repository browser (GCU-405, SUP-5320)
* Add link on 'inherited from' icon to open master item in editor (GCU-393)
* Add link in the editor to navigate to parent folder of edited item (GCU-394)
* Icons 'inherited' and 'localized' are present in the content frame (GCU-392)
* Improve version restoring and update currently opened version when restored (GCU-360)
* Implement 'always localize' feature (GCU-383)

### Fixes

* Refresh/change folder after copying items (GCU-406, SUP-5293)
* Item list is refreshed after restoring from wastebin with an active search (GCU-348)
* Fix error messages caused by iframe handling (GCU-409)
* Close the editor after unlocalizing edited item (GCU-418)
* Fix unresponsive publish queue when there are many nodes (GCU-395)
* Fix old file picker being used for the Aloha gcnfileupload plugin (GCU-358)
* Internal links correctly navigate when clicked (SUP-5089)
* Enable users with no template permissions to create pages (GCU-245)


## 1.13.2 (2017-12-20)

### Features

* Show confirm delete modal if user doesn't have wastebin permissions (GCU-127)
* Allow user to sort wastebin contents (GCU-258)
* Add paging support to repository browser (GCU-405, SUP-5320)
* Improve version restoring of current item in editor (GCU-360)
* Allow filtering repository browser by page ID (GCU-390)
* Add API for customer-defined JavaScript (see DEVELOPER_GUIDE.md) (GCU-402)
* Add ability to localize more items at once (GCU-389)
* Implement 'always localize' feature (GCU-383)

### Fixes

* Correctly fetch template information when working in a channel (GCU-379)
* Remove redundant API calls when navigating
* Searching for a page ID no longer returns irrelevant results (SUP-5184)
* Allow clearing dates in the time management modal (GCU-355)
* Always compare new to old when comparing page versions (GCU-378)
* Filter/search in repository browser can be cleared (GCU-377)
* Fix size of search bar chip with long inputs (GCU-396)
* Trigger layout of MasonryGrid when opening repo browser
* Refresh/change folder after copying items (GCU-406, SUP-5293)
* Refresh ItemList after restoring from wastebin with an active search (GCU-348)
* Enable users with no template permissions to create pages (GCU-245)
* Fix repository browser selection for "image" overviews (GCU-415, SUP-5243)
* Improve undo delete with restoring localized items of child node and show combined notifications (GCU-287)
* Fix error messages caused by iframe handling (GCU-409)
* Fix old file picker being used for the Aloha gcnfileupload plugin (GCU-358)
* Internal links correctly navigate when clicked (SUP-5089)* Fix unresponsive publish queue when there are many nodes (GCU-395)


## 1.14.0 (2017-10-31)

### Features

* Elastic integration with advanced search filters (available with CMS v5.29.0) (GCU-376)
* Extend usage information with outbound links (available with CMS v5.29.0) (GCU-385)
* Show confirm delete modal if user doesn't have wastebin permissions (GCU-127)
* Allow clearing dates in the time management modal (GCU-355)
* Allow sorting of wastebin contents (GCU-258) 

### Fixes

* Correctly fetch template information when working in a channel (GCU-379)
* Eliminate redundant API calls when navigating.


## 1.13.1 (2017-10-16)

### Fixes

* Fix runtime error introduced in 1.13.0 which prevents date pickers in the app from working.
* Fix regression introduced in 1.13.0 which did not persist last URL on refresh.


## 1.13.0 (2017-10-10)

### Features

* Node selectors are filterable when there are many nodes (GCU-368)
* On login, the UI starts from the last-used node (SUP-5079)

### Fixes

* Fix item selection logic in list (SUP-5080, GCU-381)
* Fix dropdown menus not using available space


## 1.12.0 (2017-09-13)

### Features

* Repository browser supports selecting items from user favourites (GCU-314)
* Opening an item in the editor panel highlights it in the list (GCU-371)
* Node and base folder properties can now be edited (GCU-361)


### Fixes

* Close editor when user deletes opened item (GCU-340, SUP-4163)
* Correctly update page status after publishing (SUP-4933)
* Fix taking pages offline where there are no language variants (SUP-4895)
* Fix runtime error in item list (GCU-373)
* Performance improvements (GCU-374)
* Improve handling of unknown nodes and folders (GCU-375)

### Refactoring
* Refactor app into lazy-loaded feature modules (GCU-374)


## 1.11.0 (2017-08-22)

### Features
* (for developers) Expose a GCMSUI object in the iframe window, allowing interaction with certain UI features (SUP-4754)
* Do not hide display fields when right panel is open (GCU-308)
* Redirect to previous URL on login (GCU-370)

### Fixes
* UI now uses ahead-of-time compilation for improved load times (GCU-364)
* Fix language comparison mode (GCU-365)
* Fix error when searching by page id for a localized page (SUP-4411)


## 1.10.0 (2017-07-24)

### Features
* Support for nice urls (GTXPE-79, GCU-362)

### Fixes
* Fix height of multi-select inputs in object properties form (GCU-359)
* Clicking Gentics logo clears out the search term (GCU-356)
* Fix unwanted switching of SplitViewContainer in IE11 (GCU-332, WKUM-894)
* Fix page scrolling issue in IE11 (SUP-4690)


## 1.9.0 (2017-07-12)

### Features
* Display a notification when maintenance mode is active (GCU-306)

### Fixes
* Don't prevent events from customer scripts in edit frame (GCU-353, SUP-4338)
* Don't mark pages as locked when previewing or without edit permissions (SUP-4483)
* Fix behavior and styling of "synchronize channel with master" modal (SUP-4589)


## 1.8.3 (2017-06-12)

### Fixes
* Use correct item permissions when searching (GCU-347, SUP-4279)
* Only send one API request for permissions at a time
* Open page preview when clicking a page in favourites list (SUP-4432)
* Fix page status after publishing pages with time management (SUP-4346)
* Fix inbox message parsing for edge-cases (GCU-344)
* Fix node id parameter in page edit & preview


## 1.8.2 (2017-05-23)

### Fixes
* Add error message when new passwords do not match
* Fix missing scrollbars in sidebar "messages" and "favourites" components in FF & IE11
* Fix broken api error detection


## 1.8.1 (2017-04-21)

### Fixes

* Mark page as changed after submitting tagfill dialog (GCU-331)
* Enable publish button when page has unsaved changes (GCU-336)
* Don't show "save/discard changes?" modal after publishing (GCU-333)
* Display better error messages for unexpected server errors (GCU-326)


## 1.8.0 (2017-04-12)

### Features
* Increase performance and responsiveness across all browsers (GCU-248)
* Enable deep-linking of objects open in the ContentFrame (GCU-277)
* Improved confirmation modals when attempting to navigate away from unsaved changes (GCU-277)
* Display checkboxes when list items and headers are hovered (GCU-318, GCU-320)
* Display page status in repository browser (GCU-311)

### Fixes
* Fix broken custom scripts in iframes for long-loading pages (GCU-293, GCU-294)
* Fix missing cancel button in certain tagfill dialogs (GCU-295)
* Fix time management modal inconsistency in edge-cases (GCU-259)
* Display correct creation & edit data in file/image preview (GCU-315)
* Fix file upload issues for Firefox 45 (GCU-297)
* Repair breadcrumb bar behavior on different viewport sizes (GCU-309)
* Fix link handling in ContentFrame (GCU-310)
* Fix jumping item list when using pagination (GCU-319)
* Improve visibility of folder start page icon, include it in repository browser (GCU-301)
* Improve arrangement of ContentFrame buttons & menu items (GCU-305)
* Pages close automatically after publishing (GCU-305)
* Scroll folder list to the top on navigation to a new folder (GCU-312)
* More descriptive error messages for failed file uploads (GCU-299, GCU-315)


## 1.7.2 (2017-02-20)

### Fixes
* Pre-select the first available template when creating pages
* Fix application freezes in complex multichanneling scenarios (GCU-290)
* Synchronizing channels fixed for multi-inheritance scenarios (GCU-291)
* Fix regression in image editing interface (GCU-284)
* Fix display of long image file names in Firefox (GCU-292)
* Fix caching of outdated app.css file by browsers (GCU-289)
* Login form no longer displayed if the user has a valid session (GCU-288)
* Navigation no longer resets after refreshing (GCU-270)


## 1.7.1 (2017-02-09)

### Fixes
* Fix delete modal for pages which are localized in an inheriting channel (GCU-282)
* Fix/Improve modal styling in Internet Explorer (GCU-260, GCU-279, GCU-283)
* Fix sort order of display fields (GCU-285)


## 1.7.0 (2017-02-06)

### Features
* Localized objects are now indicated with an icon (GCU-262)
* User is warned when attempting to delete an object which has localizations (GCU-273)
* More granular control when deleting page language variants

### Fixes
* Improvements to stability of editing panel (GCU-271, GCU-272)
* Fix incorrect dialog and disabled tabs when editing properties (GCU-274)
* Fix properties forms being initially dirty and sending incorrect values (GCU-275)
* Fix multiple small styling issues


## 1.6.2 (2017-01-27)

### Fixes
* Changing the template of an existing page was not indicated in the UI (GCU-266)
* Fix freezing editing panel after continuous navigation (GCU-267)
* Resolve numerous small stying issues


## 1.6.1 (2017-01-25)

### Fixes
* Choosing template during page creation not persisted on server (WMQS-228)


## 1.6.0 (2017-01-23)

### Breaking Changes
* Upgrade to Angular 2.4.3

### Fixes
* Fix occasional blank page after using tagfill (GCU-261)
* Fix select controls being cut off in "Create Page" dialog (GCU-252)
* Login/create forms receive focus and submit on enter (GCU-192, GCU-143)


## 1.5.0 (2017-01-04)

### Features
* Repository browser disallows copy/move when user lacks permission (GCU-150)
* Publish queue, message inbox and other UI elements react to user permissions (GCU-165)
* Paginate contents of wastebin (GCU-253)

### Fixes
* Template select menu no longer inaccessible when creating page (GCU-252)
* User storage works for CMS versions before 5.26.0 using localStorage
* Move/Copy/Publish buttons now correctly shown/hidden with permissions (GCU-249)
* Double clicking objects in FolderContents list no longer results in blank page (GCU-227, GCU-255)
* Fix editor panel not always closing immediately
* Fix incorrect linking of object paths (GCU-256)
* Fix blank page in IE11 after editing a page (GCU-254)


## 1.4.0 (2016-12-19)

### Features
* User settings are saved on the server (GCU-228)
* Copy/move/publish/delete/take offline multiple items at once (GCU-231)

### Fixes
* Incorrect page filename sanitization switched off (GCU-235)
* Context menu is now scrollable at small resolutions (GCU-222)
* New tag fill dialogs and new repo browser now work in IE 11 (GCU-234)
* Repo browser and other modal dialogs now display correctly in Firefox (GCU-230)
* Reduce performance drop after visiting multiple folders and items (GCU-180)
* Fix various iframe bugs in Firefox (GCU-246)
* Fix unintended caching of app scripts by browsers (GCU-251)
* Fix checkbox being checked when uploading file in tagfill (GCU-247)


## 1.3.0 (2016-12-01)

### Features
* Repository browser supports searching for live URLs (GCU-209)
* Show preview/edit frame in fullscreen on small screens (GCU-181)
* File & image binaries can be replaced by dragging and dropping new files (GCU-212)
* Time management available from page editor menu (GCU-219)
* Quick jump to page by searching page id (GCU-216)
* Make item folders clickable when searching (GCU-220)
* Favourites in localStorage saved & loaded unique per user (GCU-128)
* Add favourite button for current folder (GCU-142)
* DateTimePicker localized to german and english (GCU-170)

### Fixes
* Message inbox no longer throws error for system messages with no sender (GCU-215)
* Display error message for every failed login attempt (GCU-207)
* Viewing file/image properties will open to correct tab (GCU-158)
* File drop overlay positioned correctly when folder contents are scrolled
* Display "list tools" select menu in tag fill dialogs (GCU-218)
* Wastebin modal styled correctly in IE 11 (GCU-223)
* Revert to using "alohapage" servlet for page previews (GCU-221)


## 1.2.3 (2016-11-08)

### Fixes
* Fix typo in 1.2.2 (GCU-214)


## 1.2.2 (2016-11-08)

### Fixes
* URL handling works for more customer configurations (GCU-214)


## 1.2.1 (2016-10-28)

### Fixes
* Fix Image upload in Tagfill dialogues (GCU-208)
* Display tagfill dialog above sidebar (GCU-210) 


## 1.2.0 (2016-10-25)

### Features
* Search-and-filter box in repository browser (GCU-138)
* Default replacement character for pages changed to "-" from "_" (GCU-144)
* Paths are now relative to the UI path to support path prefixes (GCU-205)

### Fixes
* Prevent exception when cancelling an inheritance decision modal
* Fix errors with iframe `progress` method when url is undefined (GCU-202)
* Fix regression with inserting new image into page via tagfill (GCU-200)
* Fix tagfill image dialog in IE (GCU-200)
* Fix "drop file to upload" feature (GCU-182)
* Prevent exception when canceling an inheritance decision modal
* Fix object property saving for textareas and selects (GCU-201)


## 1.1.0 (2016-10-12)

### Features
* API errors logged more verbosely and handled separately (GCU-99)
* Maintenance mode forces logout of current user (GCU-168)
* Creating a page opens it in edit mode (GCU-109)
* Add "take offline" option to ContentFrame menu (GCU-177)
* Wastebin items can be restored/deleted in bulk, language variant granularity (GCU-166)
* Node selectors now indicate inheritance hierarchy (GCU-189)
* Highlight filter matches in ItemList (GCU-80)

### Fixes
* Page language indicator was missing after copying page (GCU-183)
* Create page behavior broke when navigating in background (GCU-187)
* Fix ItemList items not updating to reflect state changes (GCU-188)
* Fix ok/cancel buttons in Tagfill for IE 11 (GCU-190)
* Fix issue with switching to new item before first has loaded (GCU-147)
* Fix exception when pages contain embedded cross-origin iframes (GCU-191)
* Instant publishing fixed for single-page publishing (GCU-193)
* Fix caching issues with IE (GCU-194)
* Fix object properties table layout in IE


## 1.0.0
### Initial release


## 0.9.0

### Features
* Versioning system implemented
