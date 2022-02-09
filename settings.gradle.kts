include(":android",":intellij",":common")

include(":intellij:libs")

pluginManagement {
    repositories {
        maven {
            setUrl("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        gradlePluginPortal()
    }
}