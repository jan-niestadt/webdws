import java.io.InputStream;
import java.io.FileInputStream;

public class test_schema_loading {
    public static void main(String[] args) {
        try {
            String schemaPath = "src/main/resources/schema/library.xsd";
            InputStream inputStream = new FileInputStream(schemaPath);
            
            System.out.println("Schema file found and readable!");
            System.out.println("File size: " + inputStream.available() + " bytes");
            
            // Read first few lines to verify content
            byte[] buffer = new byte[200];
            int bytesRead = inputStream.read(buffer);
            String content = new String(buffer, 0, bytesRead);
            System.out.println("First 200 characters:");
            System.out.println(content);
            
            inputStream.close();
        } catch (Exception e) {
            System.err.println("Error loading schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
