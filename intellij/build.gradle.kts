plugins {
    id("org.jetbrains.intellij") version "1.3.0"
    id("kotlin")
    id("java")
}

// 添加依赖
dependencies {
    implementation("com.alibaba:fastjson:1.2.75")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${findProperty("kotlin_version")}")
    implementation(project(":common"))
}

intellij {
    pluginName.set("aex")
    version.set("IC-2020.1")//设定依赖的版本
//    localPath '/Applications/StudioAP.app' //当前依赖本地版本
    plugins.set(listOf("android", "git4idea", "java", "gradle"))
    updateSinceUntilBuild.set(false)

}
tasks {
    publishPlugin {
        token.set(findProperty("PUBLISH_TOKEN")?.toString())
        channels.set(listOf("stable"))
    }
}