# Sección: WebClient: consumiendo servicios RestFul

---

En esta sección crearemos este microservicio que consumirá el servicio rest desarrollado en el proyecto
[**spring-webflux-api-rest**](https://github.com/magadiflo/spring-webflux-api-rest.git)

## Creando nuestro microservicio cliente

Cambiamos el puerto de este microservicio, ya que por defecto es el 8080 quien ya se encuentra ocupado por el
microservicio **spring-webflux-api-rest**:

````properties
server.port=8095
````

Ahora, configuramos nuestro **cliente http (WebClient)** quien nos permitirá consumir el api rest. Creamos el
componente WebClient y lo registramos en el contendor de Spring para que después lo podamos inyectar en otras clases.

````java

@Configuration
public class ApplicationConfig {
    @Bean
    public WebClient webClient() {
        return WebClient.create("http://localhost:8080/api/v2/products");
    }
}
````

WebClient tiene su método estático `WebClient.create()` para poder crear un nuevo **WebClient** con Reactor Netty por
defecto. Ahora, existe además una variante de ese método y es el que usamos en el código anterior donde le pasámos una
url base, esa **url base se usará para todos los request que se hagan con WebClient.**

## Creando las clases models DTO

Crearemos nuestros DTOs Category y Product correspondiente al Category y Product del microservicio
**spring-webflux-api-rest**:

````java
public record Category(String id, String name) {
}
````

````java
public record Product(String id, String name, Double price, LocalDate createAt, String image, Category category) {
}
````

**NOTA**

> Estoy creando los DTOs con Records para que sean datos inmutables, ya que utilizaremos estos DTOs para recepcionar
> información del endpoint. **Es importante que los nombres de los records y sus propiedades sean iguales a los nombres
> de las clases y sus propiedades con las que son enviadas** para que el nombre de las clases y atributos **se mapeen
> correctamente.**

## Creando el componente Service implementado con WebClient

Crearemos primero la interfaz que tendrá todos los métodos a implementar:

````java
public interface IProductService {
    Flux<Product> findAllProducts();

    Mono<Product> findProduct(String id);

    Mono<Product> saveProduct(Product product);

    Mono<Product> updateProduct(String id, Product product);

    Mono<Boolean> deleteProduct(String id);
}
````

Realizamos la implementación concreta de nuestra interfaz **IProductService** donde inyectaremos el client http
**WebClient** que configuramos como un @Bean en la clase de configuración **ApplicationConfig**:

````java

@Service
public class ProductServiceImpl implements IProductService {

    private final WebClient client;

    public ProductServiceImpl(WebClient client) {
        this.client = client;
    }

    @Override
    public Flux<Product> findAllProducts() {
        return this.client.get()
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToFlux(response -> response.bodyToFlux(Product.class));
    }

    @Override
    public Mono<Product> findProduct(String id) {
        return this.client.get().uri("/{id}", Collections.singletonMap("id", id))
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> response.bodyToMono(Product.class));
    }

    @Override
    public Mono<Product> saveProduct(Product product) {
        return null;
    }

    @Override
    public Mono<Product> updateProduct(String id, Product product) {
        return null;
    }

    @Override
    public Mono<Boolean> deleteProduct(String id) {
        return null;
    }
}
````

Como observamos en el código anterior, por el momento solo hemos implementado los métodos **findAllProducts()** y el
método **findProduct()**. Ambos métodos realizan casi lo mismo, con la diferencia que uno recupera todos los productos
y el oto método solo un producto, así que explicaré la implementación del método **findProduct()**:

````java

@Service
public class ProductServiceImpl implements IProductService {
    /* omitted code */
    @Override
    public Mono<Product> findProduct(String id) {
        return this.client.get().uri("/{id}", Collections.singletonMap("id", id))
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> response.bodyToMono(Product.class));
    }
    /* omitted code */
}
````

- En el método utilizamos el **cliente http WebClient** que inyectamos por constructor para hacer la petición `get()`
  hacia el endpoint que configuramos en el @Bean de la clase **ApplicationConfig**.
- Como vamos a buscar un producto, necesitamos completar el url agregándole la uri `/{id}` para buscar el producto por
  el identificador que le pasemos.
