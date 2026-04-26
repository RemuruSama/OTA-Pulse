# Walkthrough - Update Available UI and View Button

I have updated the main update screen to show "Update Available" with the version name when a new update is found. Additionally, I added a "View" button that allows users to quickly open the OTA details dialog.

## Changes

### Resources

#### [strings.xml](file:///C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/res/values/strings.xml)

- Added `update_available_label` and `btn_view` strings.

#### [fragment_home_update.xml](file:///C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/res/layout/fragment_home_update.xml)

- Reverted the "View" button's position to the top-right of the "Update Available" section.
- Aligned the "UPDATE AVAILABLE" label with the button.
- The version string (`tv_update_version_value`) is now placed on its own line below both the label and the button.
- Maintained `singleLine="true"` and `ellipsize="end"` for the version text to ensure it remains on one line and adapts nicely.
- Kept the compact 32dp height for the button to ensure it doesn't push the layout too far down.
- **Visual Effects**: Added `android:stateListAnimator="@animator/bounce_animation"` to the "View" button. This provides a snappy "shrink-and-pop" interaction when clicked, consistent with other primary actions in the app.

### Logic Refinements

#### [OtaToolsViewModel.kt](file:///C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/otatools/ui/OtaToolsViewModel.kt)

- Removed the automatic setting of `showOtaDetailsDialog` when a multi-server search finishes.
- This ensures that checking for updates only reveals the update section in the card, without interrupting the user with a popup dialog until they click "View".

### UI Components

#### [HomeUpdateFragment.kt](file:///C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/updates/ui/HomeUpdateFragment.kt)

- Updated `observeViewModel` to show the "Update Available" section when a successful update result is received.
- Set the version name from the `OtaUpdate` result to `tvUpdateVersionValue`.
- Configured the `btnViewUpdate` button to open the OTA details dialog.
- Ensured the section is hidden during loading or when an error occurs.

## Verification Summary

### Manual Verification
- Verified that the "Update Available" section correctly appears only when an update is found.
- Confirmed that the "View" button successfully opens the detailed OTA dialog.
- Checked that the UI remains clean and hides the update section when re-checking or in case of errors.
