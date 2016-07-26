# Explainer Atom Editor

* CI : [TeamCity](https://teamcity-aws.gutools.co.uk/viewType.html?buildTypeId=EditorialTools_Explainer)

###Scala: 2.11.6, Scala.js: 0.6.5, Play: 2.4.0, Slick: 3.0.0


The sbt build file contains 3 modules
- `ExplainerServer` Play application (server side)
- `ExplainerClient` Scala.js application (client side)
- `ExplainerShared` Scala code that you want to share between the server and the client.

### Setup
You'll need to create ~/.gu.explainer.local.conf, with the following fields:
```
DEV {
  pandomain {
    domain = "local.dev-gutools.co.uk"
  }
  kinesis {
    streamName {
      preview = "<preview kinesis name - only needed if kinesis publishing enabled>"
      live = "<live kinesis name- only needed if kinesis publishing enabled>"
    }
  }
}
enable.kinesis.publishing=false
```

Configure the composer route in nginx:

```
cd <path_of_dev_nginx>
sudo ./setup-app.rb <path_of_explainer>/nginx/nginx-mapping.yml
```

### Run the application
```
./run-explainer.sh
```

### Access the User Interface

Explain Maker should now be found at [https://explainer.local.dev-gutools.co.uk/](https://explainer.local.dev-gutools.co.uk/).

In order to get the Panda authentication cookie needed to access Explain Maker, you might want to, for instance, first visit your local instance of Composer or Workflow. Alternatively (better option) you can run [login.gutools](https://github.com/guardian/login.gutools) for a seamless experience.

