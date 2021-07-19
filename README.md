# Alpha-Cubesat-Ground
Ground Station software for the AlphaCubesat project

## Overview
This is the Ground Station software for the AlphaCubesat project. It is a full-stack application with code for frontend UI and backend.

The backend, under `cubesat-backend`, receives data from the RockBlock API, processes 
that data, and ships it to ElasticSearch. 
It is architected as a RESTful server with 
multiple modules that each provide functionality for
 specific services such as receiving telemetry,
 authentication, and issuing commands. 

The UI, under `control-frontend`, is a work in progress but allows you to select from a 
set of commands, fill in fields, validate them, and send them to the backend, which in turn
uses RockBlock web services to issue them to the satellite. Additional features
such as a log of sent commands, macros, and scheduling on a visual timeline are not yet present, but
will be.

The project is developed in Clojure/Clojurescript (with Compojure-api and Re-frame), and the backend is a rewrite (along with the new features) of the previous 
version of the ground software in C++ (https://github.com/AlphaCornell/AlphaGroundStation).

## Dependencies
- Java version >= 8
- Leiningen, a build tool for Clojure. https://leiningen.org/ has instructions for installation. Another helpful link: https://purelyfunctional.tv/guide/how-to-install-clojure/
- NPM, javascript package manager: https://www.npmjs.com/get-npm
- React: `npm install react` (may need to use `sudo npm install react`)
- ReactDOM: `npm install react-dom` (may need to use `sudo npm install react-dom`)
- Create-React-Class: `npm install create-react-class` (may need to use `sudo npm install create-react-class`)
- All other dependencies (such as Clojure/script libraries) will be handled by Leiningen when building

## Setting up Alpha Ground Station for Development, Running in Development Mode
This section provides instructions on setting up the environment and running the ground software for development/testing purposes. Both the frontend and backend use frameworks that support code hotloading, so simply making changes to the code will update the running ground software, so long as it is started in development mode. This makes for a convenient development experience, and it is recommended to keep the ground software and environment running while developing as changes in code will be loaded without the need to recompile/reboot anything.

*Note: Alpha runs its ground software on Ubuntu Server, so if you are using something else, such as Windows, the locations of ElasticSearch and Kibana configuration files might be elsewhere.*

1. **Install ElasticSearch.**
ElasticSearch is what the ground system uses to store telemetry data received from the satellite. 
A guide for installation can be found in Elastic's documentation: https://www.elastic.co/guide/en/elasticsearch/reference/current/install-elasticsearch.html. Then, configure ElasticSearch as necessary (configuration at `/etc/elasticsearch/elasticsearch.yml`). By default it will listen on `localhost:9200`.

2. **Install Kibana.**
Kibana is a visualization tool for creating graphics and timeseries out of data in ElasticSearch. This will be useful for configuring alerts, graphs, and other aggregations of telemetry data.
A guide for installation can be found in Elastic's documentation: https://www.elastic.co/guide/en/kibana/current/install.html. Kibana can be configured as needed in the configuration file (at `/etc/kibana/kibana.yml`). By default Kibana listens on `localhost:5601`.

3. **Configure the Backend Server.**
Navigate to `cubesat-backend` and run `lein ring server`. Configure the backend in `config.edn`. The `config.edn` file provided has placeholder/default values for everything, so you can just change fields.This configuration is where you specify the behavior each module in the backend. 
Under `:telemetry`, you can specify the ElasticSearch indices for storing data received from RockBlock Web Services as well as processed satellite data. 
Under `:database` configuration, you can specify the location of the ElasticSearch server, as well as authentication credentials. If you are not using authentication in ElasticSearch (you won't by default), you can remove the `:basic-auth` option entirely under `:conn-config`. Under `:image` you can configure the root directory on the local filesystem where satellite image data will be stored (and served from). Finally, under `:control` you specify your RockBlock web services credentials as well as the id "imei" of the physical RockBlock unit you are using. This allows the ground system to authenticate with RockBlock and issue commands to the device.

4. **Run the Backend Server.**
Run the command `lein ring server`. This will start the Alpha backend server that listens for requests at `localhost:3000`. It will search for another port
if 3000 is taken, and print the one it listens on to the console. The server will run in development mode, so it hotloads code - if you make a change, as long as you don't change dependencies in `project.clj`, the new code will be injected (and start running) into the already running server when you save a file in your editor. This means you rarely, if ever, need to reboot the program when developing and testing new changes/additions. An interactive API documentation Swagger GUI will also be generated when the development server is booted, which can be found at `/api`. Unfortunately, this does not update when code is hotloaded, so you should refresh the page if you make changes to the API and want to see them reflected in the documentation.

5. **Configure the Frontend.**
Navigate to `control-frontend`. There is not much configuration that is necessary, but the front end UI is served by its own server. As this UI will attempt to make requests against the actual backend API, the frontend development server will not be able to handle them. Thus, this frontend server is configured in `shadow-cljs.edn` to reroute (proxy) any requests it cannot satisfy to the address of the actual backend (which should be running if you completed step 4). By default, this attempts to reroute to `localhost:3000`, as this is the default address for the Alpha backend. But if you are using a different one, you will need to change `:proxy-url` to point to the right address.

6. **Run the Frontend UI**
Once configured, run the command `lein dev` (make sure the back-end is still running. An easy way to do this run `lein ring server` in one terminal and `lein dev` in another). This will boot the server that serves the UI and proxies requests to the real backend. Navigate to the address configured (it will be printed in the terminal, and it is `localhost:8000` by default). Again, frontend code is hotloaded, so leave the UI open as you develop, and changes to the code will be uploaded to the browser and reflected in the UI without the need to refresh the page in the browser or manually rerun/rebuild any code. This is accomplished with shadow-cljs, and you should see a small popup animation in the browser window whenever an update occurs. This will also point out errors if it finds any. The UI will require you to log in. Assuming you haven't changed the config or users file, the default username is `example-user` and the password is `example`.


## Setting up Alpha Ground Station in Production
There are some tasks and configuration, such as building the code for production, that are outlined here. These will help you set up the ground station in a production environment that serves and runs optimized code, and is configured to be secure.

- **Build the backend code** in `cubesat-backend` by running `lein do clean, ring uberjar`. This will produce a jar file under `target/` containing the backend and all dependencies to be run stand-alone when you deploy it.
- **Build the frontend code** in `control-frontend` by running `lein prod`. This will produce the production JavaScript code, and all other assets for the UI under `resources/public`. This will also produce a standalone jar that serves it under `target/`, containing everything.
- You will run into the request routing issue with the frontend again in a production environment - the UI will make API requests to the UI server when it needs to access the real backend server. So it is recommended to **use a reverse proxy** such as nginx to group everything under one service, and have the API requests be routed where they need to go. However, if you wish to host it from a subdirectory when you configure the proxy, the ground software currently doesn't allow configuration of the base url, so you will need to hardcode it. Support for this is coming soon (see "work in progress" section).
- While you are at it, **host kibana behind a reverse proxy**. Again, if you host it at a subdirectory, you will need it to make requests to the new route. This can be configured by changing the `server.basePath` configuration to the name of your desired path in `kibana.yml`.
- **Secure the Elastic Stack**. Secure kibana by going to the configuration file, and adding `xpack.security.enabled: true`. This will enable Kibana authentication. Then, go to the ElasticSearch configuration file, and add: `xpack.security.enabled: true` and (only if you are running a single node, you will need TLS for a cluster, which is not covered here) `discovery.type: single-node`. Now, navigate to the ElasticSearch installation and run `bin/elasticsearch-setup-passwords interactive`. This will prompt you for passwords for all the default users, including the `kibana` and `elastic` users. Now for Kibana to communicate with ElasticSearch, it must authenticate as the `kibana` user. To allow it to do this, return to the Kibana config, and add `elasticsearch.username: "kibana"` and `elasticsearch.password: "<password>"` where `<password>` is the password you just set for the `kibana` user.
The default superuser on Kibana is `elastic` with the password you just set. You can manage/create/remove users, change passwords, and determine roles in the Kibana UI by navigating to the options panel (⚙) and clicking on "security". Create a role with the name of your choice that has all permissions on the indices you configured in `config.edn` for telemetry data. Then create a user with this role, and alter `config.edn` to use its credentials. 
- **Secure the ground system** -- Navigate to the tools project `cubesat-clitools` and build it with `lein uberjar`. Then run the resulting jar, which will prompt you to create some user credentials. It will accept the name of an output file as an argument. Once finished, change the `:users-file` configuration in the configuration file, and the backend will require authentication with any of the credentials in the new file. `cubesat-clitools` can also be used to create and manage users for this file.
- **Beware of missing or unfinished features**. This software is still being developed for Alpha's mission. So some features may be missing. Check the following section for more information.

## Work in Progress Features
**Currently the ground software is still being built and is not fully ready for use**. This section outlines incomplete features that will be coming soon.

- More robust token authentication for API. Might implement expiration and support for other methods in the future.**
- A homepage. Currently the frontend only serves the UI dashboard for control. A homepage serving as a hub that links the user to kibana, various sources of documentation, and the control dashboard would be nice to have.
- Additional control UI features. Currently it is possible to select commands, fill in fields, and run them, but more features are on their way such as a log of the history, popup notifications, and a timeline for scheduling future commands instead of just running instantly, which is currently the only option.

## Documentation
For code documentation, run `lein codox`, which will output documentation of the codebase at `/target/doc` as html. At some point I will host these on cubesat.global. UPDATE: you can visit [here](http://cubesat.global/doc/) for backend documentation.

For API documentation, run the development server, which generates interactive API documentation.

For Alpha members, additional documentation on the satellite protocols and other project details are on the Google Drive as usual.
