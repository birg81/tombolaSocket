package tombolaSocket;

import java.io.BufferedReader; // Per leggere Stream (1/2)
import java.io.InputStreamReader; // Per leggere Stream (2/2)
import java.io.PrintWriter; // Per scrivere Stream
import java.net.ServerSocket; // Socket che si mette in ascolto
import java.net.Socket; // Server che accetta connessione
import java.util.Date; // Solo quando il programma funge da WebServer

public class TombolaServer implements Runnable {
	private static ServerSocket ss;
	private static int[] numTombola;

	private static void shuffle(int[] deck) {
		for (int i = 0; i < deck.length; i++) {
			final int index = (int) (Math.random() * deck.length);
			int t = deck[i];
			deck[i] = deck[index];
			deck[index] = t;
		}
	}
	private static String createCartella() {
		shuffle(numTombola);
		StringBuilder tabCartella = new StringBuilder(
				"SERIE %d\r\n<table id='CartellaTombola'>"
						.formatted((int) (1 + Math.random() * 999)));
		for (int i = 0; i < 15; i++)
			tabCartella.append(
					"%s<td>%d</td>%s".formatted(i % 5 == 0 ? "<tr>\r\n" : "",
							numTombola[i], i % 5 == 4 ? "</tr>\r\n" : ""));
		return tabCartella.append("</table>\r\n").toString();
	}
	public TombolaServer(int PORT) throws Exception {
		ss = new ServerSocket(PORT); // server in ascolto di connessioni
		System.out.printf("TCP Server Listening on Port: %d..\n", PORT);
		numTombola = new int[90];
		for (int i = 0; i < numTombola.length; i++)
			numTombola[i] = i + 1;
		shuffle(numTombola);
	}
	@Override
	public void run() {
		boolean GoAway = true;
		Socket s = null;
		BufferedReader r = null; // oggetto che legge lo stream
		PrintWriter w = null; // oggetto che scrive sullo stream
		String strData = null, // messaggio che leggo o che scrivo sullo stream
				htmlPkg, headPkg;
		for (; GoAway;)
			try {
				s = ss.accept(); // accetto la connessione

				// Lettura dello stream: eseguita da due oggetti
				r = new BufferedReader(
						new InputStreamReader(s.getInputStream()));
				// la scrittura ad uno solo NB: importante il parametro TRUE!!
				w = new PrintWriter(s.getOutputStream(), true);

				strData = r.readLine();
				System.out.printf("[recive] %s << %s..\n",
						s.getRemoteSocketAddress(), strData);

				String now = new Date().toString();
				htmlPkg = """
						<head>
						<link rel='icon' href='https://image.winudf.com/v2/image1/Y29tLmVzdHJhemlvbmUuYmluZ28uY2hyaXN0bWFzLm5hdGFsZS50b21ib2xhLmJpbmdvX2ljb25fMTU0MTgxNDg4MV8wODY/icon.png?w=24&fakeurl=1' type='image/png'/>
						<title>Tombola</title>
						<style>
						BODY {
							font: 12pt 'Arial';
							color: #fff;
							text-shadow: 2px 2px 4px #000;
							background-image: linear-gradient(#00ffcc, #0099ff);
						}
						H1{
							font-size: 250%
						}
						TD {
							height: 130px;
							width: 130px;
							color: #000;
							font-weigth: bold;
							font-size: 60pt;
							text-align: center;
							background-color: #fff;
						}
						div#my_time{
							position: absolute;
							top: calc(100% - 24pt);
						}
						</style>
						</head>
						<body>
						%s
						<div id='my_time'>%s</div>
						</body>
						</html>
						"""
						.formatted(createCartella(), now);
				headPkg = """
						HTTP/1.1 200 OK
						Date: %s
						Server: Apache/2.4.35 (Win64)
						Last-Modified: %s
						Content-Length: %s
						Content-Type: text/html
						Connection: Closed
						""".formatted(now, now, htmlPkg.length());

				// Inverto la stringa ricevuta e la trasformo in maiuscolo
				strData = "send %s bytes"
						.formatted((htmlPkg + headPkg).length());

				// scrivo sullo stream e ripulisco lo stream con il flush
				w.print("%s%s".formatted(headPkg, htmlPkg));

				System.out.printf("[sendTo] %s >> %s..\n",
						s.getRemoteSocketAddress(), strData);

				// chiudo sia r, w, s li riapro alla prossima connessione
				r.close();
				w.close();
				s.close();

				// se ricevo end (DNE è il contrario maiuscolo di end)
				if ("DNE".equalsIgnoreCase(strData)) {
					System.out.println("TCP Server was Remote Closed!..");
					ss.close();
					GoAway = false; // interrompo il ciclo infinito
					break; // forzo ad uscire dal ciclo
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	public static void main(String[] args) throws Exception {
		new Thread(new TombolaServer(
				args.length > 0 ? Integer.parseInt(args[0]) : 80)).start();
	}
}