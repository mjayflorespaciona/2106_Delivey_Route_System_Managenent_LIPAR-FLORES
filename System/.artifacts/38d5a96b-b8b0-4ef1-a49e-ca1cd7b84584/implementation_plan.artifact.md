# RoutGo Role-Based Activity & Fragment Separation Implementation Plan

Separating all activities, fragments, and navigation flows into 3 isolated role packages: `customer`, `rider`, and `admin` to ensure zero screen overlaps and strict role-based access control.

## User Review Required

> [!IMPORTANT]
> - **Zero Screen Overlaps**: Customer, Rider, and Admin screens will use dedicated fragments and activities tailored exclusively for each role, eliminating shared component visibility logic.
> - **Role Security Guards**: Activity lifecycle checks (`verifyAdminSecurity()`, `verifyRiderSecurity()`) enforce strict access control so unauthorized role attempts are blocked instantly.

## Proposed Changes

### 1. Package Reorganization & Role Separation

#### [NEW] `com.example.delitrack.frontEnd.customer`
- **`CustomerDashboardActivity.kt`**: Customer portal container.
- **`CustomerHomeFragment.kt`**: Customer dashboard (welcome message, delivery counters, recent activities).
- **`CustomerDeliveriesFragment.kt`**: Customer package requests list (`item_customer_delivery_card.xml`).
- **`CustomerProfileFragment.kt`**: Customer profile management & password reset.

#### [NEW] `com.example.delitrack.frontEnd.rider`
- **`RiderDashboardActivity.kt`**: Rider portal container.
- **`RiderHomeFragment.kt`**: Rider home (duty toggle, delivery statistics, assigned job cards).
- **`RiderDeliveriesFragment.kt`**: Rider assigned job list with action controls (*Accept, In Transit, Delivered, Failed, Issue*).
- **`RiderMapFragment.kt`**: Nasugbu–Lian GIS route map with rider GPS sharing controls.
- **`RiderProfileFragment.kt`**: Rider profile displaying Rider ID, assigned vehicle, and duty status.

#### [NEW] `com.example.delitrack.frontEnd.admin`
- **`AdminDashboardActivity.kt`**: Admin portal container.
- **`AdminHomeFragment.kt`**: Admin control overview (live delivery, rider, and vehicle counters).
- **`AdminDeliveriesFragment.kt`**: Admin order dispatch list with controls (*Confirm, Assign, Prepare, Reject, Receipt*).
- **`AssignRiderVehicleActivity.kt`**: Fleet management, rider/vehicle CRUD, cargo capacity & weight validation.
- **`AdminCustomerManagementActivity.kt`**: Customer account control (search, edit, history, activate/deactivate).
- **`AdminGpsMonitoringActivity.kt`**: Live rider GPS monitoring dashboard.
- **`AdminMapFragment.kt`**: Admin GIS route map with interactive delivery markers & info dialogs.
- **`ReportsFragment.kt`**: Delivery reports, date filters, PDF & CSV export.
- **`AdminProfileFragment.kt`**: Admin profile & sign out.

### 2. Dedicated Item Card Snippets

#### [NEW] [item_customer_delivery_card.xml](file:///C:/Users/Aron/Desktop/SystemRoute/System/app/src/main/res/layout/item_customer_delivery_card.xml)
- Dedicated card for customer views (no rider/admin action buttons).

#### [NEW] [item_rider_delivery_card.xml](file:///C:/Users/Aron/Desktop/SystemRoute/System/app/src/main/res/layout/item_rider_delivery_card.xml)
- Dedicated card for rider views (*Accept, In Transit, Delivered, Failed, Issue*).

#### [NEW] [item_admin_delivery_card.xml](file:///C:/Users/Aron/Desktop/SystemRoute/System/app/src/main/res/layout/item_admin_delivery_card.xml)
- Dedicated card for admin views (*Confirm, Assign, Prepare, Reject, Receipt*).

### 3. Manifest & Login Navigation

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Aron/Desktop/SystemRoute/System/app/src/main/AndroidManifest.xml)
- Register role-separated activities.

#### [MODIFY] [AdminLoginActivity.kt](file:///C:/Users/Aron/Desktop/SystemRoute/System/app/src/main/java/com/example/delitrack/frontEnd/AdminLoginActivity.kt)
- Direct user to role portal based on Firestore `role` field.

## Verification Plan

### Automated Tests
- Run Gradle build (`app:assembleDebug`) to verify clean compilation across all role packages.

### Manual Verification
- Log in as Customer ➔ Verify pure Customer screens only.
- Log in as Rider ➔ Verify pure Rider screens only.
- Log in as Admin ➔ Verify pure Admin screens only.
