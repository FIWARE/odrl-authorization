Feature: ODRL-authorization should validate tokens and apply policies.

  Scenario: A consumer cannot access a resource.
    Given Provider has protected a resource with ODRL policies
    Given An empty token
    When Consumer requests access to a protected resource without a valid token
    Then Consumer should receive an access denied response
