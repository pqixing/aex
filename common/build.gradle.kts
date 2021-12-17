plugins {
    id("kotlin")
    id("java")
}

// 添加依赖
dependencies {
    implementation("com.alibaba:fastjson:1.2.78")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${findProperty("kotlin_version")}")
}
tasks {
    compileJava{
        sourceCompatibility = JavaVersion.VERSION_11.toString()
        targetCompatibility = JavaVersion.VERSION_11.toString()
    }
    compileKotlin.get().kotlinOptions.jvmTarget =JavaVersion.VERSION_11.toString()
}