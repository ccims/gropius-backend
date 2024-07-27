# Gropius Backend

For more documentation, have a look at our documentation website: https://ccims.github.io/gropius-docs

## About

The backend of the Gropius system.
Provides all services a Gropius backend uses, and modules used by those services.
For a description of all modules, see [Modules](./modules).

## Running the backend

https://github.com/ccims/gropius provides both a production and development setup of all services of the backend using docker-compose.

### Production

```sh
git clone --recursive https://github.com/ccims/gropius.git
cd gropius
docker-compose up
```
For a production setup, it is recommended to change default passwords and secrets defined in `docker-compose.yaml`

### Development

```sh
git clone --recursive https://github.com/ccims/gropius.git
cd gropius
docker-compose -f docker-compose-dev.yaml up
```

Alternatively, it is possible to execut services on its own using Gradle.
[Modules](./modules) provides an overview of the used commands, requirements and runtime dependencies.
[docker-compose-dev.yaml](https://github.com/ccims/gropius/blob/main/docker-compose-dev.yaml) can be used as a reference for a working set of configuration properties for each service.