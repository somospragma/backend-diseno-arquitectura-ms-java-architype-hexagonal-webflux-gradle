dependencies {
    implementation(project(":domain:model"))
    implementation(project(":domain:ports"))
    implementation(platform(libs.reactor.bom))
    implementation(libs.reactor.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(platform(libs.reactor.bom))
    testImplementation(libs.reactor.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