- El operador `accept()` define el tipo de **MediaType** que esperamos recibir como Response del endpoint a consultar.
- El operador `exchangeToMono` (alternativa al operador `retrieve()`) nos permite crear un **Mono** a partir de la
  respuesta obtenida.

   ````
   exchangeToMono(response -> response.bodyToMono(Product.class));
   ````
  Por ahora, la implementación anterior con el `exchangeToMono()` es suficiente, no necesitamos hacer más. Ahora,
  como estamos en el endpoint que busca un producto, puede ser que el id pasado por parámetro no exista y el endpoint
  al que consultamos nos retornará un **404 Not Fount**, no debemos preocuparnos, ya que nuestro **ProductServiceImpl**
  **findProduct()** al ver que el endpoint le retorna un Mono vacío, eso es lo que le mandará al **ProductHanlder**, y
  es en el **ProductHandler** que manejamos esa alternativa utilizando el operador
  `switchIfEmpty(ServerResponse.notFound().build())`.

  Una cosa adicional sobre el `exchangeToMono` es que brinda más control a través del acceso a **ClientResponse**. Esto
  puede ser útil para escenarios avanzados, por ejemplo, para decodificar la respuesta de manera diferente según el
  estado de la respuesta. Un ejemplo de uso podría ser el siguiente:

   ````
  ...
  .exchangeToMono(response -> {
      if (response.statusCode().equals(HttpStatus.OK)) {
          return response.bodyToMono(Product.class);
      }
      return response.createError();
  });
   ````

Ahora, en el código implementado por Andrés Guzmán utiliza el operador `retrieve()` quien brinda acceso al estado de la
respuesta y los encabezados a través de ResponseEntity junto con el manejo del estado de error.

````java

@Service
public class ProductServiceImpl implements IProductService {
    /* omitted code */
    @Override
    public Mono<ProductDTO> findProduct(String id) {
        return this.client.get().uri("/{id}", Collections.singletonMap("id", id))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Producto.class);
    }
    /* omitted code */
}
````

En mi caso, seguiré usando el `exchangeToMono` o `exchangeToFlux`.

## Implementando el CRUD en el Service

Ahora toca implementar los demás métodos de nuestro **ProductServiceImpl**, estas implementaciones serán similares a lo
que expliqué para los métodos **findAllProducts()** y **findProduct()**:

````java

@Service
public class ProductServiceImpl implements IProductService {

    /* other code */

    @Override
    public Mono<Product> saveProduct(Product product) {
        return this.client.post()
                .contentType(MediaType.APPLICATION_JSON)    // <-- tipo de contenido que enviamos en el Request
                .accept(MediaType.APPLICATION_JSON)         // <-- tipo de contenido que aceptamos en el Response
                .bodyValue(product)
                .exchangeToMono(response -> response.bodyToMono(Product.class));
    }

    @Override
    public Mono<Product> updateProduct(String id, Product product) {
        return this.client.put().uri("/{id}", Collections.singletonMap("id", id))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(product)
                .exchangeToMono(response -> response.bodyToMono(Product.class));
    }

    @Override
    public Mono<Boolean> deleteProduct(String id) {
        return this.client.delete().uri("/{id}", Collections.singletonMap("id", id))
                .exchangeToMono(response -> response.statusCode().equals(HttpStatus.NO_CONTENT) ? Mono.just(true) : Mono.just(false));
    }
}
````

## Creando el componente Handler implementado con el RouterFunction

Crearemos nuestros handlerFunctions en el componente ProductHandler. Es importante resaltar que **todo método handler
siempre va a retornar** un `Mono<ServerResponse>`:

````java

@Component
public class ProductHandler {
    private final IProductService productService;

    public ProductHandler(IProductService productService) {
        this.productService = productService;
    }

    public Mono<ServerResponse> findAllProducts(ServerRequest request) {
        Flux<Product> productFlux = this.productService.findAllProducts();
        return ServerResponse.ok().body(productFlux, Product.class);
    }

    public Mono<ServerResponse> showProduct(ServerRequest request) {
        String id = request.pathVariable("id");
        return this.productService.findProduct(id)
                .flatMap(product -> ServerResponse.ok().bodyValue(product))
                .switchIfEmpty(ServerResponse.notFound().build()); //<-- Cuando el producto buscado no existe
    }
}
````

Ahora toca crear la clase de configuración para nuestros **RouterFunctions**:

````java

@Configuration
public class RouterConfig {
    @Bean
    public RouterFunction<ServerResponse> routes(ProductHandler handler) {
        return RouterFunctions.route(RequestPredicates.GET("/api/v1/client-app"), handler::findAllProducts)
                .andRoute(RequestPredicates.GET("/api/v1/client-app/{id}"), handler::showProduct);
    }
}
````

