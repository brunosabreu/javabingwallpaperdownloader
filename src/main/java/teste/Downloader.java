package teste;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import teste.exception.DownloaderException;
import teste.to.BingImage;
import teste.to.BingImages;

/**
 * Classe principal.
 */
public class Downloader {
    static {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("java.util.logging.config.file", "log-config.properties");
    }
    private static final String DOWNLOAD_LOG_NAME = "download";
    private static final Logger RUNTIME_LOGGER = LoggerFactory.getLogger("runtime");
    private static final Logger DOWNLOAD_LOGGER = LoggerFactory.getLogger(DOWNLOAD_LOG_NAME);
    private static final String DIR_NAME = "./";
    private static final String LOG_DIR = DIR_NAME + "log/";
    private static final int NUMBER = 8;
    private static final String[] MARKETS = { "en-US", "EN-GB", "pt-BR", "EN-CA", "DE-DE", "FR-FR", "JA-JP", "ZH-CN" };
    private static final String FORMATO_TEMPO = "yyyyMMdd";
    private static final String FORMATO_NOME = "yyyy-MM-dd";
    private List<String> downloadLogs = new ArrayList<>();

    /**
     * Excecução linha de comando.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Downloader downloader = new Downloader();
        downloader.recuperarArquivosBaixados();
        for (int m = 0; m < MARKETS.length; m++) {
            String market = MARKETS[m];
            downloader.execute(NUMBER - 1, NUMBER, market);
            int idx = 0;
            if (m > 2) {
                idx++;
            }
            downloader.execute(idx, NUMBER, market);
            RUNTIME_LOGGER.info("fim {}", market);
        }
    }


    private void recuperarArquivosBaixados() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(LOG_DIR),
                "*" + DOWNLOAD_LOG_NAME + "*")) {
            for (Path path : stream) {
                try (BufferedReader br = Files.newBufferedReader(path)) {
                    String downloadLog = br.readLine();
                    while (downloadLog != null) {
                        downloadLogs.add(downloadLog.toLowerCase());
                        downloadLog = br.readLine();
                    }
                }
            }
        }
    }


    private void execute(int index, int number, String market) {
        List<BingImage> images = null;
        try {
            images = getMetadata(index, number, market).getImages();
            for (BingImage bingImage : images) {
                downloadFile(market, bingImage);
            }
        } catch (Exception e) {
            RUNTIME_LOGGER.error("Erro: {}, resposta: {}", e.getMessage(), images, e);
        }
    }


    private void downloadFile(String market, BingImage bingImage) throws DownloaderException, IOException {
        String urlBase = bingImage.getUrlbase();
        String idImagem = urlBase.substring(urlBase.lastIndexOf('=') + 5, urlBase.lastIndexOf('_')).trim();
        if (isDownloaded(idImagem)) {
            return;
        }
        try (CloseableHttpResponse response = HttpClients.createDefault()
                .execute(new HttpGet("http://www.bing.com/" + urlBase + "_1920x1080.jpg"))) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != 200) {
                throw new DownloaderException("Erro ao buscar binário: " + statusLine);
            }
            String contentType = response.getEntity().getContentType().getValue();
            if (!contentType.startsWith("image")) {
                throw new DownloaderException("Erro ao buscar binário: " + contentType);
            }

            Date startDate = new SimpleDateFormat(FORMATO_TEMPO).parse(bingImage.getStartdate());
            String descricao;
            String copyright = bingImage.getCopyright();
            if (!isLatin(copyright)) {
                descricao = idImagem;
            } else {
                descricao = copyright.substring(0, copyright.lastIndexOf('(')).trim();
            }

            String nomeImagem = new SimpleDateFormat(FORMATO_NOME).format(startDate) + " "
                    + descricao.replaceAll("[\\\\/:*?\"<>|]", "") + ".jpg";
            File file = new File(DIR_NAME + nomeImagem);
            InputStream in = response.getEntity().getContent();
            OutputStream out = new FileOutputStream(file);
            IOUtils.copy(in, out);
            in.close();
            out.close();

            BasicFileAttributeView attributes = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class);
            FileTime time = FileTime.fromMillis(startDate.getTime());
            attributes.setTimes(time, time, time);

            RUNTIME_LOGGER.info("baixado: {} {} {}", market, idImagem, nomeImagem);
            StringBuilder idImagemLog = new StringBuilder(idImagem);
            for (int i = 1; i < 30 - idImagem.length(); i++) {
                idImagemLog.append(" ");
            }
            String log = idImagemLog.toString() + " " + market + " " + nomeImagem;
            DOWNLOAD_LOGGER.info(log);
            downloadLogs.add(log.toLowerCase());
        } catch (ParseException e) {
            RUNTIME_LOGGER.error(e.getMessage(), e);
        }
    }


    private boolean isLatin(String string) {
        int countNotLatin = 0;
        for (char c : string.toCharArray()) {
            if ((int) c > 256) {
                countNotLatin++;
            }
        }
        return (float) countNotLatin / string.length() < 0.2;
    }


    private boolean isDownloaded(String newImageId) {
        String id = newImageId.toLowerCase();
        for (String downloadLog : downloadLogs) {
            if (downloadLog.contains(id)) {
                return true;
            }
        }
        return false;
    }


    private BingImages getMetadata(int index, int number, String market) throws DownloaderException {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = (SimpleClientHttpRequestFactory) restTemplate
                .getRequestFactory();
        requestFactory.setConnectTimeout(30000);
        requestFactory.setReadTimeout(180000);
        // Conversor necessario para interpretar as respostas JSON: MappingJackson2HttpMessageConverter.
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

        // Invoca o servico.
        BingImages response;
        try {
            StringBuilder urlBuilder = new StringBuilder("http://www.bing.com/HPImageArchive.aspx?format=js");
            urlBuilder.append("&idx=");
            urlBuilder.append(index);
            urlBuilder.append("&n=");
            urlBuilder.append(number);
            if (market != null) {
                urlBuilder.append("&mkt=");
                urlBuilder.append(market);
            }
            String url = urlBuilder.toString();
            response = restTemplate.getForObject(url, BingImages.class);
        } catch (Exception e) {
            throw new DownloaderException("Erro ao chamar o get.", e);
        }

        if (response != null) {
            return response;
        } else {
            throw new DownloaderException("O serviço de get não retornou uma resposta.");
        }
    }

}
