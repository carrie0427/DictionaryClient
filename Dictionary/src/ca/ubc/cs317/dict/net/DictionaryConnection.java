package ca.ubc.cs317.dict.net;

import ca.ubc.cs317.dict.exception.DictConnectionException;
import ca.ubc.cs317.dict.model.Database;
import ca.ubc.cs317.dict.model.Definition;
import ca.ubc.cs317.dict.model.MatchingStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

/**
 * Created by Jonatan on 2017-09-09.
 */
public class DictionaryConnection {

    private static final int DEFAULT_PORT = 2628;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private Map<String, Database> databaseMap = new LinkedHashMap<String, Database>();

    /**
     * Establishes a new connection with a DICT server using an explicit host and port number, and handles initial
     * welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @param port Port number used by the DICT server
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     *                                 don't match their expected value.
     */
    public DictionaryConnection(String host, int port) throws DictConnectionException {

        try {
            socket = new Socket(host, port);
            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            /*
            To check if the connection is made(status code 220), if not, close all opened socket, PrintWriter, and BufferedReader.
            DictConnectionException is thrown
             */

            if (!input.readLine().substring(0, 3).equals("220")) {
                input.close();
                output.close();
                socket.close();
                throw new DictConnectionException();
            }

        } catch (IOException e) {
            System.out.println("Couldn't get I/O for the connection to " + host);
        }

    }

    /**
     * Establishes a new connection with a DICT server using an explicit host, with the default DICT port number, and
     * handles initial welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     *                                 don't match their expected value.
     */
    public DictionaryConnection(String host) throws DictConnectionException {
        this(host, DEFAULT_PORT);
    }

    /**
     * Sends the final QUIT message and closes the connection with the server. This function ignores any exception that
     * may happen while sending the message, receiving its reply, or closing the connection.
     */
    public synchronized void close() {

        output.println("QUIT"); // To write "QUIT" to the connected server

        try {
            // Extract the first 3 characters in the string to check if server received the "QUIT" command(status code 221)
            if (input.readLine().substring(0, 3).equals("221")) {
                input.close();
                output.close();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * Requests and retrieves all definitions for a specific word.
     *
     * @param word     The word whose definition is to be retrieved.
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 definitions in the first database that has a definition for the word should be used
     *                 (database '!').
     * @return A collection of Definition objects containing all definitions returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Collection<Definition> getDefinitions(String word, Database database) throws DictConnectionException {
        Collection<Definition> set = new ArrayList<>();
        getDatabaseList(); // Ensure the list of databases has been populated


        output.println("DEFINE" + " " + database.getName() + " \"" + word + "\""); // Enter the query into the PrinterWriter to be sent to the server
        try {
            String[] messages = input.readLine().split(" "); // split the message gotten from the BufferedReader into arrays

            switch (messages[0]) {
                case "552":
                    return set;

                case "150": // if the status code == "150", n definition is found
                    String temp;
                    int i = 0;

                    while ((temp = input.readLine()) != null && i < Integer.parseInt(messages[1])) { //printing out the definition
                        if (temp.equals("."))
                            continue;
                        String[] info = temp.split(" ");

                        switch (info[0]) {
                            case "151":
                                Definition def = new Definition(word, database);
                                String description;
                                while (!(description = input.readLine()).equals(".")) {
                                    def.appendDefinition(description);
                                }
                                set.add(def); //add the definitions into the set
                                i++;
                                break;

                            case "250": //stop printing when status code == "250". indicating the end of the definition
                                return set;

                            default:
                                throw new DictConnectionException(); // if status code is not "151" or "250", that means an error has occured, throwing an exception
                        }
                    }
                    break;

                default:
                    throw new DictConnectionException();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return set;
    }

    /**
     * Requests and retrieves a list of matches for a specific word pattern.
     *
     * @param word     The word whose definition is to be retrieved.
     * @param strategy The strategy to be used to retrieve the list of matches (e.g., prefix, exact).
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 matches in the first database that has a match for the word should be used (database '!').
     * @return A set of word matches returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<String> getMatchList(String word, MatchingStrategy strategy, Database database) throws DictConnectionException {
        Set<String> set = new LinkedHashSet<>();

        // Enter the query into the PrinterWriter to be sent to the server
        output.println("MATCH " + database.getName() + " " + strategy.getName() + " \"" + word + "\"");
        try {
            String[] messages = input.readLine().split(" ");

            if (messages[0].equals("552")) {
                return set;
            }

            if (!messages[0].equals("152")) { // check if there is a match(status code "152", throw exception if not found.
                throw new DictConnectionException(messages[0]);
            } else {
                for (int i = 0; i < Integer.parseInt(messages[1]); i++) {
                    String[] temp = input.readLine().split("\"");
                    set.add(temp[1]); // add the definitions into the set
                }

                input.readLine();
                if (!input.readLine().substring(0, 3).equals("250")) {
                    throw new DictConnectionException();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return set;
    }

    /**
     * Requests and retrieves a list of all valid databases used in the server. In addition to returning the list, this
     * method also updates the local databaseMap field, which contains a mapping from database name to Database object,
     * to be used by other methods (e.g., getDefinitionMap) to return a Database object based on the name.
     *
     * @return A collection of Database objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Collection<Database> getDatabaseList() throws DictConnectionException {

        if (!databaseMap.isEmpty()) return databaseMap.values();


        output.println("SHOW DB");  // Enter the query into the PrinterWriter to be sent to the server
        try {
            String[] messages = input.readLine().split(" ");

            if (messages[0].equals("554")) {
                return databaseMap.values();
            }

            if (!messages[0].equals("110")) { //check if databases is found(status code "110), if not, throw exception
                throw new DictConnectionException();
            } else {
                for (int i = 0; i < Integer.parseInt(messages[1]); i++) {
                    String temp = input.readLine();
                    String key = temp.substring(0, temp.indexOf(" "));
                    databaseMap.put(key, new Database(key, temp.substring(temp.indexOf(" "), temp.length()))); //insert the databases into the databaseMap
                }

                input.readLine();
                if (!input.readLine().substring(0, 3).equals("250")) { //check if it is the end of the array(status code "250")
                    throw new DictConnectionException();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return databaseMap.values();
    }

    /**
     * Requests and retrieves a list of all valid matching strategies supported by the server.
     *
     * @return A set of MatchingStrategy objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<MatchingStrategy> getStrategyList() throws DictConnectionException {
        Set<MatchingStrategy> set = new LinkedHashSet<>();

        // Enter the query into the PrinterWriter to be sent to the server
        output.println("SHOW STRATEGIES");
        try {
            String[] messages = input.readLine().split(" ");
            if (messages[0].equals("555")) {
                return set;
            }

            if (!messages[0].equals("111")) { // check if there are any strategies available(status code "111")
                throw new DictConnectionException();
            } else {
                for (int i = 0; i < Integer.parseInt(messages[1]); i++) {
                    String temp = input.readLine();
                    String name = temp.substring(0, temp.indexOf(" "));
                    String description = temp.substring(temp.indexOf(" "), temp.length());
                    set.add(new MatchingStrategy(name, description));   // add all the strategies into the se
                }

                input.readLine();
                if (!input.readLine().substring(0, 3).equals("250")) {  // check if it reaches the end of the array (status code "250")
                    throw new DictConnectionException();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return set;
    }

}