Probamos estos dos endpoints, pero **antes es necesario tener levantado el proyecto spring-webflux-api-rest**:

Lista de productos:

````bash
curl -v http://localhost:8095/api/v1/client-app | jq

--- Respuesta
< HTTP/1.1 200 OK
< transfer-encoding: chunked
< Content-Type: application/json
[
  {
    "id": "64e011da9b83116d8415b40d",
    "name": "Sony Cámara HD",
    "price": 680.6,
    "createAt": "2023-08-18",
    "image": null,
    "category": {
      "id": "64e011da9b83116d8415b408",
      "name": "Electrónico"
    }
  },
  {
    "id": "64e011da9b83116d8415b419",
    "name": "Silla de oficina",
    "price": 540,
    "createAt": "2023-08-18",
    "image": null,
    "category": {
      "id": "64e011da9b83116d8415b40a",
      "name": "Muebles"
    }
  },
  {...},
  {
    "id": "64e011da9b83116d8415b415",
    "name": "Sillón 3 piezas",
    "price": 10,
    "createAt": "2023-08-18",
    "image": null,
    "category": {
      "id": "64e011da9b83116d8415b40a",
      "name": "Muebles"
    }
  }
]
````

Ver el producto con un id existente en la base de datos:

````bash
curl -v http://localhost:8095/api/v1/client-app/64e011da9b83116d8415b415 | jq

--- Respuesta
< HTTP/1.1 200 OK
< Content-Type: application/json
< Content-Length: 171
{
  "id": "64e011da9b83116d8415b415",
  "name": "Sillón 3 piezas",
  "price": 10,
  "createAt": "2023-08-18",
  "image": null,
  "category": {
    "id": "64e011da9b83116d8415b40a",
    "name": "Muebles"
  }
}
````

Ver un producto con id que no existe en la base de datos:

````bash
 curl -v http://localhost:8095/api/v1/client-app/555555555555 | jq

--- Respuesta
< HTTP/1.1 404 Not Found
< content-length: 0
````

## Implementando el CRUD en el Handler

Implementamos los métodos restantes del handler:

````java

@Component
public class ProductHandler {
    /* omitted code */
    public Mono<ServerResponse> createProduct(ServerRequest request) {
        String requestPathValue = request.requestPath().value();
        Mono<Product> productMono = request.bodyToMono(Product.class);
        return productMono
                .flatMap(this.productService::saveProduct)
                .flatMap(productDB -> ServerResponse
                        .created(URI.create(requestPathValue + "/" + productDB.id()))
                        .bodyValue(productDB)
                );
    }

