plugins {
    `java-library`
    `maven-publish`
}

group = "club.tesseract.minestom"
version = System.getenv("TAG_VERSION")?: "dev"

repositories {
    mavenCentral()
    maven("https://repo.hypera.dev/snapshots/") // luckperms (minestom)
}

dependencies {
    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

    api(libs.minestom)
    api(libs.slf4j.api)

    /* Luckperms */
    api(libs.luckperms)
    api(libs.sponge.configurate.core)
    api(libs.sponge.configurate.hocon)
    /* End of Luckperms */

    testImplementation(libs.logback)
    testImplementation(libs.minestom)
    testImplementation(libs.luckperms)

    /* Luckperms */
    testImplementation(libs.luckperms)
    testImplementation(libs.sponge.configurate.core)
    testImplementation(libs.sponge.configurate.hocon)
    testImplementation(libs.hikari)
    testImplementation(libs.h2database) // Replace with database provider of your choice
    /* End of Luckperms */
}

java{
    withSourcesJar()
    withJavadocJar()

    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    test {
        useJUnitPlatform()
    }

    compileJava {
        options.encoding = "UTF-8"
    }
}

publishing{

    repositories{
        maven {
            name = "release"
            url = uri("https://repo.tesseract.club/releases")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_SECRET")
            }
        }
    }

    publications{
        create<MavenPublication>("maven"){
            groupId = group as String
            artifactId = rootProject.name
            version = project.version as String
            from(components["java"])

            pom{
                name.set(project.name)
                description.set("")
                url.set("https://github.com/TropicalShadow/minestom-utils")

                developers{
                    developer {
                        id.set("tropicalshadow")
                        name.set("TropicalShadow")
                        email.set("me@tesseract.club")
                    }
                }

                issueManagement{
                    system.set("GitHub")
                    url.set("https://github.com/TropicalShadow/minestom-utils/issues")
                }

                scm{
                    connection.set("scm:git:git://github.com/TropicalShadow/minestom-utils.git")
                    developerConnection.set("scm:git:ssh://github.com/TropicalShadow/minestom-utils.git")
                    url.set("https://github.com/TropicalShadow/minestom-utils")
                    tag.set("HEAD")
                }

                ciManagement {
                    system.set("Github Actions")
                    url.set("https://github.com/TropicalShadow/minestom-utils/actions")
                }
            }

        }
    }
}
