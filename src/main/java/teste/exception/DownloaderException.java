package teste.exception;

/**
 * Exceção para os stjsecweb.
 */
public class DownloaderException extends Exception {
	private static final long serialVersionUID = -2462850582704730295L;


	/**
	 * Construtor default.
	 */
	public DownloaderException() {
		super();
	}


	/**
	 * Construtor default.
	 * 
	 * @param mensagem
	 *            mensagem
	 * @param causa
	 *            causa
	 */
	public DownloaderException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}


	/**
	 * Construtor default.
	 * 
	 * @param mensagem
	 *            mensagem
	 */
	public DownloaderException(String mensagem) {
		super(mensagem);
	}


	/**
	 * Construtor default.
	 * 
	 * @param causa
	 *            causa
	 */
	public DownloaderException(Throwable causa) {
		super(causa);
	}
}
