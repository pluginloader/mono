plugins{
    id("io.github.pluginloader.gradle") version("1.14.0")
}

group = "io.github.pluginloader"
version = "1.0.0"

plu.public()

dependencies{
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.3.2"){
        exclude("org.jetbrains.kotlin")
        exclude("org.jetbrains.kotlinx")
    }
}

plu.includeDep()