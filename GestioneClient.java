package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GestioneClient extends Thread {

    public Socket s;
    public BufferedReader in;
    public BufferedWriter out;
    public String username;
    public static List<GestioneClient> gc = new ArrayList<>();
    private boolean inChatPrivata = false; // Indica se il client è in chat privata
    private GestioneClient destinatarioPrivato = null; // Il destinatario della chat privata

    public GestioneClient(Socket s) {
        try {
            this.s = s;
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            username = in.readLine();
            synchronized (gc) {
                gc.add(this); // Aggiunge il client alla lista condivisa
            }
           
            // menu
            while (true) {
                String scelta = in.readLine();
                System.out.println(username+" ha scelto:"+scelta); 
                

                switch (scelta) {
                    //GESTIONE CHAT PRIVATA
                    case "1":
                    out.write("PRIV");
                    out.newLine();
                    out.flush();
                
                    String dst = in.readLine(); // Leggi l'username del destinatario della chat privata
                
                    // Trova il client destinatario
                    GestioneClient destinatario = null;
                    synchronized (gc) {
                        for (GestioneClient c : gc) {
                            if (c.username.equals(dst)) {
                                destinatario = c;
                                break;
                            }
                        }
                    }
                
                    if (destinatario != null) {
                        out.write("ok");
                        out.newLine();
                        out.flush();
                
                        // Imposta lo stato per la chat privata
                        this.inChatPrivata = true;
                        this.destinatarioPrivato = destinatario;
                
                        // Notifica l'inizio della chat privata
                        destinatario.invioMessaggio("SERVER: " + username + " ha avviato una chat privata con te.");
                        
                        //Gestisce i messaggi + QUIT
                        boolean connessionePriv = true;
                        while (connessionePriv) {
                            String messaggio = in.readLine();
                            if (messaggio.equals("/QUIT")) {
                                connessionePriv = false; 
                                this.inChatPrivata = false;
                                this.destinatarioPrivato = null;
                            
                                if (destinatario != null) {
                                    destinatario.inChatPrivata = false;
                                    destinatario.destinatarioPrivato = null;
                                    destinatario.invioMessaggio("SERVER: " + username + " ha lasciato la chat privata.");
                                }
                                this.invioMessaggio("/QUIT"); // Comunica al client che la chat è finita
                            } else {
                                destinatario.invioMessaggio(username + " (Privato): " + messaggio);
                            }
                        }
                    } else {
                        out.write("Utente non trovato!");
                        out.newLine();
                        out.flush();
                    }
                        break;

                    case "2":
                    // menu pubblico
                    messageBrodcast("SERVER: " + username + " si e' unito alla chat");
                    out.write("PUBBL");
                    out.newLine();
                    out.flush();
                    boolean connessione = true;
                    //Gestione Messaggio 
                    while (connessione) {
                            String messaggio = in.readLine();
                            if (messaggio.equals("/QUIT")) {
                                connessione = false;
                                messageBrodcast("SERVER: " + username + " ha abbandonato la chat.");
                                this.invioMessaggio("Digita '1' per andare in chat PRIVATA, '2' per quella PUBBLICA e '3' per DISCONNETTERTI"); // Notifica il client
                                break; // Torna al menu principale
                            } else {
                                System.out.println(messaggio);
                                this.messageBrodcast(username+": "+messaggio);
                            }
                        }
                        break;
                        //Disconnessioe client 
                    case "3":
                        System.out.println(username + " si è disconesso");
                        break;
                }
            }

        } catch (IOException e) {

            e.printStackTrace();

        }
    }
    public synchronized void invioMessaggio(String mess) {
        try {
        
            out.write(mess);
            out.newLine();
            out.flush();

        } catch (IOException e) {

            e.printStackTrace();

        }
    }

    public void messageBrodcast(String messaggio) {
        synchronized (gc) {
            for (GestioneClient client : gc) {
                if (!client.inChatPrivata && client != this) { // Invia solo ai client non in chat privata
                    client.invioMessaggio(messaggio);
                }
            }
        }
    }
}
