package main

import "fmt"



func main() {
	config := LoadConfig()

	fmt.Printf("Key: %s, address: %s", config.ApiKey, config.Address)
	
}
