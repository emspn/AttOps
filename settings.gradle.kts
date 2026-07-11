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
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AttOps"
include(":app")
include(":core:designsystem")
include(":core:common")
include(":core:network")
include(":core:location")
include(":core:camera")
include(":core:navigation")

// Features
include(":features:auth")
include(":features:employee")
include(":features:dashboard")
include(":features:tasks")
include(":features:reports")
