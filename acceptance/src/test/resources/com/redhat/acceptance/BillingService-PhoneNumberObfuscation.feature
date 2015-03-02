@Billing

Feature: Billing Service - Obfuscate phone numbers

Scenario: 01 - Regulation #4829 - outgoing numbers should be obfuscated on your bill
Given the billing service is deployed
And a billing file arrives with the following call records:
|Duration|From       |From Country|To         |To Country|Type|
|300     |07112284718|GBR         |07991380132|GBR       |Call|
|60      |07112284718|GBR         |07857131234|GBR       |Call|
|0       |07112284718|GBR         |07854250011|GBR       |SMS |
When the billing files have been processed
Then the billing file call records should match:
|Duration|From       |From Country|To         |To Country|Type|
|300     |07112284718|GBR         |0799138XXXX|GBR       |Call|
|60      |07112284718|GBR         |0785713XXXX|GBR       |Call|
|0       |07112284718|GBR         |0785425XXXX|GBR       |SMS |
