plugins {
    id("org.jetbrains.intellij") version "1.3.0"
    id("kotlin")
    id("java")
}

// 添加依赖
dependencies {
    api("com.alibaba:fastjson:1.2.78")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${findProperty("kotlin_version")}")
    api(project(":common"))
    runtimeOnly(fileTree("dir" to "$projectDir/libs", "include" to arrayOf("*.jar")))

}

intellij {
    pluginName.set("aex")
    version.set("IC-2020.1")//设定依赖的版本
//    localPath.set("/Applications/StudioBeta.app") //当前依赖本地版本
//    localPath.set("/Applications/IntelliJ IDEA CE.app") //当前依赖本地版本
    plugins.set(listOf("android", "git4idea", "java", "gradle"))
    updateSinceUntilBuild.set(false)

}
tasks {
    publishPlugin {
        token.set(findProperty("PUBLISH_TOKEN")?.toString())
        channels.set(listOf("stable"))
    }
    compileJava {
        sourceCompatibility = JavaVersion.VERSION_11.toString()
        targetCompatibility = JavaVersion.VERSION_11.toString()
    }
    compileKotlin.get().kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
}