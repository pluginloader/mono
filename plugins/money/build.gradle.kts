plugins{
    id("io.github.pluginloader.gradle") version("2.0.1")
}

repositories {
    maven{url = uri("https://jitpack.io")}
}

dependencies {
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
}

group = "io.github.pluginloader"
version = "1.0.0"

plu.paper("1.12.2")
plu.central("provide")
plu.public()