.PHONY: run test

run:
	@mvn -q package -DskipTests && java -jar target/warrencromartie.jar

test:
	@mvn test
