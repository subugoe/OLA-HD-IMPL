## OLA-HD: A Long-term Archive System
OLA-HD is a mixture between an archive system and a repository.
The input is a `.zip` file whose content is structure as [BagIt](https://tools.ietf.org/html/rfc8493).
The bag will be validated before any further actions take place.
Data uploaded to the system will be stored in both tapes and hard drives.
The separation rules are defined in the configuration file.

In addition, each uploaded zip get a persistent identifier (PID).
This PID can be used for sharing, citing, and versioning purposes.

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