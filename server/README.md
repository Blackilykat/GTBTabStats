# GTB Tab Stats / server

The serverside which calls the Hypixel API.

This is running on my server at `https://gtb-tab-stats.blackilykat.dev/api`.

This is required by the [API policy](https://developer.hypixel.net/policies/):

> Do not include your API key in your code, especially if you plan to distribute a binary or have the code available publicly.

The client cannot directly perform the request as it would need to own an API key. The server side takes care of caching and keeping the API key a secret, while relaying only needed information to the clients.

## Use

In the working directory, add a `config.json` file with:
```json
{
	"ApiKey": "YOUR_KEY",
	"Address": ":8080",
	"CacheDurationHours": 4
}
```

**TLS** and **rate limiting** are handled by a reverse proxy.

## Compile

Run during development:
```
$ make run
```

Compile (executable will be in `build/main`):
```
$ make
```
