# # Define the default target
# .PHONY: all
# all: build
#
# # Build the project using Cargo
# .PHONY: build
# build:
# 	zig build-exe producer.zig -lc -lrdkafka -L/opt/homebrew/Cellar/librdkafka/2.8.0/lib -I/opt/homebrew/include
# 	zig build-exe consumer.zig -lc -lrdkafka -L/opt/homebrew/Cellar/librdkafka/2.8.0/lib -I/opt/homebrew/include
#

BIN_DIR := target/bin
OBJ_DIR := target/obj

build:
	mkdir -p $(BIN_DIR) $(OBJ_DIR)
	zig build-exe producer.zig -lc -lrdkafka -L/opt/homebrew/Cellar/librdkafka/2.8.0/lib -I/opt/homebrew/include --cache-dir $(OBJ_DIR) -femit-bin=$(BIN_DIR)/producer
	zig build-exe consumer.zig -lc -lrdkafka -L/opt/homebrew/Cellar/librdkafka/2.8.0/lib -I/opt/homebrew/include --cache-dir $(OBJ_DIR) -femit-bin=$(BIN_DIR)/consumer

clean:
	rm -rf $(BIN_DIR) $(OBJ_DIR)
