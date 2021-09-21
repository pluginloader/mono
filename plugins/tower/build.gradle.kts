plugins{
    id("pluginloader.gradle") version("1.10.0")
}

group = "pluginloader"
version = "1.0.0"

//plu.paper("1.12.2")
//plu.private()
plu.plu("configs")
plu.public()

dependencies{
    mavenDependency("io.netty:netty-buffer:4.1.51.Final")
    mavenDependency("io.netty:netty-codec:4.1.51.Final")
    mavenDependency("io.netty:netty-common:4.1.51.Final")
    mavenDependency("io.netty:netty-resolver:4.1.51.Final")
    mavenDependency("io.netty:netty-transport:4.1.51.Final")
    mavenDependency("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.2.0")

    testApi("io.netty:netty-buffer:4.1.51.Final")
    testApi("io.netty:netty-codec:4.1.51.Final")
    testApi("io.netty:netty-common:4.1.51.Final")
    testApi("io.netty:netty-resolver:4.1.51.Final")
    testApi("io.netty:netty-transport:4.1.51.Final")
    testApi("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.2.0")
    testApi("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.0")
    testApi("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.0")
}