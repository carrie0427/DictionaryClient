Adle Ker Hue Chiam k4y0b 22669162
Jiali Fan t4f2b 56214307


For DictionaryConnection(String host, int port), we first try to create a new socket,
a new PrinterWriter, and a new BufferedReader. If any of those informational are not 
found, an IO exception will be thrown. After creating them, we try to check if a
connection is made by reading the first 3 characters in the PrinterWriter, if not,
we close the socket, PrinterWriter, and BufferedReader created earlier, and then
throw a new DictConnectionException.

For close(), we first write in to the server the keyword "QUIT" to indicate that we
want to close the connection. Then check if the server received the correct message
by checking the status code in the PrinterWrite. if it is correct, we then go ahead and
close the socket, PrinterWriter, and BufferedReader. If not, an IO Exception is thrown.

For Collection<Definition> getDefinitions(String word, Database,database),
we first write to the server with the keyword "DEFINE" followed by the database name
and the word we want to get the definition of. We then check the returned message from the 
server indicating the status of the query. If we get the status code "150", it means we found
n definitions of the word we are looking for. We then go ahead and print the definitions out.
We stop printing when we get the status code "151" indicating that we have reached the end 
of all the definitions. We throw a DictConnectionException if the status code is not "150" or "151"
indicating that 1. No definitions are found. 2. Incorrect query syntax.

For Set<String> getMatchList(String word, MatchingStrategy strategy, Database database),
we first write to the server the keyword "MATCH" followed by the database name, strategy name
and the word. We then check if there are any matching words found by checking f the status code
equates to "152". We throw a DictConnectionException if it is not. If it is, we add the definitions 
to the set which will then be returns to be printed. 

For Collection<Database> getDatabaseList(), we first check if there are databases found by checking
the status code. If it equates to "110", we go ahead and insert all the databases found in the 
databaseMap. An exception is thrown if it is not found. We then return of the values in the databaseMap.

For Collection<Database> getStrategyList(), we first check if there are strategies found by checking
the status code. If it equates to "111", we go ahead and insert all the strategies found in the set. 
An exception is thrown if it is not found. We then return of the values in the set.