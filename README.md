# red5-avalon

This project implements a [StreamFilenameGenerator](http://dl.fancycode.com/red5/api/org/red5/server/api/stream/IStreamFilenameGenerator.html) to bridge between the [Avalon Media System](http://avalonmediasystem.org/) and the [red5 Media Server](http://www.red5.org/). It provides the logic and configuration necessary for red5 to find streams produced by the Avalon/Matterhorn stack, and to pass a token back to a running Avalon server to authorize stream playback.

## Build and Install

### Prerequisites

* JDK 1.6 or greater
* Apache Ant

### Instructions

1. Download and install [red5 Media Server](http://www.red5.org/red5-server/) 1.0 or above
2. In the `red5-avalon` directory, execute `RED5_HOME=/path/to/red5-server ant deploy`
3. Start the red5 server
4. Wait about 30 seconds (to allow the warfile to unpack itself)
5. Shut down the red5 server
5. Load `/path/to/red5/server/webapps/avalon/WEB-INF/red5-web.properties` in an editor
6. Change the value of `avalon.serverUrl` to point to your Avalon server
7. Restart the red5 server
