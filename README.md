# Explainer Atom Editor

* CI : [TeamCity](https://teamcity-aws.gutools.co.uk/viewType.html?buildTypeId=EditorialTools_Explainer)

###Scala: 2.11.6, Scala.js: 0.6.5, Play: 2.4.0, Slick: 3.0.0


The sbt build file contains 3 modules
- `ExplainerServer` Play application (server side)
- `ExplainerClient` Scala.js application (client side)
- `ExplainerShared` Scala code that you want to share between the server and the client.

### Setup

You need a `/etc/gu/explainer.stage.conf` file:

```
$ cat /etc/gu/explainer.stage.conf
stage=DEV
```

Configure the composer route in nginx:

```
cd <path_of_dev_nginx>
sudo ./setup-app.rb <path_of_explainer>/nginx/nginx-mapping.yml
```

### Run the application
```
$ sbt run
```

### Access the User Interface

Explain Maker should now be found at [https://explainer.local.dev-gutools.co.uk/](https://explainer.local.dev-gutools.co.uk/).

In order to get the Panda authentication cookie needed to access Explain Maker, you might want to, for instance, first visit your local instance of Composer: [https://composer.local.dev-gutools.co.uk/](https://composer.local.dev-gutools.co.uk/).