    public Mono<ServerResponse> updateProduct(ServerRequest request) {
        String id = request.pathVariable("id");
        Mono<Product> productMono = request.bodyToMono(Product.class);
        return productMono
                .flatMap(product -> this.productService.updateProduct(id, product))
                .flatMap(updatedProductDB -> ServerResponse.ok().bodyValue(updatedProductDB))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> deleteProduct(ServerRequest request) {
        String id = request.pathVariable("id");
        return this.productService.deleteProduct(id)
                .flatMap(wasDeleted -> wasDeleted ? Mono.just(true) : Mono.empty())
                .flatMap(wasDeleted -> ServerResponse.noContent().build())
                .switchIfEmpty(ServerResponse.notFound().build());
    }
}
````

Luego, definimos sus rutas desde donde accederemos vía http:

````java

@Configuration
public class RouterConfig {
    @Bean
    public RouterFunction<ServerResponse> routes(ProductHandler handler) {
        return RouterFunctions.route(RequestPredicates.GET("/api/v1/client-app"), handler::findAllProducts)
                .andRoute(RequestPredicates.GET("/api/v1/client-app/{id}"), handler::showProduct)
                .andRoute(RequestPredicates.POST("/api/v1/client-app"), handler::createProduct)
                .andRoute(RequestPredicates.PUT("/api/v1/client-app/{id}"), handler::updateProduct)
                .andRoute(RequestPredicates.DELETE("/api/v1/client-app/{id}"), handler::deleteProduct);
    }
}
````

### Realizando pruebas a los endpoints implementados

**NOTA**

> Es importante tener el proyecto **spring-webflux-api-rest** levantado y obviamente el proyecto
> **spring-webflux-client-app**, que es donde implementamos la última parte del CRUD.

Creando un producto:

````bash
curl -v -X POST -H "Content-Type: application/json" -d "{\"name\": \"Cuadro de Picazzo\", \"price\": 550.80, \"category\": {\"id\": \"64e38ad2808dbc56d1959f1e\", \"name\": \"Decoración\"}}" http://localhost:8095/api/v1/client-app | jq

--- Respuesta
<
< HTTP/1.1 201 Created
< Location: /api/v1/client-app/64e3a16b808dbc56d1959f2e
< Content-Type: application/json
<
{
  "id": "64e3a16b808dbc56d1959f2e",
  "name": "Cuadro de Picazzo",
  "price": 550.8,
  "createAt": "2023-08-21",
  "image": null,
  "category": {
    "id": "64e38ad2808dbc56d1959f1e",
    "name": "Decoración"
  }
}
````

Actualizando un producto con **id existente:**

````bash
curl -v -X PUT -H "Content-Type: application/json" -d "{\"name\": \"Pintura Miguel Angel\", \"price\": 8900.80, \"category\": {\"id\": \"64e38ad2808dbc56d1959f1e\", \"name\": \"Decoración\"}}" http://localhost:8095/api/v1/client-app/64e3a16b808dbc56d1959f2e | jq

--- Respuesta
>
< HTTP/1.1 200 OK
< Content-Type: application/json
<
{
  "id": "64e3a16b808dbc56d1959f2e",
  "name": "Pintura Miguel Angel",
  "price": 8900.8,
  "createAt": "2023-08-21",
  "image": null,
  "category": {
    "id": "64e38ad2808dbc56d1959f1e",
    "name": "Decoración"
  }
}
````

Actualizando un producto con **id no existente:**

````bash
curl -v -X PUT -H "Content-Type: application/json" -d "{\"name\": \"Pintura Miguel Angel\", \"price\": 8900.80, \"category\": {\"id\": \"64e38ad2808dbc56d1959f1e\", \"name\": \"Decoración\"}}" http://localhost:8095/api/v1/client-app/64e3a16b808dbc56d1959f2e | jq

--- Respuesta
>
< HTTP/1.1 404 Not Found
````

Eliminando el producto con **id existente:**

````bash
curl -v -X DELETE http://localhost:8095/api/v1/client-app/64e3a16b808dbc56d1959f2e | jq
 
--- Respuesta
>
< HTTP/1.1 204 No Content
````

Eliminando el producto con **id no existente:**

````bash
curl -v -X DELETE http://localhost:8095/api/v1/client-app/64e3a16b808dbc56d1959f2e | jq

--- Respuesta
>
< HTTP/1.1 404 Not Found
````

## Implementando el método crear que tiene validación en el Api Rest

En nuestro API Rest tenemos dos métodos para crear productos, **uno cuenta con validación y el otro no.** Hasta ahora
solo hemos implementado el método que no cuenta con validación, en ese sentido, toca implementar el método que sí
cuenta con validación.

Creamos un nuevo método en nuestra interfaz **IProductService:**

````java
public interface IProductService {
    /* omitted code */
    Mono<Product> saveProductWithValidation(Product product);
    /* omitted code */
}
````

Implementamos el método en el **ProductServiceImpl:**

````java

@Service
public class ProductServiceImpl implements IProductService {
    /* omitted code */
    @Override
    public Mono<Product> saveProductWithValidation(Product product) {
        return this.client.post().uri("/create-product-with-validation") //<-- Uri del endpoint que crea un producto pero con validación
                .contentType(MediaType.APPLICATION_JSON)    // <-- tipo de contenido que enviamos en el Request
                .accept(MediaType.APPLICATION_JSON)// <-- tipo de contenido que aceptamos en el Response
                .bodyValue(product)
                .exchangeToMono(response -> response.bodyToMono(Product.class));
    }
    /* omitted code */
}
````

Creamos su respectivo **handlerFunction**:

````java

@Component
public class ProductHandler {
    /* omitted code */
    public Mono<ServerResponse> createProductWithValidation(ServerRequest request) {
        String requestPathValue = request.requestPath().value();
        Mono<Product> productMono = request.bodyToMono(Product.class);
        return productMono
                .flatMap(this.productService::saveProductWithValidation)
                .flatMap(productDB -> ServerResponse
                        .created(URI.create(requestPathValue + "/" + productDB.id()))
                        .bodyValue(productDB)
                );
    }
    /* omitted code */
}
````

Finalmente agregamos el endpoint correspondiente al handlerFunction **createProductWithValidation():**

````java

@Configuration
public class RouterConfig {
    @Bean
    public RouterFunction<ServerResponse> routes(ProductHandler handler) {
        return RouterFunctions.route(RequestPredicates.GET("/api/v1/client-app"), handler::findAllProducts)
                .andRoute(RequestPredicates.GET("/api/v1/client-app/{id}"), handler::showProduct)
                .andRoute(RequestPredicates.POST("/api/v1/client-app"), handler::createProduct)
                .andRoute(RequestPredicates.POST("/api/v1/client-app/create-product-with-validation"), handler::createProductWithValidation)
                .andRoute(RequestPredicates.PUT("/api/v1/client-app/{id}"), handler::updateProduct)
                .andRoute(RequestPredicates.DELETE("/api/v1/client-app/{id}"), handler::deleteProduct);
    }
}
````

Creando un producto con nuestro endpoint de validación:

````bash
curl -v -X POST -H "Content-Type: application/json" -d "{\"name\": \"Pintura Rupestre\", \"price\": 2500.50, \"category\": {\"id\": \"64e3a42ac3cc8e7a5cadc4b4\", \"name\": \"Decoración\"}}" http://localhost:8095/api/v1/client-app/create-product-with-validation | jq

--- Respuesta
>
< HTTP/1.1 201 Created
< Location: /api/v1/client-app/create-product-with-validation/64e3a950c3cc8e7a5cadc4c5
< Content-Type: application/json
<
{
  "id": "64e3a950c3cc8e7a5cadc4c5",
  "name": "Pintura Rupestre",
  "price": 2500.5,
  "createAt": "2023-08-21",
  "image": null,
  "category": {
    "id": "64e3a42ac3cc8e7a5cadc4b4",
    "name": "Decoración"
  }
}
````

Creando un producto sin datos con nuestro endpoint de validación:

````bash
curl -v -X POST -H "Content-Type: application/json" -d "{}" http://localhost:8095/api/v1/client-app/create-product-with-validation | jq

--- Respuesta
>
< HTTP/1.1 500 Internal Server Error
< Content-Type: application/json
<
{
  "timestamp": "2023-08-21T18:15:20.186+00:00",
  "path": "/api/v1/client-app/create-product-with-validation",
  "status": 500,
  "error": "Internal Server Error",
  "requestId": "c27ac726-3"
}
````

Como observamos el resultado anterior **el error 500 ocurre en este microservicio de cliente
(spring-webflux-client-app)**, mientras que nuestro **spring-webflux-api-rest** está retornando un error
**400 Bad Request** junto a los campos que han sido validados como incorrectos.

**NOTA**

> Aunque el error 400 Bad Request del proyecto **spring-webflux-api-rest** no se muestra en consola del IDE IntelliJ
> IDEA (habría que hacer una configuración en el application.properties si queremos verlo) **sabemos que sí está
> retornando eso**, ya que podemos hacer una petición al endpoint del mismo proyecto **spring-webflux-api-rest**
> http://localhost:8080/api/v2/products/create-product-with-validation y ver en el **cmd** el error **400 Bad Request**
> junto a los campos que han sido validados como incorrectos.

## Validando el handler createProductWithValidation de nuestro microservicio cliente

En el apartado anterior implementamos el handlerFunction **createProductWithValidation()** que llama al endpoint
`http://localhost:8080/api/v2/products/create-product-with-validation` ubicado en el proyecto
**spring-webflux-api-rest** y observamos que cuando mandamos un producto con datos incorrectos, el endpoint del api rest
que consumimos responde con un `400 BAD_REQUEST` trayendo consigo el mensaje de los campos que han sido validados como
incorrectos.

Ahora, en nuestro microservicio **spring-webflux-client-app** el handlerFunction **createProductWithValidation** no está
manejando el error proporcionado por el api rest de consumo, simplemente, si la solicitud falla, lanza un
`500 Internal Server Error`.

Para manejar la excepción retornada por el api rest agregaremos en el operador `.exchangeToMono()` la verificación del
status code retornado por el api rest, esto lo hacemos en la función **saveProductWithValidation()** del
**ProductServiceImpl**:

````java

@Service
public class ProductServiceImpl implements IProductService {
    /* omitted code */
    @Override
    public Mono<Product> saveProductWithValidation(Product product) {
        return this.client.post().uri("/create-product-with-validation")
                .contentType(MediaType.APPLICATION_JSON)    // <-- tipo de contenido que enviamos en el Request
                .accept(MediaType.APPLICATION_JSON)         // <-- tipo de contenido que aceptamos en el Response
                .bodyValue(product)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.CREATED)) { // (1)
                        return response.bodyToMono(Product.class);
                    }
                    return response.createError();                          // (2)
                });
    }
    /* omitted code */
}
````

