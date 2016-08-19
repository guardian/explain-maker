# Explainer Atom Editor

* CI : [TeamCity](https://teamcity-aws.gutools.co.uk/viewType.html?buildTypeId=EditorialTools_Explainer)
* See live: [PROD](https://explainers.gutools.co.uk) [CODE](https://explainers.code.dev-gutools.co.uk)

###Scala: 2.11.6, Scala.js: 0.6.5, Play: 2.4.0, Slick: 3.0.0


The sbt build file contains 3 modules
- `ExplainMakerServer` Play application (server side)
- `ExplainMakerClient` Scala.js application (client side)
- `ExplainMakerShared` Scala code that you want to share between the server and the client.

### Setup
You'll need to create ~/.gu/explainer.local.conf, with the following fields:

```
DEV {
  pandomain {
    domain = "local.dev-gutools.co.uk"
  }
  kinesis {
    streamName {
      preview = "<preview kinesis name - only needed if kinesis publishing enabled>"
      live = "<live kinesis name- only needed if kinesis publishing enabled>",
      elk  = "<elk kinesis stream - only needed if elk publishing enabled"
    }
  }
  capi.key = "CAPI-API-KEY"
}
enable.kinesis.publishing=false
enable.elk.logging=false

include "application.conf"
```

Configure the composer route in nginx:

```
cd <path_of_dev_nginx>
sudo ./setup-app.rb <path_of_explainer>/nginx/nginx-mapping.yml
```

Install dependencies and build CSS:

```
./setup.sh
```

### Run the application
```
./run-explainer.sh
```

You can also watch the .scss files so they compile on change:

```
cd explainer-server
npm run watch
```

### Access the User Interface

Explain Maker should now be found at [https://explainer.local.dev-gutools.co.uk/](https://explainer.local.dev-gutools.co.uk/).

In order to get the Panda authentication cookie needed to access Explain Maker, you might want to, for instance, first visit your local instance of Composer or Workflow. Alternatively (better option) you can run [login.gutools](https://github.com/guardian/login.gutools) for a seamless experience.

## Troubleshooting

### NPM issues
These are usually to do with old dependencies being overwritten. Delete your `npm_modules` folder and clear the npm cache with:

```
cd explainer-server
rm -rf node_modules
npm cache clear
```

then reinstall them with:

```
cd ../
./setup.sh
```

This will pick up the correct Node version and reinstall all client side dependencies.
