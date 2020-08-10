build:
	clj --main cljs.main --compile-opts build.edn --compile

serve:
	clj --main cljs.main --serve
