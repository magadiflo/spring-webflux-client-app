# Sección: WebClient: consumiendo servicios RestFul

---

En esta sección crearemos este microservicio que consumirá el servicio rest desarrollado en el proyecto
[**spring-webflux-api-rest**](https://github.com/magadiflo/spring-webflux-api-rest.git)

## Creando nuestro microservicio cliente

Cambiamos el puerto de este microservicio, ya que por defecto es el 8080 quien ya se encuentra ocupado por el
microservicio **spring-webflux-api-rest**:

````properties
server.port=8090
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
public record CategoryDTO(String id, String name) {
}
````

````java
public record ProductDTO(String id, String name, Double price, LocalDate createAt, String image,
                         CategoryDTO categoryDTO) {
}
````

## Creando el componente Service implementado con WebClient

Crearemos primero la interfaz que tendrá todos los métodos a implementar:

````java
public interface IProductService {
    Flux<ProductDTO> findAllProducts();

    Mono<ProductDTO> findProduct(String id);

    Mono<ProductDTO> saveProduct(ProductDTO productDTO);

    Mono<ProductDTO> updateProduct(String id, ProductDTO productDTO);

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
    public Flux<ProductDTO> findAllProducts() {
        return this.client.get()
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToFlux(response -> response.bodyToFlux(ProductDTO.class));
    }

    @Override
    public Mono<ProductDTO> findProduct(String id) {
        return this.client.get().uri("/{id}", Collections.singletonMap("id", id))
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                        return response.bodyToMono(ProductDTO.class);
                    }
                    return response.createError();
                });
    }

    @Override
    public Mono<ProductDTO> saveProduct(ProductDTO productDTO) {
        return null;
    }

    @Override
    public Mono<ProductDTO> updateProduct(String id, ProductDTO productDTO) {
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
    public Mono<ProductDTO> findProduct(String id) {
        return this.client.get().uri("/{id}", Collections.singletonMap("id", id))
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                        return response.bodyToMono(ProductDTO.class);
                    }
                    return response.createError();
                });
    }
    /* omitted code */
}
````

- En el método utilizamos el **cliente http WebClient** que inyectamos por constructor para hacer la petición `get()`
  hacia el endpoint que configuramos en el @Bean de la clase **ApplicationConfig**.
- Como vamos a buscar un producto, necesitamos completar el url agregándole la uri `/{id}` para buscar el producto por
  el identificador que le pasemos.
- El operador `accept()` define el tipo de **MediaType** que esperamos recibir como Response del endpoint a consultar.
- El operador `exchangeToMono` (alternativa al operador `retrieve()`) brinda más control a través del acceso a
  **ClientResponse**. Esto puede ser útil para escenarios avanzados, por ejemplo, para decodificar la respuesta de
  manera diferente según el estado de la respuesta, un claro ejemplo es lo que hacemos en nuestro método
  **findProduct()**:

   ````
  .exchangeToMono(response -> {
      if (response.statusCode().equals(HttpStatus.OK)) {
          return response.bodyToMono(ProductDTO.class);
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
    public Mono<ProductDTO> saveProduct(ProductDTO productDTO) {
        return this.client.post()
                .contentType(MediaType.APPLICATION_JSON) // <-- tipo de contenido que enviamos en el Request
                .accept(MediaType.APPLICATION_JSON)      // <-- tipo de contenido que aceptamos en el Response
                .bodyValue(productDTO)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.CREATED)) {
                        return response.bodyToMono(ProductDTO.class);
                    }
                    return response.createError();
                });
    }

    @Override
    public Mono<ProductDTO> updateProduct(String id, ProductDTO productDTO) {
        return this.client.put().uri("/{id}", Collections.singletonMap("id", id))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(productDTO)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                        return response.bodyToMono(ProductDTO.class);
                    }
                    return response.createError();
                });
    }

    @Override
    public Mono<Void> deleteProduct(String id) {
        return this.client.delete().uri("/{id}", Collections.singletonMap("id", id))
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.NO_CONTENT)) {
                        return response.bodyToMono(Void.class);
                    }
                    return response.createError();
                });
    }
}
````
