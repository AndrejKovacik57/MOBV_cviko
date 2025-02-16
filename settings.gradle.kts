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
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = "sk.eyJ1IjoieGtvdmFjaWthMSIsImEiOiJjbTJrcjN2ZnkwM21sMnFzOXFlbW8yYmVtIn0.lAiKsHZ949wxI9N8yGpkXA"
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

}

rootProject.name = "MOBV cviko1"
include(":app")
