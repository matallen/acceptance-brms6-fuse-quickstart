@Billing
@Performance
@Set1

Feature: Billing Service - Performance tests

Scenario: NFR-01 - Billing processing must be able to process 20 files per minute 
Given the billing service is deployed
And 20 billing files are generated:
|Line Count|Duration|From       |From Country|To         |To Country |Type|
|3-5       |0-300   |<generated>|<generated> |<generated>|<generated>|Call|
When the billing files have been processed
Then all billing files should be processed within 60 seconds
And all billing file call records should match:
|To      |From     |
|.+XXXX$ |^[0-9]+$ |
