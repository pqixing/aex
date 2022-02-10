plugins {
    id("kotlin")
    id("java")
}

val sourceType = "bumblebee"

//兼容措施
val dirs = mapOf("bumblebee" to "/Applications/Studio.app/Contents")
val dir = dirs[sourceType]

sourceSets.main {
    java.srcDir("$projectDir/src/main/$sourceType")
}

tasks {
    compileJava {
        sourceCompatibility = JavaVersion.VERSION_11.toString()
        targetCompatibility = JavaVersion.VERSION_11.toString()
    }
    compileKotlin.get().kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    jar.get().doLast {
        File(buildDir, "libs/${project.name}.jar").copyTo(File(projectDir, "${sourceType}.jar"), true)
    }
}

// 添加依赖
dependencies {
    compileOnly(fileTree("dir" to "$dir/plugins/android/lib", "include" to arrayOf("*.jar")))
    compileOnly(fileTree("dir" to "$dir/plugins/gradle/lib", "include" to arrayOf("*.jar")))
    compileOnly(fileTree("dir" to "$dir/lib", "include" to arrayOf("*.jar")))
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${findProperty("kotlin_version")}")
    compileOnly(project(":intellij"))
}