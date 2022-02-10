include(":core",":gui",":model",":core:mock")

//include(":gui:libs")

pluginManagement {
    repositories {
        maven {
            setUrl("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        gradlePluginPortal()
    }
}