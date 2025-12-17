.PHONY: build run clean

build: $(SRC)
	mkdir -p build
	go build -o build/main ./src

run:
	cd run; go run ../src

clean:
	rm -rf build
