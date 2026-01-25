# minestom-utils

Small utilities and integrations for Minestom (LuckPerms, Spark, Polar, Axiom, etc.).

## Requirements

- Java 25 (toolchain is set to 25)
- Minestom 1.21.11

## Install from Maven

Repository:

- https://repo.tesseract.club/releases
  https://repo.tesseract.club/#/releases/club/tesseract/minestom/minestom-utils

### Gradle (Kotlin)

```kotlin
repositories {
    maven("https://repo.tesseract.club/releases")
}

dependencies {
    implementation("club.tesseract.minestom:minestom-utils:<version>")
}
```

### Gradle (Groovy) [why are you still using Groovy]

```groovy
repositories {
    maven { url "https://repo.tesseract.club/releases" }
}

dependencies {
    implementation "club.tesseract.minestom:minestom-utils:<version>"
}
```

### Maven

```xml
<repositories>
  <repository>
    <id>tesseract-releases</id>
    <url>https://repo.tesseract.club/releases</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>club.tesseract.minestom</groupId>
    <artifactId>minestom-utils</artifactId>
    <version>VERSION</version>
  </dependency>
</dependencies>
```

## Basic API example

```java
// Example: register commands and set up a full-bright dimension
CommandRegistry.registerAll();
var fullBright = FullBrightDimension.getInstance();
```
