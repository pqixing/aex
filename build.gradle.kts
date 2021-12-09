// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        maven { setUrl("https://maven.aliyun.com/nexus/content/groups/public") }
        maven { setUrl("https://maven.aliyun.com/nexus/content/repositories/jcenter") }
        maven { setUrl("https://maven.aliyun.com/repository/central") }
        maven { setUrl("https://maven.aliyun.com/repository/google") }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${findProperty("kotlin_version")}")
        classpath("com.android.tools.build:gradle:3.6.4")
    }
}

allprojects {
    repositories {
        maven { setUrl("https://maven.aliyun.com/nexus/content/groups/public") }
        maven { setUrl("https://maven.aliyun.com/nexus/content/repositories/jcenter") }
        maven { setUrl("https://maven.aliyun.com/repository/central") }
        maven { setUrl("https://maven.aliyun.com/repository/google") }
    }
}
