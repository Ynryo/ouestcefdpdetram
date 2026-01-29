plugins {
    alias(libs.plugins.android.application) apply false
}
val debugApplicationIdSuffix by extra("release")
