default:
	@echo Welcome to mook todomvc

copy-assets:
	mkdir	-p public/css && \
	cp assets/index.html public && \
	cp node_modules/todomvc-app-css/index.css public/css/todomvc.css

compile:
	yarn shadow-cljs compile app

watch:
	yarn shadow-cljs watch app
