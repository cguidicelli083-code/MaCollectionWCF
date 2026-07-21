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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Recadrage de photo (CanHub/Android-Image-Cropper), distribué via JitPack.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MaCollectionWCF"
include(":app")
