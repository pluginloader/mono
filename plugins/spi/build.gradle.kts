plugins{
    id("io.github.pluginloader.gradle") version("1.11.10")
}

group = "io.github.pluginloader"
version = "1.0.0"

plu.paper("1.12.2")
plu.central("configs", "tower")
plu.public()

dependencies{
    dependency(project(":leveldb"))
    compileOnly("org.iq80.leveldb:leveldb:0.12")
}