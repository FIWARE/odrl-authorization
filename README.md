# odrl-authorization
[![](https://nexus.lab.fiware.org/repository/raw/public/badges/chapters/security.svg)](https://github.com/FIWARE/catalogue/tree/master/security/README.md)

The FIWARE ODRL Authorization (ODRL-Authorization) is an integrated suite of components designed to facilitate authorization using Verifiable Credentials.

This repository provides a description of the FIWARE Verifiable Credential Authorization and deployment recipes.

This project is part of [FIWARE](https://www.fiware.org/). For more information check the FIWARE Catalogue entry for
[Security](https://github.com/FIWARE/catalogue/tree/master/security).

| :books: [Documentation]()  |  :dart: [Roadmap](https://github.com/FIWARE/odrl-authorization/tree/master/doc/ROADMAP.md)|
|---|---|

<details>
<summary><strong>Table of Contents</strong></summary>

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Overview](#overview)
- [Release Information](#release-information)
- [Core components](#core-components)
  - [Deployment](#deployment)
    - [Local Deployment](#local-deployment)
    - [Deployment with Helm](#deployment-with-helm)
  - [Testing](#testing)
  - [APISIX Deployment Modes](#apisix-deployment-modes)
    - [Comparison Table](#comparison-table)
    - [1. With ETCD and with the Ingress Controller](#1-with-etcd-and-with-the-ingress-controller)
    - [2. With ETCD and without the Ingress Controller](#2-with-etcd-and-without-the-ingress-controller)
    - [3. Without ETCD and with the Ingress Controller](#3-without-etcd-and-with-the-ingress-controller)
    - [4. Without ETCD and without the Ingress Controller](#4-without-etcd-and-without-the-ingress-controller)
  - [How to contribute](#how-to-contribute)
  - [License](#license)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

</details>

# Overview

FIWARE ODRL Authorization enables management of access authorization to services using an Attribute-Based Access Control (ABAC) model expressed with ODRL policies. In this architecture, ODRL is the policy language for expressing permissions, constraints and obligations; those ODRL policies are translated into executable Rego rules that the Open Policy Agent (OPA) evaluates at runtime.

The goal is to deliver a pluggable, standards-aligned authorization plane that:

* Accepts access requests (including contexts derived from Verifiable Credentials / VPs) at the gateway (APISIX).

* Evaluates requests against ABAC policies authored in ODRL (after translation to Rego).

* Returns enforceable allow/deny decisions that APISIX uses to permit or block traffic

# Release Information
FIWARE ODRL Authorization uses a continious integration flow, where every merge to the main-branch triggers a new release. Versioning follows Semantic Versioning 2.0.0, therefor only major changes will contain breaking changes. Important releases will be listed below, with additional information linked:

# Core components

* [APISIX (Policy Enforcement Point — PEP)](https://apisix.apache.org/): intercepts incoming requests, performs credential/token validation and forwards the authorization query to Open Policy Agent (OPA). APISIX enforces the decision returned by the PDP.

* [Open Policy Agent (Policy Decision Point — PDP - OPA)](https://www.openpolicyagent.org/): evaluates the incoming request context against Rego policies and returns a decision (allow / deny). OPA fetchs policies from ODR-PAP for ABAC evaluation.

* [ODRL-PAP (Policy Administration Point / Translator)](https://github.com/wistefan/odrl-pap): stores and manages ODRL policies (the human/semantic representation) and translates them into Rego policy modules that OPA can execute. It serves as the policy lifecycle manager (create / update / push) for the authorization stack.

<p align="center">
<img src="doc/img/components.png">
</p>

## Deployment

### Local Deployment

The FIWARE ODRL Authorization provides a minimal local deployment setup intended for development and testing purposes.

The requirements for the local deployment are:
* [Maven](https://maven.apache.org/)
* Java Development Kit (at least v17)
* [Docker](https://www.docker.com/)
* [Helm](https://helm.sh/)
* [Helmfile](https://helmfile.readthedocs.io/en/latest/)

In order to interact with the system, the following tools are also helpful:
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
- [curl](https://curl.se/download.html)
- [jq](https://stedolan.github.io/jq/download/)
- [yq](https://mikefarah.gitbook.io/yq/)

> :warning: In current Linux installations, ```br_netfilter``` is disabled by default. That leads to networking issues inside the k3s cluster and will prevent the connector to start up properly. Make sure that its enabled via ```modprobe br_netfilter```. See [Stackoverflow](https://stackoverflow.com/questions/48148838/kube-dns-error-reply-from-unexpected-source/48151279#48151279) for more.

To start the deployment, just use:

```shell
    mvn clean deploy -Plocal
```

### Deployment with Helm

The odrl-authorization is a [Helm Umbrella-Chart](https://helm.sh/docs/howto/charts_tips_and_tricks/#complex-charts-with-many-dependencies), containing all the sub-charts of the different components and their dependencies. Its sources can be found[here](./charts/odrl-authorization).

The chart is available at the repository ```https://fiware.github.io/odrl-authorization/```. You can install it via:

```shell
    # add the repo
    helm repo add odrl-authorization https://fiware.github.io/odrl-authorization/
    # install the chart
    helm install <DeploymentName> odrl-authorization/odrl-authorization -n <Namespace> -f values.yaml
```
**Note,** that due to the app-of-apps structure of the deployment and the different dependencies between the components, a deployment without providing any configuration values will not work. Make sure to provide a
`values.yaml` file for the deployment, specifying all necessary parameters. This includes setting parameters of the endpoints, DNS information (providing Ingress or OpenShift Route parameters),
structure and type of the required VCs, internal hostnames of the different components and providing the configuration of the DID and keys/certs.

Configurations for all sub-charts (and sub-dependencies) can be managed through the top-level [values.yaml](./charts/odrl-authorization/values.yaml) of the chart. It contains the default values of each component and additional parameter shared between the components. The configuration of the applications can be changed under the key ```<APPLICATION_NAME>```, please see the individual applications and there sub-charts for the available options.

The chart is [published and released](./github/workflows/release-helm.yaml) on each merge to master.

## Testing

In order to test the [helm-chart](./charts/odrl-authorization) provided for the FIWARE ODRL authorization, an integration-test
framework based on [Cucumber](https://cucumber.io/) and [Junit5](https://junit.org/junit5/) is provided: [it](./it).

The tests can be executed via:
```shell
    mvn clean integration-test -Ptest
```
They will spin up the [Local Deployment](#local-deployment) and run
the [test-scenarios](./it/src/test/resources/it/mvds_basic.feature) against it.


## APISIX Deployment Modes

APISIX can operate in four distinct deployment modes. Each mode determines how routes are stored, managed, and persisted, as well as which components are responsible for maintaining the routing configuration.

### Comparison Table

| Mode                                               | ETCD | Ingress Controller | Route Source                               | Persistence                    | Notes                                      |
| -------------------------------------------------- | ---- | ------------------ | ------------------------------------------ | ------------------------------ | ------------------------------------------ |
| **1. With ETCD and with Ingress Controller**       | ✔️   | ✔️                 | APISIX CRDs, Kubernetes Ingress, Admin API | ✔️ Persisted in ETCD           | Recommended for Kubernetes-native setups   |
| **2. With ETCD and without Ingress Controller**    | ✔️   | ❌                  | Admin API only                             | ✔️ Persisted in ETCD           | Chart-defined routes are *not* initialized |
| **3. Without ETCD and with Ingress Controller**    | ❌    | ✔️                 | APISIX CRDs, Kubernetes Ingress, Admin API | ❌ In-memory only               | Requires at least one route to start       |
| **4. Without ETCD and without Ingress Controller** | ❌    | ❌                  | Static ConfigMap (`apisix.yaml`)           | ✔️ Persisted only in ConfigMap | **Under development**; installation may fail but upgrades will work                 |

---

### 1. With ETCD and with the Ingress Controller

In this mode, APISIX persists all route definitions in ETCD. Routes may be defined via APISIX CRDs, standard Kubernetes Ingress resources, or the Admin API.
Because the configuration is stored in ETCD, all routes—including those created through the Admin API—will **remain available after restarts**.

```yaml
apisix:
  ingress-controller:
    enabled: true
  apisix:
    deployment:
      role: traditional
      role_traditional:
        config_provider: yaml
  etcd:
    enabled: true
```

---

### 2. With ETCD and without the Ingress Controller

In this configuration, ETCD persists the routes, but no Ingress Controller is available to manage them. As a result, routes can **only** be created or updated using the APISIX Admin API.
Chart-defined routes are **not** initialized automatically.

```yaml
apisix:
  ingress-controller:
    enabled: false
  apisix:
    deployment:
      role: traditional
      role_traditional:
        config_provider: yaml
  etcd:
    enabled: true
```

---

### 3. Without ETCD and with the Ingress Controller

When ETCD is disabled, APISIX loads all routes from APISIX CRDs and stores them in memory. The Ingress Controller continuously synchronizes APISIX with these CRDs.
Although the Admin API can still modify routes, such changes **will not persist across restarts**.
Kubernetes Ingress objects may also be used to define new routes.

> [!WARNING]
> APISIX requires at least one route to exist for the service to start correctly.

```yaml
apisix:
  ingress-controller:
    enabled: true
  apisix:
    deployment:
      role: traditional
      role_traditional:
        config_provider: yaml
  etcd:
    enabled: false
```

---

### 4. Without ETCD and without the Ingress Controller

In this mode, routes are defined statically within the `apisix.yaml` ConfigMap. APISIX loads these routes at startup, and the configuration remains unchanged unless the ConfigMap or Helm values are manually updated.
This mode is suitable for simple or fully static environments.

> [!WARNING]
> This mode is currently under development. Installation may fail, but upgrades will function correctly.

```yaml
apisix:
  ingress-controller:
    enabled: false
  apisix:
    deployment:
      mode: standalone
      role: data_plane
  etcd:
    enabled: false
```

<!-- BEGIN HELM DOCS -->


## Values

<table>
	<thead>
		<th>Key</th>
		<th>Type</th>
		<th>Default</th>
		<th>Description</th>
	</thead>
	<tbody>
		<tr>
			<td>apisix.apisix.admin.enabled</td>
			<td>bool</td>
			<td><pre lang="json">
true
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.apisix.customPlugins.enabled</td>
			<td>bool</td>
			<td><pre lang="json">
true
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.apisix.customPlugins.plugins[0].configMap.mounts[0].key</td>
			<td>string</td>
			<td><pre lang="json">
"opa.lua"
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.apisix.customPlugins.plugins[0].configMap.mounts[0].path</td>
			<td>string</td>
			<td><pre lang="json">
"/opts/custom_plugins/apisix/plugins/opa.lua"
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.apisix.customPlugins.plugins[0].configMap.mounts[1].key</td>
			<td>string</td>
			<td><pre lang="json">
"helper.lua"
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.apisix.customPlugins.plugins[0].configMap.mounts[1].path</td>
			<td>string</td>
			<td><pre lang="json">
"/opts/custom_plugins/apisix/plugins/helper.lua"
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.apisix.customPlugins.plugins[0].configMap.name</td>
			<td>string</td>
			<td><pre lang="json">
"opa-lua"
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.apisix.customPlugins.plugins[0].name</td>
			<td>string</td>
			<td><pre lang="json">
"opa-lua"
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.apisix.deployment.mode</td>
			<td>string</td>
			<td><pre lang="json">
"traditional"
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.catchAllRoute</td>
			<td>object</td>
			<td><pre lang="json">
{
  "enabled": true,
  "oidc": {
    "clientId": "mySecuredService",
    "discoveryEndpoint": "http://verifier:3000/services/mySecuredService/.well-known/openid-configuration"
  },
  "opa": {
    "host": "http://localhost:8181",
    "policy": "policy/main",
    "with_body": true
  },
  "upstream": {
    "type": "Service",
    "url": "http://my-broker:8080"
  }
}
</pre>
</td>
			<td>configuration of a catchAll-route(e.g. /*)</td>
		</tr>
		<tr>
			<td>apisix.catchAllRoute.enabled</td>
			<td>bool</td>
			<td><pre lang="json">
true
</pre>
</td>
			<td>enable catch all route</td>
		</tr>
		<tr>
			<td>apisix.catchAllRoute.oidc</td>
			<td>object</td>
			<td><pre lang="json">
{
  "clientId": "mySecuredService",
  "discoveryEndpoint": "http://verifier:3000/services/mySecuredService/.well-known/openid-configuration"
}
</pre>
</td>
			<td>configuration to verify the jwt, coming from the verifier</td>
		</tr>
		<tr>
			<td>apisix.catchAllRoute.upstream</td>
			<td>object</td>
			<td><pre lang="json">
{
  "type": "Service",
  "url": "http://my-broker:8080"
}
</pre>
</td>
			<td>configuration to connect the upstream broker</td>
		</tr>
		<tr>
			<td>apisix.catchAllRoute.upstream.type</td>
			<td>string</td>
			<td><pre lang="json">
"Service"
</pre>
</td>
			<td>Domain or Service</td>
		</tr>
		<tr>
			<td>apisix.enabled</td>
			<td>bool</td>
			<td><pre lang="json">
true
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.etcd.enabled</td>
			<td>bool</td>
			<td><pre lang="json">
true
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.extraContainers[0]</td>
			<td>object</td>
			<td><pre lang="json">
{
  "args": [
    "run",
    "--ignore=.*",
    "--server",
    "-l",
    "debug",
    "-c",
    "/config/opa.yaml",
    "--addr",
    "0.0.0.0:8181",
    "/tpp/tpp.rego"
  ],
  "image": "openpolicyagent/opa:1.11.0",
  "imagePullPolicy": "IfNotPresent",
  "name": "open-policy-agent",
  "ports": [
    {
      "containerPort": 8181,
      "name": "opa-http",
      "protocol": "TCP"
    }
  ],
  "volumeMounts": [
    {
      "mountPath": "/config",
      "name": "opa-config"
    },
    {
      "mountPath": "/tpp",
      "name": "tpp-policy"
    }
  ]
}
</pre>
</td>
			<td>Deploy Open-Policy-Agent as sidecar</td>
		</tr>
		<tr>
			<td>apisix.extraVolumes[0].configMap.name</td>
			<td>string</td>
			<td><pre lang="json">
"tpp-policy"
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.extraVolumes[0].name</td>
			<td>string</td>
			<td><pre lang="json">
"tpp-policy"
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.extraVolumes[1].configMap.name</td>
			<td>string</td>
			<td><pre lang="json">
"opa-config"
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.extraVolumes[1].name</td>
			<td>string</td>
			<td><pre lang="json">
"opa-config"
</pre>
</td>
			<td></td>
		</tr>
		<tr>
			<td>apisix.ingress-controller.config.kubernetes.defaultIngressClass</td>
			<td>bool</td>
			<td><pre lang="json">
false
</pre>
</td>
			<td>Set apisix ingress as default ingress in kubernetes</td>
		</tr>
		<tr>
			<td>apisix.ingress-controller.config.kubernetes.ingressClass</td>
			<td>string</td>
			<td><pre lang="json">
"apisix"
</pre>
</td>
			<td>Name of the ingress class created in kubernetes</td>
		</tr>
		<tr>
			<td>apisix.ingress-controller.enabled</td>
			<td>bool</td>
			<td><pre lang="json">
true
</pre>
</td>
			<td>Enable the ingress controller pod to read Kubernetes Ingress resources. See [chart documentation](https://artifacthub.io/packages/helm/apisix/apisix-ingress-controller) for more details</td>
		</tr>
		<tr>
			<td>apisix.ingress-controller.gatewayProxy.createDefault</td>
			<td>bool</td>
			<td><pre lang="json">
true
</pre>
</td>
			<td>Controls whether to create a default GatewayProxy custom resource.</td>
		</tr>
		<tr>
			<td>apisix.routes</td>
			<td>dict</td>
			<td><pre lang="json">
[]
</pre>
</td>
			<td>Configuration of routes for apisix

```yaml
uri: /*
  host: host-name-test
  type: Service
  namespace: super-ns # release namespace by default
  upstream:
    nodes:
      data-service-test:9090: 1
    type: roundrobin
  plugins:
    openid-connect:
      bearer_only: true
      use_jwks: true
      client_id: data-service
      client_secret: unused
      discovery: https://verifier:8080/services/data-service/.well-known/openid-configuration
    opa:
      host: "http://localhost:8181"
      policy: policy/main
      with_body: true
```
</td>
		</tr>
		<tr>
			<td>fullnameOverride</td>
			<td>string</td>
			<td><pre lang="json">
""
</pre>
</td>
			<td>String to fully override common.names.fullname</td>
		</tr>
		<tr>
			<td>nameOverride</td>
			<td>string</td>
			<td><pre lang="json">
""
</pre>
</td>
			<td>String to fully override common.names.namespace</td>
		</tr>
		<tr>
			<td>odrl-pap.database</td>
			<td>object</td>
			<td><pre lang="json">
{
  "existingSecret": {
    "enabled": true,
    "key": "postgres-admin-password",
    "name": "database-secret"
  },
  "url": "jdbc:postgresql://postgresql:5432/pap",
  "username": "postgres"
}
</pre>
</td>
			<td>connection to the database</td>
		</tr>
		<tr>
			<td>odrl-pap.database.existingSecret</td>
			<td>object</td>
			<td><pre lang="json">
{
  "enabled": true,
  "key": "postgres-admin-password",
  "name": "database-secret"
}
</pre>
</td>
			<td>secret to take the password from</td>
		</tr>
		<tr>
			<td>odrl-pap.database.url</td>
			<td>string</td>
			<td><pre lang="json">
"jdbc:postgresql://postgresql:5432/pap"
</pre>
</td>
			<td>url to connect the db at</td>
		</tr>
		<tr>
			<td>odrl-pap.database.username</td>
			<td>string</td>
			<td><pre lang="json">
"postgres"
</pre>
</td>
			<td>username to access the db</td>
		</tr>
		<tr>
			<td>odrl-pap.enabled</td>
			<td>bool</td>
			<td><pre lang="json">
true
</pre>
</td>
			<td>Enable the deployment of odr-pap. OPA requires it; if disabled, you will need to deploy it manually.</td>
		</tr>
		<tr>
			<td>odrl-pap.fullnameOverride</td>
			<td>string</td>
			<td><pre lang="json">
"odrl-pap"
</pre>
</td>
			<td>allows to set a fixed name for the services</td>
		</tr>
		<tr>
			<td>opa.config</td>
			<td>object</td>
			<td><pre lang="json">
{
  "data": {
    "maxDelay": 15,
    "minDelay": 1
  },
  "methods": {
    "maxDelay": 3,
    "minDelay": 1
  },
  "policies": {
    "maxDelay": 4,
    "minDelay": 2
  },
  "port": 8181,
  "resourceUrl": "http://odrl-pap:8080/bundles/service/v1"
}
</pre>
</td>
			<td>config for open policy agent. Deployed as a PDP</td>
		</tr>
		<tr>
			<td>opa.config.data</td>
			<td>object</td>
			<td><pre lang="json">
{
  "maxDelay": 15,
  "minDelay": 1
}
</pre>
</td>
			<td>pull delays for the data bundle</td>
		</tr>
		<tr>
			<td>opa.config.methods</td>
			<td>object</td>
			<td><pre lang="json">
{
  "maxDelay": 3,
  "minDelay": 1
}
</pre>
</td>
			<td>pull delays for the methods bundle</td>
		</tr>
		<tr>
			<td>opa.config.policies</td>
			<td>object</td>
			<td><pre lang="json">
{
  "maxDelay": 4,
  "minDelay": 2
}
</pre>
</td>
			<td>pull delays for the policies bundle (in seconds)</td>
		</tr>
		<tr>
			<td>opa.config.port</td>
			<td>int</td>
			<td><pre lang="json">
8181
</pre>
</td>
			<td>port to make opa available at</td>
		</tr>
		<tr>
			<td>opa.config.resourceUrl</td>
			<td>string</td>
			<td><pre lang="json">
"http://odrl-pap:8080/bundles/service/v1"
</pre>
</td>
			<td>address of the odrl-pap to get the policies from</td>
		</tr>
		<tr>
			<td>tpp</td>
			<td>object</td>
			<td><pre lang="json">
{
  "enabled": false,
  "transfers": {
    "host": "",
    "path": "/transfers"
  }
}
</pre>
</td>
			<td>integration of checks for the transfer process protocol</td>
		</tr>
		<tr>
			<td>tpp.enabled</td>
			<td>bool</td>
			<td><pre lang="json">
false
</pre>
</td>
			<td>should checking for a running transfer process be enabled</td>
		</tr>
		<tr>
			<td>tpp.transfers.host</td>
			<td>string</td>
			<td><pre lang="json">
""
</pre>
</td>
			<td>host of the endpoint to check the process id, e.g. rainbow</td>
		</tr>
		<tr>
			<td>tpp.transfers.path</td>
			<td>string</td>
			<td><pre lang="json">
"/transfers"
</pre>
</td>
			<td>path to check the id at</td>
		</tr>
	</tbody>
</table>
<!-- END HELM DOCS -->

## How to contribute

Please, check the doc [here](doc/CONTRIBUTING.md).

## License
odrl-authorization is licensed under [Apache v2.0](LICENSE).

For the avoidance of doubt, the owners of this software
wish to make a clarifying public statement as follows:

> Please note that software derived as a result of modifying the source code of this
> software in order to fix a bug or incorporate enhancements is considered a derivative
> work of the product. Software that merely uses or aggregates (i.e. links to) an otherwise
> unmodified version of existing software is not considered a derivative work, and therefore
> it does not need to be released as under the same license, or even released as open source.