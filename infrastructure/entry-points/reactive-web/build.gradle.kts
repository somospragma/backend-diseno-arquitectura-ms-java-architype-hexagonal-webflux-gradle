import org.springframework.boot.gradle.tasks.bundling.BootJar

apply(plugin = "java-library")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

// Módulo de librería: no produce un jar ejecutable, solo la lib para app-service.
tasks.named<BootJar>("bootJar") { enabled = false }
tasks.named<Jar>("jar") { enabled = true }

dependencies {
    implementation(project(":domain:model"))
    implementation(project(":domain:ports"))
    implementation(project(":domain:usecases"))
    implementation(project(":infrastructure:helpers"))
    implementation(libs.boot.starter.webflux)
    implementation(libs.boot.starter.validation)
    implementation(libs.jakarta.validation.api)
    implementation(libs.mapstruct)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.boot.starter.test)
    testImplementation(libs.reactor.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
