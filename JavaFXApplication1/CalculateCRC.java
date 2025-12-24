import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.CRC32;

public class CalculateCRC {
    public static void main(String[] args) throws IOException {
        String[] files = {
                System.getProperty("user.home") + "/workspace/gunadeep/testfile.txt.chunk1",
                System.getProperty("user.home") + "/workspace/gunadeep/testfile.txt.chunk2",
                System.getProperty("user.home") + "/workspace/gunadeep/testfile.txt.chunk3"
        };

        for (String file : files) {
            byte[] data = Files.readAllBytes(Paths.get(file));
            CRC32 crc = new CRC32();
            crc.update(data);
            long crcValue = crc.getValue();
            String crcHex = Long.toHexString(crcValue).toUpperCase();
            while (crcHex.length() < 8) {
                crcHex = "0" + crcHex;
            }
            System.out.println(file + ": " + crcHex);
        }
    }
}
