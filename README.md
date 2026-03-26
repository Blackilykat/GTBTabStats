# GTB Tab Stats

A Minecraft fabric mod for the hypixel server that displays guess the build-related information (selected language, GTB wins, BB score) in the tab list when in a guess the build lobby.

Made for recent Minecraft versions.

## Project structure

`/server`: The serverside which calls the Hypixel API. This is required by the [API policy](https://developer.hypixel.net/policies/):

> Do not include your API key in your code, especially if you plan to distribute a binary or have the code available publicly.

`/client`: The fabric mod.
