.DELETE_ON_ERROR:
main: build

.PHONY: test
test:
	@echo "Running tests..."
	clojure -M:kaocha

test-watch:
	@echo "Running tests and watching for filechanges..."
	clojure -M:kaocha --watch
.PHONY: build
build:
	@echo "Building jar..."
	clojure -T:build uber
	@echo "Completed!"

.PHONY: upgrade
upgrade:
	@echo "Updating dependencies..."
	clojure -M:upgrade
	@echo "Completed!"

.PHONY: outdated
outdated:
	@echo "List outdated dependencies..."
	clojure -M:outdated
	@echo "Completed!"
