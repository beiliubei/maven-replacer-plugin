mvn clean -Dmaven.test.skip=true compile assembly:assembly

mvn install:install-file -Dfile=target/replacer-1.6.0.jar -DgroupId=com.google.code.maven-replacer-plugin -DartifactId=replacer -Dversion=1.6.0 -Dpackaging=jar