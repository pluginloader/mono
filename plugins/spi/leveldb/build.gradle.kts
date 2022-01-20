plugins{
    id("io.github.pluginloader.gradle")
}

group = "io.github.pluginloader"
version = "1.0.0"

plu.public()

dependencies{
    implementation("org.iq80.leveldb:leveldb:0.12")
}

plu.includeDep()