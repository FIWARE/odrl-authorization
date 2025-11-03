Feature: ODRL-authorization should validate tokens and apply policies.

  Scenario: A consumer cannot access a resource because token is empty.
    Given Provider has protected a resource with ODRL policies
    Given An empty token
    When Consumer requests access to a protected resource
    Then Consumer should receive an access denied response

  Scenario: A consumer cannot access a resource because token is not valid for the resource.
    Given Provider has protected a resource with ODRL policies
    Given A protected resource not allowed to the consumer
    When Consumer requests access to a protected resource
    Then Consumer should receive a forbidden response

  Scenario: A consumer access a protected resource
    Given Provider has protected a resource with ODRL policies
    When Consumer requests access to a protected resource
    Then Consumer should receive the resource information