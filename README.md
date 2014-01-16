maven-replacer-plugin
=====================

based on http://code.google.com/p/maven-replacer-plugin


# maven-replacer-plugin



## Overview
	添加了多文件替换的支持，再configure中添加了files 和 outputFiles标签。

## Content
 
#### Compile & Package
	mvn clean -Dmaven.test.skip=true compile assembly:assembly


#### Install 
	mvn install:install-file -Dfile=target/replacer-1.6.0.jar -DgroupId=com.google.code.maven-replacer-plugin -DartifactId=replacer -Dversion=1.6.0 -Dpackaging=jar

#### Useage in pom.xml
	<plugin>
		<groupId>com.google.code.maven-replacer-plugin</groupId>
		<artifactId>replacer</artifactId>
		<version>${replacer.version}</version>
		<executions>
			<execution>
				<phase>process-sources</phase>
				<goals>
					<goal>replace</goal>
				</goals>
			</execution>
		</executions>
		<configuration>
			<files>
				<file>${host.template.file}</file>
				<file>${sckey.template.file}</file>
			</files>
			<outputFiles>
				<outputFile>${host.file}</outputFile>
				<outputFile>${sckey.file}</outputFile>
			</outputFiles>
			<replacements>
				<replacement>
					<token>@hosturl@</token>
					<value>http://github.com</value>
				</replacement>
				<replacement>
					<token>@imageurl@</token>
					<value>http://image.xxx.com</value>
				</replacement>
			</replacements>
		</configuration>
	</plugin>
	
#### Run
	mvn com.google.code.maven-replacer-plugin:replacer:replace