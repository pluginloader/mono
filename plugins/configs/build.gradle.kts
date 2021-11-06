plugins{
    id("pluginloader.gradle") version("1.11.1")
}

group = "pluginloader"
version = "1.0.0"

//plu.paper("1.12.2")
//plu.private()
//plu.plu("configs")
plu.public()

dependencies{
    mavenDependency("com.charleskorn.kaml:kaml:0.28.0")
}