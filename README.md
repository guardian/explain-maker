# Explainer Atom Editor

* CI : [TeamCity](https://teamcity-aws.gutools.co.uk/viewType.html?buildTypeId=EditorialTools_Explainer)
* See live: [PROD](https://explainers.gutools.co.uk) [CODE](https://explainers.code.dev-gutools.co.uk)

###Scala: 2.11.6, Scala.js: 0.6.5, Play: 2.4.0, Slick: 3.0.0


The sbt build file contains 3 modules
- `ExplainMakerServer` Play application (server side)
- `ExplainMakerClient` Scala.js application (client side)
- `ExplainMakerShared` Scala code that you want to share between the server and the client.


### Setup Nginx

To run explain maker locally you will need ngnix.

1. Install nginx:
  * *Linux:*   ```sudo apt-get install nginx```
  * *Mac OSX:* ```brew install nginx```

2. Make sure you have a sites-enabled folder under your nginx home. This should be
  * *Linux:* ```/etc/nginx/sites-enabled```
  * *Mac OSX:* ```/usr/local/etc/nginx/```

3. Make sure your nginx.conf (found in your nginx home) contains the following line in the http{} block:
`include sites-enabled/*;`
  * you may also want to disable the default server on 8080

4. Get the [dev-nginx](https://github.com/guardian/dev-nginx) repo checked out on your machine

5. Set up certs if you've not already done so (see dev-nginx readme)

6. Configure the workflow route in nginx

```
sudo /path/to/dev-nginx/setup-app.rb /path/to/explain-maker/nginx/nginx-mapping.yml
```

### Setup Explainer
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
      reindex-preview = "<reindex preview kinesis name, usually the same as the preview stream name - only needed if kinesis publishing enabled>"
      reindex-live = "<reindex live kinesis name, usually the same as the live stream name - only needed if kinesis publishing enabled>",
      elk  = "<elk kinesis stream - only needed if elk publishing enabled"
    }
  }
  capi.key = "CAPI-API-KEY"
  presence.endpoint = "PRESENCE-ENDPOINT-URL"
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

Ensure that you have the required node version available. You should be using [nvm](https://github.com/creationix/nvm) to manage your node dependencies, then run:

```
nvm install 6
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

Explain Maker should now be found at [https://explainers.local.dev-gutools.co.uk/](https://explainers.local.dev-gutools.co.uk/).

In order to get the Panda authentication cookie needed to access Explain Maker, you might want to, for instance, first visit your local instance of Composer or Workflow. Alternatively (better option) you can run [login.gutools](https://github.com/guardian/login.gutools) for a seamless experience.

Note that, when running on local, the presence endpoint is the CODE Presence. This implies that to get Presence display correctly when running on local, you need to have a CODE Panda cookie (to get one just visit the CODE instance of Composer or Workflow).

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
