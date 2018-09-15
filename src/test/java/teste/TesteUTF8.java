/**
 * 
 */
package teste;

public class TesteUTF8 {
	public static void main(String[] args) {
		String jp = "｢大連中山広場｣中国, 遼寧省";

		for (char c : jp.toCharArray()) {
			System.out.println((int) c);
		}

	}

}
