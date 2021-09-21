plugins{
    id("pluginloader.gradle") version("1.10.0")
}

group = "pluginloader"
version = "1.0.0"//next 1.0.2

plu.paper("1.12.2")
//plu.private()
plu.plu("configs", "provide")
plu.public()