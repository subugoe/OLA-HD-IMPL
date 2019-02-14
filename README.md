# Storage and archiving via CDStar as docker service

## Introduction  (todo)


## Build and Run
To build and run the application you need to install:

[docker](https://docs.docker.com/install/linux/docker-ce/ubuntu/#upgrade-docker-ce-1)
[docker-compose](https://github.com/docker/compose) 


##### Preparation: open ports

* To visualize and explore the data in ElasticSearch, open the port for the Kibana services. In the dev environment we use the default port 5601 (see .env).
* To import/export ZIP files, open the port to the OLA-HD service. In the dev environment we use the default port 8080 (see .env).


##### Preparation: set environment variables 

* Change in the project directory (Console)
```
$> cd /path/to/project/root
```  

* Modify the environment variables in .env as needed. In the dev environment at least we have to change <host_ip> to the docker host ip address. 


##### (Re-)Build and start the docker images (Console)

```
$> ./build.sh
```


##### See service log output (Console)

```
$> docker-compose logs -f <service>
```

Switch <service> to 'redis', 'ola-hd', ... Most interesting is 'ola-hd'.  


##### Request ElasticSearch via Kibana (Browser)

```
http://141.5.105.253:5601/app/
```

* Goto DecTools > Console > add the following exampe query to the left column and click the green arrow.  

```
GET /ola-hd/_search
{
  "query": {
    "match_all": {}
  }
}
```



##### Request a (paginated) list of Works (Console)

```
<todo for Triet>
```

##### Upload a BagIt ZIP (Console)

```
<todo for Triet>
```

##### Download a BagIt ZIP (Console)

```
<todo for Triet>
```
