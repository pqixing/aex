plugins {
    id("kotlin")
    id("java")
}

// 添加依赖
dependencies {
    implementation("com.alibaba:fastjson:1.2.75")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${findProperty("kotlin_version")}")
}
