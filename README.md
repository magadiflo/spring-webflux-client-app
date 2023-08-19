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

    Mono<Void> deleteProduct(String id);
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
    public Mono<Void> deleteProduct(String id) {
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
  al que consultamos nos retornará un **404 Not Fount**, no debemos preocuparnos, ya que nuestro **handlerFunction**
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

Ahora toca implementar los demás métodos de nuestro **ProductService**, estas implementaciones serán similares a lo que
expliqué para los métodos **findAllProducts()** y **findProduct()**:

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
    public Mono<Void> deleteProduct(String id) {
        return this.client.delete().uri("/{id}", Collections.singletonMap("id", id))
                .exchangeToMono(response -> response.bodyToMono(Void.class));
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
