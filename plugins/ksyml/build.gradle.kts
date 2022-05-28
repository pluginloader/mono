plugins{
    id("io.github.pluginloader.gradle") version("1.14.0")
}

group = "io.github.pluginloader"
version = "1.1.0"

plu.public()

dependencies{
    implementation("org.snakeyaml:snakeyaml-engine:2.3")
    implementation("com.charleskorn.kaml:kaml:0.42.0"){
        exclude("org.jetbrains.kotlin")
        exclude("org.jetbrains.kotlinx")
    }
}

plu.includeDep()