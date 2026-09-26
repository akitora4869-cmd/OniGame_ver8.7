# OniGame v0.40.68

GitHub Actions のプロジェクト検出を修正。pom.xml を先に拾うのではなく、OniGamePlugin.java の実体を検索し、その src に対応する pom.xml を使用する。これにより複数 pom.xml や階層差で誤った PROJECT_DIR を選ぶ問題を防止する。
