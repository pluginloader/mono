//ver 1
plugins{
    id("org.hidetake.ssh") version "2.10.1"
    id("maven-publish")
}

if(project.properties["paperVersion"] != null) {
    rootProject.repositories.add(rootProject.repositories.maven{url = uri("https://papermc.io/repo/repository/maven-public/")})
    rootProject.dependencies.add("compileOnly", "pluginloader:bukkit-api:${project.properties["pluginloaderVersion"]}")
    rootProject.dependencies.add("compileOnly", "com.destroystokyo.paper:paper-api:${project.properties["paperVersion"]}-R0.1-SNAPSHOT")
}

rootProject.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>{kotlinOptions.jvmTarget = "1.8"}
rootProject.tasks.withType<org.gradle.jvm.tasks.Jar>{archiveFileName.set("${rootProject.name}.jar")}

task("upload"){
    dependsOn(":build")
    doLast {
        ssh.run(delegateClosureOf<org.hidetake.groovy.ssh.core.RunHandler> {
            session(org.hidetake.groovy.ssh.core.Remote(
                mapOf<String, Any>(
                    "host" to System.getenv("PLU_PUSH_HOST"),
                    "port" to System.getenv("PLU_PUSH_PORT").toInt(),
                    "user" to System.getenv("PLU_PUSH_USER"),
                    "agent" to true
                )), delegateClosureOf<org.hidetake.groovy.ssh.session.SessionHandler> {
                put(hashMapOf("from" to "build/libs/${rootProject.name}.jar", "into" to "${project.properties["p"]}"))
            })
        })
    }
}

task("rename") {
    doLast {
        val newProjectName = File("").absoluteFile.name
        val classPackage = newProjectName.replace("-", "_")
        val pl = File("src/main/kotlin/Plugin.kt")
        pl.writeText(pl.readText().replace(rootProject.name, classPackage))
        val settings = File("settings.gradle.kts")
        settings.writeText(settings.readText().replace(rootProject.name, newProjectName))
    }
}

if(System.getenv("PLU_PUBLIC_URL") != null) {
    rootProject.publishing {
        publications{create<MavenPublication>("maven"){from(rootProject.components["java"])}}

        repositories {
            maven {
                url = uri(System.getenv("PLU_PUBLIC_URL"))
                credentials {
                    username = System.getenv("PLU_PUBLIC_PUSH_USER")
                    password = System.getenv("PLU_PUBLIC_PUSH_PASSWORD")
                }
            }
        }
    }
}

rootProject.tasks.getByPath("jar").doFirst{
    val dir = rootProject.file("build/classes/kotlin/main/pluginloader")
    if(dir.exists())dir.deleteRecursively()
    rootProject.configurations.getByName("dependency").allDependencies.forEach{
        val f = rootProject.file("build/classes/kotlin/main/pluginloader/${it.name}.dependency")
        f.parentFile.mkdirs()
        f.createNewFile()
        f.writeText("${it.group}:${it.name}:${it.version}")
    }
    rootProject.configurations.getByName("mavenDependency").allDependencies.forEach{
        val f = rootProject.file("build/classes/kotlin/main/pluginloader/${it.group};${it.name};${it.version}.mavenDependency")
        f.parentFile.mkdirs()
        f.createNewFile()
    }
}

rootProject.repositories.add(rootProject.repositories.mavenLocal())
rootProject.repositories.add(rootProject.repositories.mavenCentral())

val config = rootProject.configurations.create("dependency")
config.isTransitive = false
rootProject.configurations.getByName("compileClasspath").extendsFrom(config)

rootProject.dependencies.add("compileOnly", "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${rootProject.properties["kotlinVersion"]}")
rootProject.dependencies.add("compileOnly", "org.jetbrains.kotlinx:kotlinx-serialization-core:${rootProject.properties["kotlinSerializationVersion"]}")
rootProject.dependencies.add("compileOnly", "org.jetbrains.kotlinx:kotlinx-serialization-json:${rootProject.properties["kotlinSerializationVersion"]}")
rootProject.dependencies.add("compileOnly", "pluginloader:api:${rootProject.properties["pluginloaderVersion"]}")

val mvnDependency = rootProject.configurations.create("mavenDependency")
mvnDependency.isTransitive = false
rootProject.configurations.getByName("compileClasspath").extendsFrom(mvnDependency)