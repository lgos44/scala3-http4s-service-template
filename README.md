# Scala3 service template

## Stack

- Http4s
- Tapir
- cats
- Doobie
- ProtoQuill
- Postgres

# Getting started

## Running locally

### Start a local database

```
docker-compose -f docker-compose-dev.yml up -d
```

### Start the application

```
sbt run
```

## Run with Docker

### Build an image

```
sbt docker:publishLocal 
```

### Start containers

```
docker-compose up -d
```

## Run tests

```
sbt test
```