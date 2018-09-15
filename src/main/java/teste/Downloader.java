package teste;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import teste.exception.DownloaderException;
import teste.to.BingImage;
import teste.to.BingImages;

public class Downloader {
	static {
		System.setProperty("file.encoding", "UTF-8");
		System.setProperty("java.util.logging.config.file", "log-config.properties");
	}
	private static final Logger RUNTIMELOGGER = LoggerFactory.getLogger(Downloader.class);
	private static final String DIR_NAME = "./";
	private static final String LOG_DIR = DIR_NAME + "log/";
	private static final String LOG_PATH = LOG_DIR + "_download.log";
	private static final int NUMBER = 8;
	private static final int INDEX = 0;
	private static final String[] MARKETS = { "en-US", "EN-GB", "pt-BR", "EN-CA", "DE-DE", "FR-FR", "JA-JP", "ZH-CN" };
	private static final SimpleDateFormat FORMATO_TEMPO = new SimpleDateFormat("yyyyMMdd");
	private static final SimpleDateFormat FORMATO_NOME = new SimpleDateFormat("yyyy-MM-dd");



	public static void main(String[] args) throws Exception {
		Downloader teste = new Downloader();
		teste.backupLog();
		for (int i = 0; i < MARKETS.length; i++) {
			teste.executar(INDEX, NUMBER, MARKETS[i]);
		}
	}


	private void backupLog() throws IOException {
		File dirLog = new File(LOG_DIR);
		if (!dirLog.exists()) {
			dirLog.mkdirs();
		}
		File logFile = new File(LOG_PATH);
		if (!logFile.exists()) {
			logFile.createNewFile();
		}
		FileUtils.copyFile(logFile, new File(LOG_PATH.replace(".log", "-") + FORMATO_NOME.format(new Date()) + ".log"));
	}


	private void executar(int index, int number, String market) {
		BufferedWriter downloadlog = null;
		try {
			downloadlog = new BufferedWriter(new FileWriter(LOG_PATH, true));
			List<BingImage> images = callREST(index, number, market).getImages();
			RUNTIMELOGGER.info("resposta: " + market + " " + images.toString());

			for (BingImage bingImage : images) {
				String urlBase = bingImage.getUrlBase();
				String idImagem = urlBase.substring(urlBase.length() - 10);
				String idImagemNew = urlBase.substring(urlBase.lastIndexOf('/') + 1, urlBase.lastIndexOf('_')).trim();
				if (downloaded(idImagem, idImagemNew)) {
					continue;
				}
				CloseableHttpResponse response = HttpClients.createDefault().execute(new HttpGet("http://www.bing.com/" + urlBase + "_1920x1080.jpg"));
				try {
					StatusLine statusLine = response.getStatusLine();
					if (statusLine.getStatusCode() != 200) {
						throw new DownloaderException("Erro ao buscar binário: " + statusLine);
					}
					String contentType = response.getEntity().getContentType().getValue();
					if (!contentType.startsWith("image")) {
						throw new DownloaderException("Erro ao buscar binário: " + contentType);
					}

					Date startDate = FORMATO_TEMPO.parse(bingImage.getStartdate());
					String descricao;
					String copyright = bingImage.getCopyright();
					descricao = copyright.substring(0, copyright.lastIndexOf('(')).trim();
					if (!isLatin(descricao)) {
						descricao = idImagemNew;
					}

					String nomeImagem = FORMATO_NOME.format(startDate) + "_" + descricao.replaceAll("[\\\\/:*?\"<>|]", "") + ".jpg";
					File file = new File(DIR_NAME + nomeImagem);
					InputStream in = response.getEntity().getContent();
					OutputStream out = new FileOutputStream(file);
					IOUtils.copy(in, out);
					in.close();
					out.close();

					BasicFileAttributeView attributes = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class);
					FileTime time = FileTime.fromMillis(startDate.getTime());
					attributes.setTimes(time, time, time);

					RUNTIMELOGGER.info("baixado: " + idImagemNew + " " + idImagem + " " + nomeImagem);
					StringBuilder idImagemLog = new StringBuilder(idImagemNew);
					for (int i = 1; i < 30 - idImagemNew.length(); i++) {
						idImagemLog.append(" ");
					}

					downloadlog.write(idImagemLog.toString() + " " + idImagem + " " + market + " " + nomeImagem);
					downloadlog.write('\n');

				} catch (ParseException e) {
					RUNTIMELOGGER.error(e.getMessage(), e);
				} finally {
					response.close();
				}
			}


			RUNTIMELOGGER.info("fim " + market);
		} catch (DownloaderException | IOException e) {
			RUNTIMELOGGER.error(e.getMessage(), e);
		}

		try {
			if (downloadlog != null) {
				downloadlog.close();
			}
		} catch (IOException e) {
			RUNTIMELOGGER.error(e.getMessage(), e);
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


	private boolean downloaded(String idImagem, String idImagemNew) {
		try {
			FileReader fr = new FileReader(LOG_PATH);
			BufferedReader br = new BufferedReader(fr);
			String idLog = br.readLine();
			while (idLog != null) {
				if (
				// idLog.contains(idImagem) ||
				idLog.toLowerCase().startsWith(idImagemNew.toLowerCase())) {
					return true;
				}
				idLog = br.readLine();
			}
			fr.close();
			br.close();
		} catch (IOException e) {
			RUNTIMELOGGER.error(e.getMessage(), e);
		}
		return false;
	}


	private BingImages callREST(int index, int number, String market) throws DownloaderException {
		RestTemplate restTemplate = new RestTemplate();
		SimpleClientHttpRequestFactory requestFactory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
		requestFactory.setConnectTimeout(30000);
		requestFactory.setReadTimeout(180000);
		// Conversor necessario para interpretar as respostas JSON: MappingJackson2HttpMessageConverter.
		// restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter())

		// Invoca o servico.
		BingImages response;
		try {
			StringBuilder urlBuilder = new StringBuilder("http://www.bing.com/HPImageArchive.aspx?format=xml");
			urlBuilder.append("&idx=");
			urlBuilder.append(index);
			urlBuilder.append("&n=");
			urlBuilder.append(number);
			if (market != null) {
				urlBuilder.append("&mkt=");
				urlBuilder.append(market);
			}
			response = restTemplate.getForObject(urlBuilder.toString(), BingImages.class);
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
