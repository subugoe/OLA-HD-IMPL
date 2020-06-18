# OLA-HD: A Long-term Archive System
OLA-HD is a mixture between an archive system and a repository.
The input is a `.zip` file whose content is structure as [BagIt](https://tools.ietf.org/html/rfc8493 "BagIt RFC").
The zip content will be validated before any further actions take place.
Data uploaded to the system will be stored in both tapes and hard drives.
The separation rules are defined in the configuration file.
In addition, each uploaded zip get a persistent identifier (PID).
This PID can be used for sharing, citing, and versioning purposes.

If you deploy your own instance, a [Swagger](https://swagger.io/ "Swagger homepage") documentation is
available at `/swagger-ui.html`.

For the prototype:
* Please click [here](http://141.5.98.232/ "OLA-HD Prototype") to experience it.
* Please click [here](http://141.5.98.232/api/swagger-ui.html "OLA-HD API Documentation") for the current Swagger documentation.

## Project structure
```
.
├── .gitlab
│   └── issue_templates  -> templates used by Gitlab when users want to create issues
├── admin-gui            -> the web UI for logged in users
├── back-end             -> contains source code for OLA-HD service
├── images               -> images used in README files
├── nginx                -> the Nginx proxy for the whole system
├── user-gui             -> web UI for public usage
├── .env                 -> environment variables for all Docker images
├── .gitignore
├── LICENSE
├── README.md
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

## System overview
