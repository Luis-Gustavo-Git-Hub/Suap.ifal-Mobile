// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
  alias(libs.plugins.androidx.baselineprofile) apply false
  alias(libs.plugins.android.test) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
  dependencyLocking {
    lockAllConfigurations()
  }

  configurations.configureEach {
    resolutionStrategy.eachDependency {
      when ("${requested.group}:${requested.name}") {
        "com.google.guava:guava" -> {
          val variant = if (requested.version.orEmpty().endsWith("-android")) "android" else "jre"
          useVersion("33.7.2-$variant")
        }
        "org.bouncycastle:bcprov-jdk18on" -> useVersion("1.85")
        "com.squareup.wire:wire-runtime",
        "com.squareup.wire:wire-runtime-jvm" -> useVersion("6.4.5")
      }
    }
  }
}
