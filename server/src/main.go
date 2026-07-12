package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"regexp"
	"sync"
	"time"
)

type Player struct {
	Username string `json:"username"`
	Language string `json:"language"`
	Wins int        `json:"wins"`
	Score int       `json:"score"`

	CachedTime time.Time `json:"-"`
}

type OriginalApiPlayer struct {
	Player *struct {
		Username string `json:"displayname"`
		Language string `json:"userLanguage"`
		Stats struct {
			BuildBattle struct {
				Wins int `json:"wins_guess_the_build"`
				Score int `json:"score"`
			} `json:"BuildBattle"`
		} `json:"stats"`
	} `json:"player"`
}

func clearCacheRegularly(cache *map[string] *struct {*Player; *sync.RWMutex}, mutex *sync.RWMutex, config Config) {
	tick := time.Tick(1 * time.Hour)

	for {
		<- tick
		mutex.Lock()

		fmt.Println("Starting cache cleanup")

		for k, v := range *cache {
			if v.Player == nil {
				fmt.Println("Cleanup: Removing", k, "(nil)")
				delete(*cache, k)
				continue
			}

			if time.Since(v.Player.CachedTime).Hours() >= config.CacheDurationHours {
				fmt.Println("Cleanup: Removing", k)
				delete(*cache, k)
			} else {
				fmt.Println("Cleanup: Keeping", k, "(still valid)")
			}
		}

		fmt.Println("Cache cleanup done")

		mutex.Unlock()
	}

}

func main() {
	config := LoadConfig()

	cache := map[string] *struct {*Player; *sync.RWMutex} {}
	cacheMutex := sync.RWMutex{}

	go clearCacheRegularly(&cache, &cacheMutex, config)

	uuidRegex := regexp.MustCompile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

	http.HandleFunc("GET /api/profile/{uuid...}", func(w http.ResponseWriter, r *http.Request) {
		uuid := r.PathValue("uuid")

		if !uuidRegex.MatchString(uuid) {
			// would be more technically correct to allow other accepted formats but there is no point
			// if this mod is the only place the API calls will be coming from.
			w.WriteHeader(400)
			fmt.Fprintln(w, "Invalid UUID (should be in format xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx, all lowercase)")
			return
		}

		cacheMutex.RLock()

		cached, wasCached := cache[uuid]

		cacheMutex.RUnlock()

		if wasCached {
			cached.RWMutex.RLock()

			if cached.Player == nil {
				fmt.Println("Cached 404 for", uuid)
				w.WriteHeader(404)
				cached.RWMutex.RUnlock()
				return
			} else if time.Since(cached.Player.CachedTime).Hours() < config.CacheDurationHours {
				json.NewEncoder(w).Encode(*cached.Player)
				fmt.Println("Cached 200 for", uuid, "(" + cached.Username + ")")
				cached.RWMutex.RUnlock()
				return
			} else {
				fmt.Println("Cache expired for", uuid)
				cached.RWMutex.RUnlock()
			}
		} else {
			cacheMutex.Lock()

			cache[uuid] = &struct{*Player; *sync.RWMutex}{nil, &sync.RWMutex{}}
			cached = cache[uuid]

			cacheMutex.Unlock()
		}

		cached.RWMutex.Lock()
		defer cached.RWMutex.Unlock()

		fmt.Println("Making request for", uuid)

		client := &http.Client{}

		req, err := http.NewRequest("GET", "https://api.hypixel.net/v2/player?uuid=" + uuid, nil)
		if err != nil {
			fmt.Println("ERROR in creating request:", err)
			w.WriteHeader(500)
			return
		}

		req.Header.Add("API-Key", config.ApiKey)

		res, err := client.Do(req)

		if err != nil {
			fmt.Println("ERROR in performing request:", err)
			w.WriteHeader(500)
			return
		}

		switch res.StatusCode {
		case 200:
			var oaPlayer OriginalApiPlayer
			err = json.NewDecoder(res.Body).Decode(&oaPlayer)
			if err != nil {
				fmt.Println("ERROR in parsing response:", err)
				w.WriteHeader(500)
				return
			}

			if oaPlayer.Player == nil {
				fmt.Println("Caching 404 for", uuid)
				cached.Player = nil
				w.WriteHeader(404)
				return
			}
			fmt.Println("Caching 200 for", uuid, "(" + oaPlayer.Player.Username + ")")

			player := &Player{
				Username: oaPlayer.Player.Username,
				Language: oaPlayer.Player.Language,
				Wins: oaPlayer.Player.Stats.BuildBattle.Wins,
				Score: oaPlayer.Player.Stats.BuildBattle.Score,
				CachedTime: time.Now(),
			}

			cached.Player = player

			json.NewEncoder(w).Encode(*player)
		default:
			fmt.Println("Unexpected status", res.StatusCode)
			w.WriteHeader(500)
		}
	})

	log.Fatal(http.ListenAndServe(config.Address, nil))
}
