buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.9.10"
}



repositories {
    mavenCentral()
}

kotlin {

    targets {
        jvm {
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}
