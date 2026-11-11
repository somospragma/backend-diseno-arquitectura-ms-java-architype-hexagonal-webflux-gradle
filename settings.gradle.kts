rootProject.name = "accounts-service"

include(
    ":domain:model",
    ":domain:ports",
    ":domain:usecases",
    ":infrastructure:driven-adapters:r2dbc-persistence",
    ":infrastructure:driven-adapters:external-api",
    ":infrastructure:driven-adapters:dynamodb-persistence",
    ":infrastructure:driven-adapters:sqs-consumer",
    ":infrastructure:driven-adapters:redis-cache",
    ":infrastructure:driven-adapters:caffeine-cache",
    ":infrastructure:driven-adapters:event-publisher",
    ":infrastructure:driven-adapters:sns-publisher",
    ":infrastructure:driven-adapters:kafka-messaging",
    ":infrastructure:entry-points:reactive-web",
    ":infrastructure:entry-points:grpc",
    ":infrastructure:helpers",
    ":application:app-service",
)
