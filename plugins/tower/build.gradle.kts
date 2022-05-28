plugins{
    id("io.github.pluginloader.gradle") version("1.14.0")
}

group = "io.github.pluginloader"
version = "1.0.0"

plu.central("configs")
plu.public()

val nettyVersion = "4.1.72"

dependencies{
    mavenDependency("io.netty:netty-buffer:4.1.72.Final")
    mavenDependency("io.netty:netty-codec:$nettyVersion.Final")
    mavenDependency("io.netty:netty-common:$nettyVersion.Final")
    mavenDependency("io.netty:netty-resolver:$nettyVersion.Final")
    mavenDependency("io.netty:netty-transport:$nettyVersion.Final")

    testApi("io.netty:netty-buffer:$nettyVersion.Final")
    testApi("io.netty:netty-codec:$nettyVersion.Final")
    testApi("io.netty:netty-common:$nettyVersion.Final")
    testApi("io.netty:netty-resolver:$nettyVersion.Final")
    testApi("io.netty:netty-transport:$nettyVersion.Final")
    testApi("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.0")
    testApi("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.0")
    testApi("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.2.0")

    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.4.2")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}