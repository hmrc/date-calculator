
# date-calculator

This service runs on port `8762` by default. Run using `sbt run`. Tests can be run via `sbt test` and integration tests can be run via `it/test`.

The service uses [GDS get bank holidays API](https://www.api.gov.uk/gds/bank-holidays/#bank-holidays) to retrieve a list of
bank holidays which are locally stored. The [date-calculator-stubs service](https://github.com/hmrc/date-calculator-stubs)
can be used to stub out this API.

---

## GET /date-calculator/add-working-days
Adds or subtracts a number of working days to a date. The request JSON should look like this:
```json
{
  "date": "2023-09-26",            
  "numberOfWorkingDaysToAdd": 1,   
  "regions": [ "EW" ]               
}
```
where the parameters are as follows:

| parameter                | description                                                                                                                                                                                                                                                                                                            |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| date                     | date to add/subtract working days to                                                                                                                                                                                                                                                                                   |
 | numberOfWorkingDaysToAdd | integer number of working days to add or subtract. Use a positive number for addition and a negative number for subtraction                                                                                                                                                                                            |
 | regions                  | non-empty array of regions in the UK to consider. Allowed values are:<br> <ul> <li> "EW" (England and Wales)</li><li>"SC" (Scotland) </li><li>"NI" (Northern Ireland)</li></ul>Only the bank holidays which are defined for the given regions will be considered in the bank holiday calculations. |

An example curl request is:
```shell
curl -v -H 'Content-Type: application/json' -d '{"date": "2023-09-26", "numberOfWorkingDaysToAdd": 1,  "regions": [ "EW", "SC", "NI" ]}' http://localhost:8762/date-calculator/add-working-days
```

The following responses are given:

| HTTP status                 | description                                                                                                                     |
|-----------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| 200 (OK)                    | calculation was successfully performed - result can be found in JSON response body:<br><code>{ "result": "2023-02-27" }</code> |
 | 400 (Bad Request)           | JSON request body cannot be parsed |
 | 422 (Unprocessable Entity)  | dates being considered in the calculation go outside of the bounds of the earliest and latest bank holidays stored in the service |
 | 500 (Internal Server Error) | bank holiday list not locally stored and cannot be retrieved |


---
### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").