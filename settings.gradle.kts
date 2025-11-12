pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.box.com/content/repositories/releases/") }
        maven { url = uri("https://maven.microsoft.com/maven2") }
    }
}

rootProject.name = "EBook Reader"
include(":app")