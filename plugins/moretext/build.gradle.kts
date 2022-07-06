plugins{
    id("io.github.pluginloader.gradle") version("2.0.0")
}

group = "pluginloader"
version = "1.0.0"

plu.paper("1.12.2")

repositories{
    mavenLocal()
}

plu.central("configs", "text:1.0.2-SNAPSHOT")