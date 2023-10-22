#/bin/bash

mkdir -p ./website/schemas

./gradlew api-public:bootRun --args="--gropius.api.public.jwtSecret=SecretToGetGraphQLSchemaDoNotUseInProduction --gropius.core.createIndicesOnStartup=false" --no-daemon &
gradlew_pid=$!
schema_endpoint="http://localhost:8080/sdl"
c=0
until curl -s -f -o /dev/null $schema_endpoint
do
    ((c++))
    if ((c > 120)); then
        echo "Failed to get graphql schema: timeout"
        exit 1
    fi
    echo "Waiting for server"
    sleep 2
done
curl -s -o ./website/schemas/api-public.gql $schema_endpoint
echo "Stopping graphql server"
kill $gradlew_pid

./gradlew api-internal:bootRun --args="--server.port=8081 --gropius.core.createIndicesOnStartup=false" --no-daemon &
gradlew_pid=$!
schema_endpoint="http://localhost:8081/sdl"
c=0
until curl -s -f -o /dev/null $schema_endpoint
do
    ((c++))
    if ((c > 120)); then
        echo "Failed to get graphql schema: timeout"
        exit 1
    fi
    echo "Waiting for server"
    sleep 2
done
curl -s -o ./website/schemas/api-internal.gql $schema_endpoint
echo "Stopping graphql server"
kill $gradlew_pid