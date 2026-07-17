.PHONY: run test checks

run:
	@mvn -q package -DskipTests && java -jar target/warrencromartie.jar

test:
	@mvn test

checks:
	@mvn spotless:check pmd:cpd-check
