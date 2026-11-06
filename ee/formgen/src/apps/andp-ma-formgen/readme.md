# TODO für 8.5

## GPU-2306
Da wird schon den richtigen Plugin benutzt. Allerdings wurden folgende Dinge noch hard coded: 
1. Form Flow: Form Flow wird nicht mehr aus feature geladen, sondern beim Laden vom Formular. Derzeit habe ich Code als hard coded configuration drinnen im code
2. Im App.tsx von andp-ma-formgen wird der plugin name hard coded als "acta-nova" drinnen im code. Das sollte man aus dem HTML Attribut "data-plugin-name" auslesen. adiese Funktionalität ist noch nicht da.

## GPU-2310 - BooleanElement
Wenn das Backend implementiert ist, sollte nochmal geschaut werden, ob alle Element-Attribut-Definitionen gleich sind. 

# Beispiel Config mit Payment
```json
{
    "meshEndpoint": "http://portal.localhost.gentics.com",
    "andpFormFlowSteps": [
        {
            "buttonText": "Start Seite",
            "name": "form_flow_name_startpage",
            "description": "form_flow_desc_startpage",
            "reactClass": "StartInfoScreen",
            "buttonDefinition": [
                {
                    "btnComponent": "CancelButton",
                    "translateKey": "form_cancel"
                },
                {
                    "btnComponent": "NextPageButton",
                    "translateKey": "form_nextPage"
                }
            ],
            "variables": [

            ]
        },
        {
            "buttonText": "Zum Formular",
            "reactClass": "FormScreen",
            "name": "form_flow_name_form",
            "description": "form_flow_desc_form",
            "buttonDefinition": [
                {
                    "btnComponent": "CancelButton",
                    "translateKey": "form_cancel"
                },
                {
                    "btnComponent": "PreviousPageButton",
                    "translateKey": "form_previousPage"
                },
                {
                    "btnComponent": "NextPageButton",
                    "translateKey": "form_nextPage"
                }
            ],
            "variables": [
                
            ]
        },
        {
            "buttonText": "Zur Zusammenfassung",
            "reactClass": "SummaryScreen",
            "name": "form_flow_name_summary",
            "description": "form_flow_desc_summary",
            "buttonDefinition": [
                {
                    "btnComponent": "CancelButton",
                    "translateKey": "form_cancel"
                },
                {
                    "btnComponent": "PreviousPageButton",
                    "translateKey": "form_previousPage"
                },
                {
                    "btnComponent": "NextPageButton",
                    "translateKey": "form_nextPage"
                }
            ],
            "variables": [
                {
                    "name": "showDetails",
                    "value" : true
                }
            ]
        },
        {
            "buttonText": "Zur Bezahlung",
            "reactClass": "PaymentScreen",
            "name": "form_flow_name_payment",
            "description": "form_flow_desc_payment",
            "buttonDefinition": [
                {
                    "btnComponent": "CancelButton",
                    "translateKey": "form_cancel"
                },
                {
                    "btnComponent": "NextPageButton",
                    "translateKey": "form_nextPage"
                },
                {
                    "btnComponent": "SubmitButton",
                    "translateKey": "form_submit"
                }
            ],
            "variables": [
                
            ]
        },
        {
            "buttonText": "Successpage",
            "reactClass": "SuccessScreen",
            "name": "form_flow_name_success",
            "description": "form_flow_desc_success",
            "buttonDefinition": [],
            "variables": [
                
            ]
        }
    ]
}

```