plugins{
    id("io.github.pluginloader.gradle") version("1.11.10")
}

group = "io.github.pluginloader"
version = "1.0.1"

plu.public()

dependencies{
    implementation("com.charleskorn.kaml:kaml:0.37.0"){
        exclude("org.jetbrains.kotlin")
        exclude("org.jetbrains.kotlinx")
    }
}

plu.includeDep()