plugins{
    id("pluginloader.gradle") version("1.10.0")
}

repositories {
    maven{url = uri("https://jitpack.io")}
}

dependencies {
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
}

group = "pluginloader"
version = "1.0.0"

plu.paper("1.12.2")
//plu.private()
plu.plu("provide")
//plu.public()