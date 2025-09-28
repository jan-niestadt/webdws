# Web-based XML editor

This describes the project goals. The user will update this file manually; the agent should not change it.

Goals for this project:
- a web-based XML editor
- targets non-technical users
- there must always be an XML schema
- the XML schema determines the user interface
- the document is validated against the XML schema
- if there are any schema violations, a descriptive message is shown in the UI where the invalid value or structure occurs
- 100% support of all XML Schema features is not required, but when an XML Schema only uses features supported by the application, it should be correctly enforced in accordance with the XML Schema specification, including which root elements are allowed, which child elements are allowed, element order, minimum/maximum numbers, etc.
