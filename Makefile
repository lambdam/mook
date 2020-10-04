build:
	clj --main cljs.main --compile-opts build.edn --compile

serve:
	clj --main cljs.main --serve

generate-jar:
	clojure -A:pack \
	mach.pack.alpha.skinny \
	--no-libs \
	--project-path mook.jar

deploy-to-clojars: generate-jar
	env $(shell cat ~/.clojars-credentials) clojure -A:deploy
