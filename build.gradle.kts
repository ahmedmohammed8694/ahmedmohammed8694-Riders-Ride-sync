// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
    configurations.all {
        resolutionStrategy {
            force("androidx.tracing:tracing:1.2.0")
            force("androidx.tracing:tracing-ktx:1.2.0")
            eachDependency {
                if (requested.group == "androidx.tracing") {
                    useVersion("1.2.0")
                    because("Force androidx.tracing to valid version 1.2.0 as 1.1.0 does not exist on Google Maven")
                }
            }
        }
    }
}
