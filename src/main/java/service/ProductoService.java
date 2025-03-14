package service;

import db.DatabaseConnection;
import model.Producto;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/productos")
public class ProductoService {

    @Context
    private UriInfo uriInfo; // Inyecta el objeto UriInfo

    // Listar todos los productos
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProductos() {
        List<Producto> productos = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Productos")) {

            while (rs.next()) {
                Producto producto = mapResultSetToProducto(rs);
                productos.add(producto);
            }
        } catch (SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error al obtener productos: " + e.getMessage())
                           .build();
        }
        return Response.ok(productos).build();
    }

    // Listar un producto por ID
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProductoById(@PathParam("id") int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Productos WHERE ID = ?")) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Producto producto = mapResultSetToProducto(rs);
                return Response.ok(producto).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("Producto no encontrado")
                               .build();
            }
        } catch (SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error al obtener el producto: " + e.getMessage())
                           .build();
        }
    }

    // Filtrar productos por query string
    @GET
    @Path("/buscar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProductosPorQueryString() {
        // Obtener el query string completo
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        
        // Verificar si se proporcionaron parámetros
        if (queryParams.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("Debe proporcionar al menos un parámetro de búsqueda")
                           .build();
        }

        // Construir la consulta SQL dinámicamente
        StringBuilder sql = new StringBuilder("SELECT * FROM Productos WHERE ");
        List<Object> values = new ArrayList<>();
        int paramCount = 0;

        // Lista de columnas válidas en la tabla Productos
        String[] columnasValidas = {"ID", "Nombre", "Descripcion", "Precio", "Cantidad", "Categoria", "Marca"};

        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().get(0); // Tomamos el primer valor del parámetro

            // Validar que el parámetro sea una columna válida
            if (!esColumnaValida(key, columnasValidas)) {
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity("Parámetro no válido: " + key)
                               .build();
            }

            if (paramCount > 0) {
                sql.append(" AND ");
            }
            sql.append(key).append(" = ?");
            values.add(value);
            paramCount++;
        }

        // Ejecutar la consulta SQL
        List<Producto> productos = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            // Asignar valores a los parámetros de la consulta
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Producto producto = mapResultSetToProducto(rs);
                productos.add(producto);
            }
        } catch (SQLException e) {
            // Log del error (puedes usar un logger como SLF4J)
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error al obtener productos: " + e.getMessage())
                           .build();
        }

        return Response.ok(productos).build();
    }

    // Crear un nuevo producto
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addProducto(Producto producto) {
        if (producto == null || producto.getNombre() == null || producto.getPrecio() <= 0 || producto.getCantidad() < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("Datos del producto inválidos")
                           .build();
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO Productos (Nombre, Descripcion, Precio, Cantidad, Categoria, Marca) VALUES (?, ?, ?, ?, ?, ?)")) {
            pstmt.setString(1, producto.getNombre());
            pstmt.setString(2, producto.getDescripcion());
            pstmt.setDouble(3, producto.getPrecio());
            pstmt.setInt(4, producto.getCantidad());
            pstmt.setString(5, producto.getCategoria());
            pstmt.setString(6, producto.getMarca());
            pstmt.executeUpdate();
            return Response.status(Response.Status.CREATED).build();
        } catch (SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error al agregar producto: " + e.getMessage())
                           .build();
        }
    }

    // Modificar un producto existente
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateProducto(@PathParam("id") int id, Producto producto) {
        if (producto == null || producto.getNombre() == null || producto.getPrecio() <= 0 || producto.getCantidad() < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("Datos del producto inválidos")
                           .build();
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE Productos SET Nombre = ?, Descripcion = ?, Precio = ?, Cantidad = ?, Categoria = ?, Marca = ? WHERE ID = ?")) {
            pstmt.setString(1, producto.getNombre());
            pstmt.setString(2, producto.getDescripcion());
            pstmt.setDouble(3, producto.getPrecio());
            pstmt.setInt(4, producto.getCantidad());
            pstmt.setString(5, producto.getCategoria());
            pstmt.setString(6, producto.getMarca());
            pstmt.setInt(7, id);
            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("Producto no encontrado")
                               .build();
            }
        } catch (SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error al actualizar el producto: " + e.getMessage())
                           .build();
        }
    }

    // Eliminar un producto
    @DELETE
    @Path("/{id}")
    public Response deleteProducto(@PathParam("id") int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM Productos WHERE ID = ?")) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();

            if (rowsDeleted > 0) {
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("Producto no encontrado")
                               .build();
            }
        } catch (SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error al eliminar el producto: " + e.getMessage())
                           .build();
        }
    }

    // Método utilitario para mapear un ResultSet a un objeto Producto
    private Producto mapResultSetToProducto(ResultSet rs) throws SQLException {
        Producto producto = new Producto();
        producto.setId(rs.getInt("ID"));
        producto.setNombre(rs.getString("Nombre"));
        producto.setDescripcion(rs.getString("Descripcion"));
        producto.setPrecio(rs.getDouble("Precio"));
        producto.setCantidad(rs.getInt("Cantidad"));
        producto.setCategoria(rs.getString("Categoria"));
        producto.setMarca(rs.getString("Marca"));
        return producto;
    }

    // Método para validar si un parámetro es una columna válida
    private boolean esColumnaValida(String parametro, String[] columnasValidas) {
        for (String columna : columnasValidas) {
            if (columna.equalsIgnoreCase(parametro)) {
                return true;
            }
        }
        return false;
    }
}