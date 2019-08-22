## OLA-HD: A Long-term Archive System
OLA-HD is a mixture between an archive system and a repository.
The input is a `.zip` file whose content is structure as [BagIt](https://tools.ietf.org/html/rfc8493 "BagIt RFC").
The zip content will be validated before any further actions take place.
Data uploaded to the system will be stored in both tapes and hard drives.
The separation rules are defined in the configuration file.
In addition, each uploaded zip get a persistent identifier (PID).
This PID can be used for sharing, citing, and versioning purposes.

A [Swagger](https://swagger.io/ "Swagger homepage") documentation is available at `/swagger-ui.html`.

## Project structure
```
.
├── .env                 -> environment variables for all Docker images
├── .gitignore
├── .gitlab
│   └── issue_templates  -> templates used by Gitlab when users want to create issues
├── LICENSE
├── README.md
├── back-end
│   ├── .dockerignore    -> list of files which should not be copy to Docker container during the build process
│   ├── .gitignore
│   ├── Dockerfile       -> a file to dockerize the system
│   ├── mvnw             -> Maven wrapper to build the project in case Maven is not installed
│   ├── mvnw.cmd         -> Maven wrapper for Windows
│   ├── pom.xml          -> project dependencies
│   └── src
│       └── main
│           ├── java     -> all source codes
│           └── resources
│               ├── application-development.properties        -> config file for development
│               ├── application-production.properties         -> config file for production
│               └── application.properties                    -> config file for both
├── build.sh             -> run this file to build the Docker image and start up the whole system (database, back-end, front-end, reverse proxy)
└── docker-compose.yml   -> describe all images in the system
```

## Prerequisites
* [Docker](https://docs.docker.com/install/ "Docker installation guide")
* [docker-compose](https://docs.docker.com/compose/install/ "docker-compose installation guide")

## Install and run
```
git clone https://gitlab.gwdg.de/pwieder/ola-hd.git
cd ola-hd
./build.sh
```

## Recipes
### Import a file
To import a file, send a `POST` request to the `/bag` endpoint.
This endpoint does not open to public.
Therefore, authentication is needed to access it.
```
curl -X POST \
     http://your.domain.com/bag \
     -H 'Authorization: <your-credential>' \
     -H 'content-type: multipart/form-data' \
     -F file=@<path-to-file>
```
In the response, a PID is returned in the `Location` header.

### Import a new version of a work
To import a new version, in addition to the `.zip` file, a PID of a previous work version must be submitted as well.
```
curl -X POST \
     http://your.domain.com/bag \
     -H 'Authorization: <your-credential>' \
     -H 'content-type: multipart/form-data' \
     -F file=@<path-to-file>
     -F prev=<PID-previous-version>
```

### Full-text search
To perform a search, send a `GET` request to the `/search` endpoint.
The query is provided via the `q` parameter, e.g. `/search?q=test`
```
curl -X GET http://your.domain.com/search?q=test
```

### Search by meta-data
Besides full-text search, users can also search by meta-data.
Currently, OLA-HD supports meta-data from [Dublin Core](https://www.dublincore.org/specifications/dublin-core/dces/).
To use it, prepend the meta-data with `dc`, e.g. `/search?q=dcCreator:John`

**IMPORTANT**: a PID always contains a forward slash, which is a special character.
For that reason, search by identifier (PID) can only be perform as a phrase search and the double quote must be encoded as `%22`.
```
curl -X GET http://your.domain.com/search?q=dcIdentifier:%22your-identifier%22
```

### Quick export
Data stored on hard drives can be quickly and publicly exported.
To do so, send a `GET` request to the `/export` endpoint.
The `id` must be provided as a URL parameter.
```
curl -X GET http://your.domain.com/export?id=your-id --output export.zip
```

### Full export request
To initiate the data movement process from tapes to hard drives, a full export request must be made.
In the request, the identifier of the file is specified.
Then, the archive manager will move this file from tapes to hard drives.
This process takes quite long, hours or days, depending on the real situation.
To send the request, simply send a `GET` request to the `export-request` endpoint with the `id`.
```
curl -X GET http://your.domain.com/export-request?id=your-id
```

### Full export
After the export request was successfully fulfilled, the full export can be made.
```
curl -X GET http://your.domain.com/full-export?id=your-id --output export.zip
```