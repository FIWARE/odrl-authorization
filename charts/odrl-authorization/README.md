# odrl-authorization

![Version: 1.0.4](https://img.shields.io/badge/Version-1.0.4-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square)

Umbrella chart to deploy FIWARE ODRL Authorization

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| https://apache.github.io/apisix-helm-chart | apisix | 2.12.3 |
| https://fiware.github.io/helm-charts | odrl-pap | 2.3.0 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| apisix.apisix.admin.enabled | bool | `true` |  |
| apisix.apisix.customPlugins.enabled | bool | `true` |  |
| apisix.apisix.customPlugins.plugins[0].configMap.mounts[0].key | string | `"opa.lua"` |  |
| apisix.apisix.customPlugins.plugins[0].configMap.mounts[0].path | string | `"/opts/custom_plugins/apisix/plugins/opa.lua"` |  |
| apisix.apisix.customPlugins.plugins[0].configMap.mounts[1].key | string | `"helper.lua"` |  |
| apisix.apisix.customPlugins.plugins[0].configMap.mounts[1].path | string | `"/opts/custom_plugins/apisix/plugins/helper.lua"` |  |
| apisix.apisix.customPlugins.plugins[0].configMap.name | string | `"opa-lua"` |  |
| apisix.apisix.customPlugins.plugins[0].name | string | `"opa-lua"` |  |
| apisix.apisix.deployment.mode | string | `"traditional"` |  |
| apisix.catchAllRoute | object | `{"enabled":true,"oidc":{"clientId":"mySecuredService","discoveryEndpoint":"http://verifier:3000/services/mySecuredService/.well-known/openid-configuration"},"opa":{"host":"http://localhost:8181","policy":"policy/main","with_body":true},"upstream":{"type":"Service","url":"http://my-broker:8080"}}` | configuration of a catchAll-route(e.g. /*) |
| apisix.catchAllRoute.enabled | bool | `true` | enable catch all route |
| apisix.catchAllRoute.oidc | object | `{"clientId":"mySecuredService","discoveryEndpoint":"http://verifier:3000/services/mySecuredService/.well-known/openid-configuration"}` | configuration to verify the jwt, coming from the verifier |
| apisix.catchAllRoute.upstream | object | `{"type":"Service","url":"http://my-broker:8080"}` | configuration to connect the upstream broker |
| apisix.catchAllRoute.upstream.type | string | `"Service"` | Domain or Service |
| apisix.enabled | bool | `true` |  |
| apisix.etcd.enabled | bool | `true` |  |
| apisix.extraContainers[0] | object | `{"args":["run","--ignore=.*","--server","-l","debug","-c","/config/opa.yaml","--addr","0.0.0.0:8181","/tpp/tpp.rego"],"image":"openpolicyagent/opa:1.11.0","imagePullPolicy":"IfNotPresent","name":"open-policy-agent","ports":[{"containerPort":8181,"name":"opa-http","protocol":"TCP"}],"volumeMounts":[{"mountPath":"/config","name":"opa-config"},{"mountPath":"/tpp","name":"tpp-policy"}]}` | Deploy Open-Policy-Agent as sidecar |
| apisix.extraVolumes[0].configMap.name | string | `"tpp-policy"` |  |
| apisix.extraVolumes[0].name | string | `"tpp-policy"` |  |
| apisix.extraVolumes[1].configMap.name | string | `"opa-config"` |  |
| apisix.extraVolumes[1].name | string | `"opa-config"` |  |
| apisix.ingress-controller.config.kubernetes.defaultIngressClass | bool | `false` | Set apisix ingress as default ingress in kubernetes |
| apisix.ingress-controller.config.kubernetes.ingressClass | string | `"apisix"` | Name of the ingress class created in kubernetes |
| apisix.ingress-controller.enabled | bool | `true` | Enable the ingress controller pod to read Kubernetes Ingress resources. See [chart documentation](https://artifacthub.io/packages/helm/apisix/apisix-ingress-controller) for more details |
| apisix.ingress-controller.gatewayProxy.createDefault | bool | `true` | Controls whether to create a default GatewayProxy custom resource. |
| apisix.routes | dict | `[]` | Configuration of routes for apisix

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
 |
| fullnameOverride | string | `""` | String to fully override common.names.fullname |
| nameOverride | string | `""` | String to fully override common.names.namespace |
| odrl-pap.database | object | `{"existingSecret":{"enabled":true,"key":"postgres-admin-password","name":"database-secret"},"url":"jdbc:postgresql://postgresql:5432/pap","username":"postgres"}` | connection to the database |
| odrl-pap.database.existingSecret | object | `{"enabled":true,"key":"postgres-admin-password","name":"database-secret"}` | secret to take the password from |
| odrl-pap.database.url | string | `"jdbc:postgresql://postgresql:5432/pap"` | url to connect the db at |
| odrl-pap.database.username | string | `"postgres"` | username to access the db |
| odrl-pap.enabled | bool | `true` | Enable the deployment of odr-pap. OPA requires it; if disabled, you will need to deploy it manually. |
| odrl-pap.fullnameOverride | string | `"odrl-pap"` | allows to set a fixed name for the services |
| odrl-pap.image.pullPolicy | string | `"Always"` |  |
| odrl-pap.image.repository | string | `"mortega5/odr-pap"` |  |
| odrl-pap.image.tag | string | `"latest"` |  |
| opa.config | object | `{"data":{"maxDelay":15,"minDelay":1},"methods":{"maxDelay":3,"minDelay":1},"policies":{"maxDelay":4,"minDelay":2},"port":8181,"resourceUrl":"http://odrl-pap:8080/bundles/service/v1"}` | config for open policy agent. Deployed as a PDP |
| opa.config.data | object | `{"maxDelay":15,"minDelay":1}` | pull delays for the data bundle |
| opa.config.methods | object | `{"maxDelay":3,"minDelay":1}` | pull delays for the methods bundle |
| opa.config.policies | object | `{"maxDelay":4,"minDelay":2}` | pull delays for the policies bundle (in seconds) |
| opa.config.port | int | `8181` | port to make opa available at |
| opa.config.resourceUrl | string | `"http://odrl-pap:8080/bundles/service/v1"` | address of the odrl-pap to get the policies from |
| tpp | object | `{"enabled":false,"transfers":{"host":"","path":"/transfers"}}` | integration of checks for the transfer process protocol |
| tpp.enabled | bool | `false` | should checking for a running transfer process be enabled |
| tpp.transfers.host | string | `""` | host of the endpoint to check the process id, e.g. rainbow |
| tpp.transfers.path | string | `"/transfers"` | path to check the id at |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.14.2](https://github.com/norwoodj/helm-docs/releases/v1.14.2)
