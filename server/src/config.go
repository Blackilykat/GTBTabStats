package main

import (
	"encoding/json"
	"os"
)

type Config struct {
	ApiKey  string
	Address string
	CacheDurationHours float64
}

func LoadConfig() Config {
	path := "./config.json"
	contents, err := os.ReadFile(path)
	if err != nil {
		panic(err)
	}

	var config Config
	err = json.Unmarshal(contents, &config)
	if err != nil {
		panic(err)
	}

	return config
}
