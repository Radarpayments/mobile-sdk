# R8 rules applied to the app that uses the SDK.

# play-services-wallet is compileOnly: an app without Google Pay has none of its classes, nor the Play services
# basement and tasks classes it brings. The code that touches them runs only after GooglePayAvailability has found the
# library, so the references may stay unresolved. kotlinx-coroutines-play-services comes without its
# play-services-tasks (excluded in build.gradle.kts) for the same reason.
-dontwarn com.google.android.gms.wallet.**
-dontwarn com.google.android.gms.common.api.**
-dontwarn com.google.android.gms.tasks.**
