.PHONY: run build test checks download download-br download-lahman download-retrosheet parquet parquet2s3 parqlo

S3_BUCKET   := s3://parqlo
AWS_PROFILE := personal
SLUG        := war
PARQUET_OUT := data/derived

build:
	@mvn -q package -DskipTests

run: build
	@java -jar target/warrencromartie.jar

test:
	@mvn test

checks:
	@mvn spotless:check pmd:cpd-check

download: ## Prompt to choose which data sources to download
	@./download.sh

download-br: ## Download latest BR WAR CSVs (if >24h old)
	@java -jar target/warrencromartie.jar br

download-lahman: ## Download Lahman CSVs from SABR Box (if >30 days old)
	@java -jar target/warrencromartie.jar lahman

download-retrosheet: ## Download Retrosheet gamelogs + Chadwick register, generate position parquets
	@java -jar target/warrencromartie.jar retrosheet

parquet: build download-br download-lahman download-retrosheet ## Refresh all data sources and convert to Parquet
	@java --enable-native-access=ALL-UNNAMED -jar target/warrencromartie.jar parquet

parqlo: build ## Generate war.json samples from definitions/ and push to parqlo
	@java -jar target/warrencromartie.jar parqlo

parquet2s3: parquet ## Refresh all data, convert to Parquet, upload to S3
	@aws sts get-caller-identity --profile $(AWS_PROFILE) > /dev/null || \
	  (echo "ERROR: AWS profile '$(AWS_PROFILE)' is not authenticated. Run: aws sso login --profile $(AWS_PROFILE)"; exit 1)
	aws s3 sync $(PARQUET_OUT) $(S3_BUCKET)/$(SLUG) --profile $(AWS_PROFILE)
