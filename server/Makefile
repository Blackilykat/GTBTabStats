.PHONY: build run clean

build: $(SRC)
	mkdir -p build
	go build -o build/main ./src

run:
	mkdir -p run
	cd run; go run -race ../src

clean:
	rm -rf build
