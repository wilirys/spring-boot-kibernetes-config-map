#### Using of Kibernetes Configmap in Spring Boot application

**1. Spring Boot Application property**

First let's add simple boot property to the MVC controller, no big deal:

```java
@RestController
public class ControllerMVC {

    @Value("${my.system.property:defaultValue}")
    protected String fromSystem;

    @RequestMapping("/say_hello")
    public String mvcTest() {
         return "Hello my majesty, your minions from " + fromSystem;
    }
}
```

and define the my.system.property into the application.properties:

    server.port=8081 
    my.system.property=fromFile

to test that property is loaded run:

```bash 
mvn clean install
```

and run spring application. So, output should be:

    Hello my majesty, your minions from fromFile

**2. Consume Kubernetes ConfigMap in Spring Boot**

Nice way of consuming the Kubernetes configmap in Spring Boot is through environment variables. So let's define environment variable my.system.property with value from configmap app-config in order to override value from application.properties:

First let's define the configmap app-config with key=my.system.property and value "fromAnsible":

```bash
kubectl create configmap app-config --from-literal=my.system.property=updatedFromAnsible
```

to see that configmap is well defined, run:

```bash 
kubectl describe configmap app-config
```

output should be:
```bash
Name:         app-config
Namespace:    default    
Labels:       none    
Annotations:  none    
Data    
====    
my.system.property:    
----    
updatedFromAnsible    
Events:  none
```

Okay, configmap is ready so let the kubernetes know about it and define environment
variable my.system.property reading the key with same name from configmap app-config.

```yaml
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  name: test-app
spec:
  replicas: 1
  template:
    metadata:
      labels:
        app: test-app
    spec:
      containers:
      - name: test-app
        image: test-controller:1.0-SNAPSHOT
        env:
        - name: my.system.property
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: my.system.property
        ports:
        - containerPort: 8081
```

Now upload the Deployment manifest to kubernetes with kubernetes service:

```bash
earthmor@pxbox:spring-boot-kubernetes-configmap$ kubectl get deployments
NAME             DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
test-app         1         1         1            1           1h
earthmor@pxbox:~/spring-boot-kubernetes-configmap$ kubectl get pods
NAME                             READY     STATUS    RESTARTS   AGE
test-app-745ff9546c-hrswc        1/1       Running   0          1h
earthmor@pxbox:spring-boot-kubernetes-configmap$ kubectl get services
NAME         TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)          AGE
kubernetes   ClusterIP   10.96.0.1      <none>        443/TCP          26d
test-app     NodePort    10.105.171.8   <none>        8081:30615/TCP   2h
```

since kubernetes service runs at:

```bash
earthmor@pxbox:spring-boot-kubernetes-configmap$ minikube service test-app --url
http://192.168.99.100:30615
```

hit the returned URL:

```bash
earthmor@pxbox:spring-boot-kubernetes-configmap$ curl http://192.168.99.100:30615/say_hello
Hello my majesty, your minions from updatedFromAnsible
```

Yes, we have overriden the spring property my.system.property via Kubernetes configmap
by injecting the property as environment variable, because env.variables have bigger priority
in Spring Boot then content of application.property file.
Updating the configMap

Let's play with configmap a little bit. What if we want to change the map value.
Easiest way of howto do it is by deleting map and create a new one.

```bash
earthmor@pxbox:spring-boot-kubernetes-configmap$ kubectl delete configmap app-config
configmap "app-config" deleted
earthmor@pxbox:spring-boot-kubernetes-configmap$ kubectl create configmap app-config --from-literal=my.system.property=changedValue
configmap "app-config" created
earthmor@pxbox:spring-boot-kubernetes-configmap$ kubectl delete configmap app-config
```

now curl the service again:

```bash
earthmor@pxbox:spring-boot-kubernetes-configmap$ curl http://192.168.99.100:30615/say_hello
Hello my majesty, your minions from updatedFromAnsible
```

new value of configmap wasn't reflected and we're having still the old one.

To see the actualized value of configmap we need to restart the POD. Since we use deployment where kubernetes is asked always run one replica we just need to delete the POD. New instance
will run eventually.

```bash 
earthmor@pxbox:spring-boot-kubernetes-configmap$ kubectl delete pod test-app-745ff9546c-hrswc
pod "test-app-745ff9546c-hrswc" deleted
earthmor@pxbox:spring-boot-kubernetes-configmap$ kubectl get pods
NAME                             READY     STATUS    RESTARTS   AGE
test-app-745ff9546c-t92hb        1/1       Running   0          31s
earthmor@pxbox:spring-boot-kubernetes-configmap$ curl http://192.168.99.100:30615/say_hello
Hello my majesty, your minions from changedValue
```

we see actual value! 