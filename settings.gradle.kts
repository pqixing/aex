include(":android",":intellij",":common")

//include(":intellij_lib")

pluginManagement {
    repositories {
        maven {
            setUrl("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        gradlePluginPortal()
    }
}