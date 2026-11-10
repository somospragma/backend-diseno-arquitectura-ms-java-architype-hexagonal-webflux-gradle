apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
    implementation(project(":domain:model"))
    implementation(project(":domain:ports"))
    implementation(project(":domain:usecases"))
    implementation(project(":infrastructure:driven-adapters:r2dbc-persistence"))
    implementation(project(":infrastructure:driven-adapters:external-api"))
    implementation(project(":infrastructure:driven-adapters:dynamodb-persistence"))
    implementation(project(":infrastructure:driven-adapters:sqs-consumer"))
    implementation(project(":infrastructure:driven-adapters:redis-cache"))
    implementation(project(":infrastructure:driven-adapters:caffeine-cache"))
    implementation(project(":infrastructure:driven-adapters:event-publisher"))
    implementation(project(":infrastructure:driven-adapters:sns-publisher"))
    implementation(project(":infrastructure:driven-adapters:kafka-messaging"))
    implementation(project(":infrastructure:entry-points:reactive-web"))
    implementation(project(":infrastructure:entry-points:grpc"))
    implementation(project(":infrastructure:helpers"))

    implementation(libs.boot.starter.webflux)
    implementation(libs.boot.starter.r2dbc)
    implementation(libs.boot.starter.actuator)
    implementation(libs.boot.starter.validation)
    implementation(libs.springdoc.webflux)
    implementation(libs.logstash.encoder)
    implementation(libs.micrometer.registry.prometheus)
    runtimeOnly(libs.r2dbc.postgresql)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.boot.starter.test)
    testImplementation(libs.reactor.test)
    testImplementation(libs.blockhound)
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly(libs.r2dbc.h2)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Aísla cada clase de test en su propia JVM. Necesario porque BlockHoundTest
// instala BlockHound a nivel de JVM (BeforeAll) y no debe contaminar el resto
// de tests (p.ej. el de integración que levanta el contexto reactivo completo).
tasks.named<Test>("test") {
    setForkEvery(1)
}

tasks.register<Test>("architectureTest") {
    useJUnitPlatform()
    include("**/HexagonalArchitectureTest.class")
}

tasks.named("build") {
    dependsOn("architectureTest")
}
