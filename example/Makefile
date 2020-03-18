default:
	@echo Welcome to mook todomvc

copy-assets:
	mkdir	-p resources/public/css && \
	cp node_modules/todomvc-app-css/index.css resources/public/css/todomvc.css

compile:
	yarn shadow-cljs compile app

watch:
	yarn shadow-cljs watch app
