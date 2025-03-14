package app;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
public class ProductoApplication extends Application {

    @Override
    public Set<Object> getSingletons() {
        // Configura el ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true); // Formatea el JSON
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Ignora propiedades desconocidas

        // Crea un proveedor JSON con la configuración de Jackson
        JacksonJsonProvider provider = new JacksonJsonProvider();
        provider.setMapper(mapper);

        // Registra el proveedor y los recursos (endpoints) en la aplicación
        Set<Object> singletons = new HashSet<>();
        singletons.add(provider);
        return singletons;
    }

    @Override
    public Set<Class<?>> getClasses() {
        // Registra las clases de recursos (endpoints)
        Set<Class<?>> classes = new HashSet<>();
        classes.add(service.ProductoService.class); // Registra la clase ProductoService
        return classes;
    }
}