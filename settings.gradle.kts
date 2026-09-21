pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CampusDesk"
include(":app")
include(":networking")
include(":shared-ui")
include(":auth")
include(":home")
include(":courses")
include(":attendance")
include(":profile")
include(":archer-sdk")
project(":archer-sdk").projectDir = file("vendor/archer-android/sdk")
