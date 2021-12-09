plugins {
    id("kotlin")
    id("java")

    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "0.16.0"
}

group = "com.pqixing"
version = "0.0.1"

pluginBundle {
    website = "https://github.com/pqixing/aex"
    vcsUrl = "https://github.com/pqixing/aex"
    tags = listOf("intellij", "pqixing", "module", "project")
}

gradlePlugin {
    plugins {
        create("apx") {
            id = "com.pqixing.apx"
            displayName = "com.pqixing.apx"
            description = "help"
            implementationClass = "com.pqixing.aex.setting.XSetting"
        }
    }
}
publishing {
    repositories {
        maven {
            name = "pqx"
            url = uri("../build/repo")
        }
    }
}


// 添加依赖
dependencies {

    implementation("org.eclipse.jgit:org.eclipse.jgit:5.6.1.202002131546-r")
    implementation(gradleApi())
    implementation("com.alibaba:fastjson:1.2.75")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${findProperty("kotlin_version")}")

    if (gradle.startParameter.taskNames.toString().contains("publish")) {
        sourceSets.main { java.srcDir("../common/src/main/java") }
        println("include sourceSet srcDir common")
    } else {
        implementation(project(":common"))
    }
}
