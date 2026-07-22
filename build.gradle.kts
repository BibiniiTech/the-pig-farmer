// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.app) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.gms.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}
