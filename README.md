# Important Notice
I am not maintaining this code anymore. However I will leave it here for those who needs some logic in here.

# HubSpot-Outreach Sync

HubSpot-Outreach Sync is a lambda function that you can deploy into your AWS account that provides synchronization between HubSpot CRM and Outreach CRM.

## How it works

OAuth2 authentication is used for authenticating in both CRM's. Related tokens, secrets etc... read from an S3 bucket specified.

An example HubSpot credential file expected by the function:
```
{
	"clientId":       "<client_id_here>",
	"clientSecret":   "<client_secret_here>",
	"refreshKey":     "<refresh_key_here>"
}
```
An example Outreach credential file expected by the function:
```
{
	"refreshKey":     "<refresh_key_here>",
	"clientId":       "<client_id_here>",
	"clientSecret":   "<client_secret_here>",
	"redirectUri":    "<redirect_uri_here>"
}
```
For simplicity it uses a lookup file generated, into a S3 bucket specified, to persist the last sync time. Last sync time is kept as timestamp in milliseconds. Here is an example lookup file:
```text
1592860359000
```
Function knows how to map HubSpot contact and Outreach prospects with a mapping file provided. An example property mapping file:
```
{
	"email":"emails",
	"contact_property_internal_name_1":"prospect_property_internal_name_1",
	"contact_property_internal_name_2":"prospect_property_internal_name_2"
}
```
In this case email<=>emails mapping is considered as a must field and needs to be in the file at all times.

Function knows where to look for credential and lookup files mentioned above via environment variables provided. List of required environment variables are listed below:
* **LOOKUP_BUCKET_NAME**: name of the bucket to persist last sync timestamp in milliseconds.
* **LOOKUP_KEY**: key of the file in lookup bucket to persist last sync timestamp in milliseconds.
* **HUBSPOT_OUTREACH_FIELD_MAPPING_OBJECT_KEY**: key of the file in lookup bucket that keeps mappings of the properties of HubSpot contact and Outreach prospect entities.
* **CREDENTIALS_BUCKET_NAME**: name of the bucket where credential files reside.
* **HUBSPOT_CREDENTIALS_OBJECT_KEY**: key of the HubSpot credential file in the credentials bucket.
* **OUTREACH_CREDENTIALS_OBJECT_KEY**: key of the Outreach credential file in the credentials bucket.

Every time the lambda function is triggered, it checks the recently modified contact list from HubSpot and creates/updates them as prospect entities in Outreach using mapping the definitions in the property mapping file provided. Algorithm goes back until the last modified timestamp of the contact is lower than the timestamp in the lookup file. Then function saves the max timestamp found among synced contacts to the lookup file in milliseconds. This way each time the function runs it only syncs the updates occurred after the last run.

## Installation

Simply compile the project and upload the fat jar file as a source code to your lambda function.

To operate properly it requires environment variables specified above and also lookup file needs to be generated with a proper timestamp in milliseconds before the first run.



## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
