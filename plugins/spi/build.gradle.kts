plugins{
    id("pluginloader.gradle") version("1.10.0")
}

group = "pluginloader"
version = "1.0.0"

plu.paper("1.12.2")
//plu.private()
plu.plu("configs", "tower")
plu.public()

dependencies{
    dependency(project(":leveldb"))
    compileOnly("org.iq80.leveldb:leveldb:0.12")
}