- **(1)**, verificamos si el StatusCode retornado por el API Rest es **CREATED**, si es así, retornamos el producto
  devuelvo por el api rest.
- **(2)**, el `response.createError()` crea un Mono que falla con un `WebClientResponseException`, que contiene el
  **estado de la respuesta, los encabezados, el cuerpo y la solicitud de origen.**

Otra alternativa que habría producido lo mismo sería haber usado en vez del operador `exchangeToMono()`, el
operador `retrieve()` y su implementación habría sido más corta, ya que, luego de que ocurra la excepción 400 por debajo
el operador `bodyToMono()` después del `retrieve` habría lanzado la excepción en automático:

````
@Override
public Mono<Product> saveProductWithValidation(Product product) {
    return this.client.post().uri("/create-product-with-validation")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(product)
            .retrieve()
            .bodyToMono(Product.class);
}
````

Ahora, ubicados en el **ProductHandler**, en su handlerFunction **createProductWithValidation()** agregamos el operador
`onErrorResume()` para manejar la excepción lanzada desde el **ProductServiceImpl**:

````java

@Component
public class ProductHandler {
    /* omitted code */
    public Mono<ServerResponse> createProductWithValidation(ServerRequest request) {
        String requestPathValue = request.requestPath().value();
        Mono<Product> productMono = request.bodyToMono(Product.class);
        return productMono
                .flatMap(this.productService::saveProductWithValidation)
                .flatMap(productDB -> ServerResponse
                        .created(URI.create(requestPathValue + "/" + productDB.id()))
                        .bodyValue(productDB)
                )
                .onErrorResume(throwable -> {
                    WebClientResponseException responseException = (WebClientResponseException) throwable; // (1)
                    if (responseException.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                        return ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(responseException.getResponseBodyAsString(StandardCharsets.UTF_8));
                    }
                    return Mono.error(responseException);
                });
    }
    /* omitted code */
}
````

