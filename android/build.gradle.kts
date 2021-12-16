plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "0.16.0"
}
group = "io.github.pqixing"
version = "0.0.1"
pluginBundle {
    website = "https://github.com/pqixing/aex"
    vcsUrl = "https://github.com/pqixing/aex"
    tags = listOf("intellij", "pqixing", "module", "project")
}

gradlePlugin {
    plugins {
        create("apx") {
            id = "io.github.pqixing.profm"
            displayName = "io.github.pqixing.profm"
            description = "Manage framework for multi projects,fast to handle git control and maven support!"
            implementationClass = "com.pqixing.profm.setting.XSetting"
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


val isPublish = gradle.startParameter.taskNames.toString().contains("publish")
// 添加依赖
dependencies {
    implementation(gradleApi())
    implementation("com.alibaba:fastjson:1.2.78")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.0.0.202111291000-r")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${findProperty("kotlin_version")}")
    if (isPublish) sourceSets.main.get().java.srcDir("$rootDir/common/src/main/java") else compileOnly(project(":common"))
}