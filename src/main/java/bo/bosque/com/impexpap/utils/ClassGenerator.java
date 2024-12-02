package bo.bosque.com.impexpap.utils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;



public class ClassGenerator {

    //private static final String DATABASE_URL = "jdbc:sqlserver://192.168.3.116:1433;databaseName=BOSQUE-2_0;?useSSL=false";
    private static final String DATABASE_URL = "jdbc:sqlserver://192.168.3.116:1433;databaseName=BOSQUE-2_0;?useSSL=false";
    private static final String DATABASE_USER = "sa";
    private static final String DATABASE_PASSWORD = "sapbus1n3ss";
    private static final String OUTPUT_DIRECTORY = "src/main/java/bo/bosque/com/impexpap/model/";

    public static void main(String[] args) {
       /*if (args.length != 2) {
            System.out.println("Usage: ClassGenerator <className> <tableName>");
            return;
        }*/

        String className ="PrestamoEstado";
        String tableName = "tpre_PrestamoEstado";

        ClassGenerator classGenerator = new ClassGenerator();
        classGenerator.generateClass(className, tableName);
    }

    public void generateClass(String className, String tableName) {
        List<Column> attributes = getTableColumns(tableName);

        StringBuilder classBuilder = new StringBuilder();
        classBuilder.append("package bo.bosque.com.impexpap.model;\n\n");
        classBuilder.append("import java.io.Serializable;\n\n");
        classBuilder.append("import lombok.*;\n\n");
        classBuilder.append("@Getter\n");
        classBuilder.append("@Setter\n");
        classBuilder.append("@ToString\n");
        classBuilder.append("@NoArgsConstructor\n");
        classBuilder.append("@AllArgsConstructor\n");
        classBuilder.append("public class ").append(className).append(" implements Serializable {\n");

        // Agregar atributos
        for (Column column : attributes) {
            String javaType = convertSqlTypeToJavaType(column.getType());
            String javaFieldName = convertToCamelCase(column.getName()); // Necesitas implementar este método
            classBuilder.append("    private ").append(javaType).append(" ").append(javaFieldName).append(";\n");
        }

        classBuilder.append("}");

        writeToFile(className + ".java", classBuilder.toString());
    }

    private String convertToCamelCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;
        for (char c : input.toCharArray()) {
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    result.append(result.length() == 0 ? Character.toLowerCase(c) : c);
                }
            }
        }
        return result.toString();
    }

    private String convertSqlTypeToJavaType(String sqlType) {
        switch (sqlType.toLowerCase()) {
            case "bigint":
            case "int":
                return "int";
            case "varchar":
            case "char":
            case "text":
                return "String";
            case "float":
            case "numeric":
                return "float";
            case "date":
            case "datetime":
            case "timestamp":
                return "Date";
            default:
                return "String"; // Tipo por defecto
        }
    }

    private List<Column> getTableColumns(String tableName) {

        List<Column> columns = new ArrayList<>();

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            try (Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD)) {
                DatabaseMetaData metaData = connection.getMetaData();
                try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                    while (rs.next()) {
                        String columnName = rs.getString("COLUMN_NAME");
                        String columnType = rs.getString("TYPE_NAME");
                        columns.add(new Column(columnName, columnType));
                    }
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        System.out.println("las columnas son: \n");
        System.out.println(columns);

        return columns;
    }

    private void writeToFile(String fileName, String content) {
        File outputDir = new File(OUTPUT_DIRECTORY);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File outputFile = new File(OUTPUT_DIRECTORY + fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
