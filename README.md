# Storage and archiving via CDStar as docker service
## Introduction


## Build and Run
To build and run the application you need to install:

[docker](https://docs.docker.com/install/linux/docker-ce/ubuntu/#upgrade-docker-ce-1)
[docker-compose](https://github.com/docker/compose) 


##### Preparation: Set environment variables 

Modify the environment variables in .env as needed



##### (Re-)Build and start the docker images (also after changes)

```
./build.sh
```
 
In web image will be constructed in the build process, this will take a few minutes.


##### See service log output

```
docker-compose logs -f <service>
```

Switch <service> to 'redis', 'ola-hd', ... Most interesting is 'ola-hd'.  



##### Request ElasticSearch

```
GET http://127.0.0.1:5601      /app/
{ 
    ...
}
```



##### Request a ... list

```
...
```

##### Upload a BagIt ZIP

```
...
```

##### Download a BagIt ZIP

```
...
```
