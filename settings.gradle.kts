rootProject.name = "vuln-core"

pluginManagement {
    resolutionStrategy {
        repositories {
            gradlePluginPortal()
            mavenCentral()
        }
    }
}

include("shared")
include("client")
include("server")