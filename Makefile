.PHONY: run test checks parquet parquet2s3 parqlo

S3_BUCKET   := s3://parqlo
AWS_PROFILE := personal
SLUG        := war
PARQUET_OUT := data/derived

run:
	@mvn -q package -DskipTests && java -jar target/warrencromartie.jar

test:
	@mvn test

checks:
	@mvn spotless:check pmd:cpd-check

parquet: ## Download latest WAR data (if >24h old) and convert to Parquet
	@mvn -q package -DskipTests && java --enable-native-access=ALL-UNNAMED -jar target/warrencromartie.jar parquet

parqlo: ## Generate war.json samples from definitions/ and push to parqlo
	@mvn -q package -DskipTests && java -jar target/warrencromartie.jar parqlo

parquet2s3: parquet ## Download CSVs, convert to Parquet, upload to S3
	@aws sts get-caller-identity --profile $(AWS_PROFILE) > /dev/null || \
	  (echo "ERROR: AWS profile '$(AWS_PROFILE)' is not authenticated. Run: aws sso login --profile $(AWS_PROFILE)"; exit 1)
	aws s3 sync $(PARQUET_OUT) $(S3_BUCKET)/$(SLUG) --profile $(AWS_PROFILE)