**(1)**, en el método `saveProductWithValidation()` del **ProductServiceImpl** estamos creando un Mono que falla con
un `WebClientResponseException`, esto gracias a esta línea `response.createError()`. Ahora, en este handlerFunction
debemos castear a dicha excepción `WebClientResponseException` de esa manera hacemos más específico el error y no tan
genérico.

Probando nuestro handlerFunction `createProductWithValidation` con datos correctos:

````bash

--- Response
>
< HTTP/1.1 201 Created
< Location: /api/v1/client-app/create-product-with-validation/64e3e7c0a9ab217fba592dc6
< Content-Type: application/json
<
{
  "id": "64e3e7c0a9ab217fba592dc6",
  "name": "Espejo",
  "price": 150.6,
  "createAt": "2023-08-21",
  "image": null,
  "category": {
    "id": "64e3ce22a9ab217fba592db7",
    "name": "Decoración"
  }
}
````

Verificando validación de nuestro handler `createProductWithValidation`:

````bash
curl -v -X POST -H "Content-Type: application/json" -d "{}" http://localhost:8095/api/v1/client-app/create-product-with-validation | jq

--- Respuesta
>
< HTTP/1.1 400 Bad Request
< Content-Type: application/json
<
{ [160 bytes data]
100   162  100   160  100     2   1249     15 --:--:-- --:--:-- --:--:--  1265
* Connection #0 to host localhost left intact
[
  "[Validación 2] El campo category must not be null",
  "[Validación 2] El campo price must not be null",
  "[Validación 2] El campo name must not be blank"
]
````

````bash
curl -v -X POST -H "Content-Type: application/json" -d "{\"name\": \"Espejo\", \"category\": {}}" http://localhost:8095/api/v1/client-app/create-product-with-validation | jq

--- Respuesta
>
< HTTP/1.1 400 Bad Request
< Content-Type: application/json
< Content-Length: 112
<
[
  "[Validación 2] El campo category.id must not be blank",
  "[Validación 2] El campo price must not be null"
]
````

