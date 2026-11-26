plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("alibiCallQuality") {
            id = "com.siddumoni.alibicall.quality"
            implementationClass = "AlibiCallQualityPlugin"
        }
    }
}
