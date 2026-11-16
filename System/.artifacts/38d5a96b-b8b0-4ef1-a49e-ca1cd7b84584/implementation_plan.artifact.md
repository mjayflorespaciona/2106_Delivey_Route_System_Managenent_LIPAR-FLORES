# RoutGo Application Logo Integration Plan

Integrate the user's `@drawable/app_logo` (`app_logo.png`) image across the RoutGo application as the official app icon and header logo across Customer, Rider, and Admin portals.

## User Review Required

> [!IMPORTANT]
> - **Logo Source Asset**: Uses the user's provided `@drawable/app_logo` (`app_logo.png`) asset located in `app/src/main/res/drawable/`.
> - **Application Launcher Icon**: Configures `@drawable/app_logo` as the Android launcher icon (`android:icon` and `android:roundIcon`).
> - **In-App Header Logos**: Updates Customer, Rider, Admin, Login, Registration, and Splash screen layouts to display `@drawable/app_logo`.

## Proposed Changes

### Android Manifest & App Launcher

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Aron/Desktop/SystemRoute/System/app/src/main/AndroidManifest.xml)
- Set `android:icon="@drawable/app_logo"` and `android:roundIcon="@drawable/app_logo"`.

### Layout XML Integration

#### [MODIFY] [activity_splash.xml](file:///C:/Users/Aron/Desktop/SystemRoute/System/app/src/main/res/layout/activity_splash.xml)
- Display `@drawable/app_logo` as the central splash branding logo.

#### [MODIFY] [fragment_home.xml](file:///C:/Users/Aron/Desktop/SystemRoute/System/app/src/main/res/layout/fragment_home.xml)
- Update Customer Dashboard header logo to use `@drawable/app_logo`.

#### [MODIFY] [fragment_rider_home.xml](file:///C:/Users/Aron/Desktop/SystemRoute/System/app/src/main/res/layout/fragment_rider_home.xml)
- Update Rider Dashboard header logo to use `@drawable/app_logo`.

#### [MODIFY] [fragment_admin_home.xml](file:///C:/Users/Aron/Desktop/SystemRoute/System/app/src/main/res/layout/fragment_admin_home.xml)
- Update Admin Dashboard header logo to use `@drawable/app_logo`.

#### [MODIFY] [activity_admin_login.xml](file:///C:/Users/Aron/Desktop/SystemRoute/System/app/src/main/res/layout/activity_admin_login.xml) & [activity_create_account.xml](file:///C:/Users/Aron/Desktop/SystemRoute/System/app/src/main/res/layout/activity_create_account.xml)
- Update login and registration top headers to display `@drawable/app_logo`.

## Verification Plan

### Automated Tests
- Run Gradle build (`app:assembleDebug`) to verify clean compilation with `@drawable/app_logo`.

### Manual Verification
- Verify the logo renders crisp and clean on the launcher icon, splash screen, and headers for Customer, Rider, and Admin screens.
