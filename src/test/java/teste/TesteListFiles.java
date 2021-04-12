package teste;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TesteListFiles {
    private static final Logger RUNTIMELOGGER = LoggerFactory.getLogger("download");
    private static final String LOG_DIR = "log";
    private static List<String> FILE_DOWLOAD_LOGS = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        new TesteListFiles().recuperarArquivosBaixados();
    }


    private void recuperarArquivosBaixados() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(LOG_DIR), "teste*")) {
                for (Path path : stream) {
                    RUNTIMELOGGER.info("filename: {}", path.toAbsolutePath());
                    try (BufferedReader br = Files.newBufferedReader(path)) {
                        String downloadLog = br.readLine();
                        while (downloadLog != null) {
                            FILE_DOWLOAD_LOGS.add(downloadLog);
                            RUNTIMELOGGER.info("downloadLog: {}", downloadLog);
                            downloadLog = br.readLine();
                        }
                    }
                }
            }
        }
}
