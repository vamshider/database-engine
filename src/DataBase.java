import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;


public class DataBase {
	
	static boolean exit = false;
	static Scanner scanner = new Scanner(System.in).useDelimiter(";");
	
    public static void main(String[] args) {
    	
    	init();
		splashScreen();
		String userCommand = ""; 

		while(!exit) {
			System.out.print(Constants.PROMPT);
			userCommand = scanner.next().replace("\n", " ").replace("\r", "").trim().toLowerCase();
			parseUserCommand(userCommand);
		}
		System.out.println("Exit Successful");
	}
	
    public static void splashScreen() {
    	
		System.out.println(line("*",80));
        System.out.println("Welcome to the DataBase");
		System.out.println("DataBase Version " + Constants.VERSION);
		System.out.println("\nUse \"HELP;\" to show the supported commands");
		System.out.println(line("*",80));
	}
	
	public static String line(String str,int number) {
		
		String lineBreaker = "";
		for(int i = 0; i < number; i++)
			lineBreaker += str;
		
		return lineBreaker;
	}
	
	public static void help() {
		System.out.println(line("*",80));
		System.out.println("---------- SUPPORTED COMMANDS ----------");
		System.out.println();
		System.out.println("1. SHOW TABLES;");
		System.out.println("\t- Displays all the tables in the database");
		System.out.println("2. CREATE TABLE table_name (<column_name datatype> <NOT NULL/UNIQUE>);");
		System.out.println("\t- Creates a new table in the database");
		System.out.println("\t- First member/column of a table should be the primary key of type INT");
		System.out.println("3. CREATE INDEX ON table_name (<column_name>);");
		System.out.println("\t- Creates a new index for the table in the database");
		System.out.println("4. INSERT INTO table_name VALUES (value1,value2,..);");
		System.out.println("\t- Inserts a new record into the table");
		System.out.println("\t- First Column is an auto-incremented primary key");
		System.out.println("5. DELETE FROM TABLE table_name WHERE row_id = keyValue;");
		System.out.println("\t- Deletes a record from the table whose rowid is <keyValue>");
		System.out.println("6. UPDATE table_name SET column_name = value WHERE condition;");
		System.out.println("\t-  Modifies the records in the table");
		System.out.println("7. SELECT * FROM table_name;");
		System.out.println("\t- Displays all the records in a table");
		System.out.println("8. SELECT * FROM table_name WHERE column_name operator value;");
		System.out.println("\t- Displays the records in the table as per the given condition");
		System.out.println("9. DROP TABLE table_name;");
		System.out.println("\t- Remove the table data and its schema");
		System.out.println("10. VERSION;");
		System.out.println("\t- Displays the current version of the DataBase");
		System.out.println("11. HELP;");
		System.out.println("\t- Displays the help information");
		System.out.println("12. EXIT;");
		System.out.println("\t- Exits the program");
		System.out.println();
		System.out.println("NOTE: All the commands are CASE INSENSITIVE");
		System.out.println();
		System.out.println(line("*",80));
	}
	
	public static boolean tableExists(String tableName){
		
		tableName = tableName + ".tbl";
		
		try {
			
			File dataDir = new File(Constants.dirUserdata);
			if (tableName.equalsIgnoreCase(Constants.TABLE_CATALOG+Constants.FILE_TYPE) || tableName.equalsIgnoreCase(Constants.COLUMN_CATALOG+Constants.FILE_TYPE))
				dataDir = new File(Constants.dirCatalog) ;
			
			String[] oldTables = dataDir.list();
			for (int i = 0; i < oldTables.length; i++) {
				if(oldTables[i].equals(tableName))
					return true;
			}
		}
		catch (Exception e) {
			System.out.println("Unable to Create the Catalog and User Data Directories");
			System.out.println(e);
		}

		return false;
	}

	public static void init(){
		try {
			File dataDir = new File("data");
			if(dataDir.mkdir()){
				System.out.println("Initializing the DataBase...");
				initialize();
			}
			else {
				dataDir = new File(Constants.dirCatalog);
				String[] oldTables = dataDir.list();
				boolean tableExists = false;
				boolean colExists = false;
				for (int i = 0; i < oldTables.length; i++) {
					
					if(oldTables[i].equals(Constants.TABLE_CATALOG + Constants.FILE_TYPE))
						tableExists = true;
					
					if(oldTables[i].equals(Constants.COLUMN_CATALOG + Constants.FILE_TYPE))
						colExists = true;
				}
				
				if(!tableExists){
					System.out.println("dataBaseTables table does not exist, initializing...");
					System.out.println();
					initialize();
				}
				
				if(!colExists){
					System.out.println("dataBaseColumns table does not exist, initializing...");
					System.out.println();
					initialize();
				}
				
			}
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public static void initialize() {
		
		try {
			File dataDir = new File(Constants.dirUserdata);
			dataDir.mkdir();
			dataDir = new File(Constants.dirCatalog);
			dataDir.mkdir();
			String[] oldTables;
			oldTables = dataDir.list();
			
			
			for (int i = 0; i < oldTables.length; i++) {
				File oldFile = new File(dataDir, oldTables[i]); 
				oldFile.delete();
			}
		}
		catch (Exception e) {
			System.out.println(e);
		}

		try {
			RandomAccessFile catalogTable = new RandomAccessFile(Constants.dirCatalog + "/davisbase_tables.tbl", "rw");
			catalogTable.setLength(Constants.PAGE_SIZE);
			catalogTable.seek(0);
			catalogTable.write(0x0D);
			catalogTable.writeByte(0x02);
									
			// Creating the DAVISBASE_TABLES table
			// This stores the information about the existing tables.
			catalogTable.writeShort(Constants.COLUMN_OFFSET);
			catalogTable.writeInt(0);
			catalogTable.writeInt(0);
			catalogTable.writeShort(Constants.TABLE_OFFSET);
			catalogTable.writeShort(Constants.COLUMN_OFFSET);
			
			catalogTable.seek(Constants.TABLE_OFFSET);
			catalogTable.writeShort(20);
			catalogTable.writeInt(1); 
			catalogTable.writeByte(1);
			catalogTable.writeByte(28);
			catalogTable.writeBytes(Constants.TABLE_CATALOG);
			
			catalogTable.seek(Constants.COLUMN_OFFSET);
			catalogTable.writeShort(21);
			catalogTable.writeInt(2); 
			catalogTable.writeByte(1);
			catalogTable.writeByte(29);
			catalogTable.writeBytes(Constants.COLUMN_CATALOG);
			
			catalogTable.close();
		}
		catch (Exception e) {
			System.out.println(e);
		}
		
		try {
			RandomAccessFile catalogColumn = new RandomAccessFile(Constants.dirCatalog + "/davisbase_columns.tbl", "rw");
			catalogColumn.setLength(Constants.PAGE_SIZE);
			catalogColumn.seek(0);       
			catalogColumn.writeByte(0x0D); 
			catalogColumn.writeByte(0x09); // Number of records

			
			int[] offset=new int[9];
			offset[0]=Constants.PAGE_SIZE-45;
			offset[1]=offset[0]-49;
			offset[2]=offset[1]-46;
			offset[3]=offset[2]-50;
			offset[4]=offset[3]-51;
			offset[5]=offset[4]-49;
			offset[6]=offset[5]-59;
			offset[7]=offset[6]-51;
			offset[8]=offset[7]-49;
			
			catalogColumn.writeShort(offset[8]); 
			catalogColumn.writeInt(0); 
			catalogColumn.writeInt(0); 
			
			for(int i=0;i<offset.length;i++)
				catalogColumn.writeShort(offset[i]);

			
			// Creating the DAVISBASE_COLUMNS table
			// This stores the information about the columns in the tables
			catalogColumn.seek(offset[0]);
			catalogColumn.writeShort(36);
			catalogColumn.writeInt(1);		// Key
			catalogColumn.writeByte(6);		// Number of Columns
			catalogColumn.writeByte(28); 	// (16 + 12)
			catalogColumn.writeByte(17);	// (5 + 12)
			catalogColumn.writeByte(15);	// (3 + 12)
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.TABLE_CATALOG); 
			catalogColumn.writeBytes(Constants.HEADER_ROWID); 
			catalogColumn.writeBytes("INT"); 
			catalogColumn.writeByte(1); 
			catalogColumn.writeBytes(Constants.FALSE); 
			catalogColumn.writeBytes(Constants.FALSE); 
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.seek(offset[1]);
			catalogColumn.writeShort(42); 
			catalogColumn.writeInt(2); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(28);
			catalogColumn.writeByte(22);
			catalogColumn.writeByte(16);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.TABLE_CATALOG); 
			catalogColumn.writeBytes(Constants.HEADER_TABLE_NAME); 
			catalogColumn.writeBytes(Constants.HEADER_TEXT); 
			catalogColumn.writeByte(2);
			catalogColumn.writeBytes(Constants.FALSE); 
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.seek(offset[2]);
			catalogColumn.writeShort(37); 
			catalogColumn.writeInt(3); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(29);
			catalogColumn.writeByte(17);
			catalogColumn.writeByte(15);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.COLUMN_CATALOG);
			catalogColumn.writeBytes(Constants.HEADER_ROWID);
			catalogColumn.writeBytes("INT");
			catalogColumn.writeByte(1);
			catalogColumn.writeBytes(Constants.FALSE);
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.seek(offset[3]);
			catalogColumn.writeShort(43);
			catalogColumn.writeInt(4); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(29);
			catalogColumn.writeByte(22);
			catalogColumn.writeByte(16);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.COLUMN_CATALOG);
			catalogColumn.writeBytes(Constants.HEADER_TABLE_NAME);
			catalogColumn.writeBytes(Constants.HEADER_TEXT);
			catalogColumn.writeByte(2);
			catalogColumn.writeBytes(Constants.FALSE);
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.seek(offset[4]);
			catalogColumn.writeShort(44);
			catalogColumn.writeInt(5); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(29);
			catalogColumn.writeByte(23);
			catalogColumn.writeByte(16);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.COLUMN_CATALOG);
			catalogColumn.writeBytes("column_name");
			catalogColumn.writeBytes(Constants.HEADER_TEXT);
			catalogColumn.writeByte(3);
			catalogColumn.writeBytes(Constants.FALSE);
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.seek(offset[5]);
			catalogColumn.writeShort(42);
			catalogColumn.writeInt(6); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(29);
			catalogColumn.writeByte(21);
			catalogColumn.writeByte(16);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.COLUMN_CATALOG);
			catalogColumn.writeBytes("data_type");
			catalogColumn.writeBytes(Constants.HEADER_TEXT);
			catalogColumn.writeByte(4);
			catalogColumn.writeBytes(Constants.FALSE);
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.seek(offset[6]);
			catalogColumn.writeShort(52); 
			catalogColumn.writeInt(7); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(29);
			catalogColumn.writeByte(28);
			catalogColumn.writeByte(19);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.COLUMN_CATALOG);
			catalogColumn.writeBytes("ordinal_position");
			catalogColumn.writeBytes("TINYINT");
			catalogColumn.writeByte(5);
			catalogColumn.writeBytes(Constants.FALSE);
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.seek(offset[7]);
			catalogColumn.writeShort(44); 
			catalogColumn.writeInt(8); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(29);
			catalogColumn.writeByte(23);
			catalogColumn.writeByte(16);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.COLUMN_CATALOG);
			catalogColumn.writeBytes(Constants.HEADER_IS_NULLABLE);
			catalogColumn.writeBytes(Constants.HEADER_TEXT);
			catalogColumn.writeByte(6);
			catalogColumn.writeBytes(Constants.FALSE);
			catalogColumn.writeBytes(Constants.FALSE);
		

			catalogColumn.seek(offset[8]);
			catalogColumn.writeShort(42); 
			catalogColumn.writeInt(9); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(29);
			catalogColumn.writeByte(21);
			catalogColumn.writeByte(16);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.COLUMN_CATALOG);
			catalogColumn.writeBytes(Constants.HEADER_IS_UNIQUE);
			catalogColumn.writeBytes(Constants.HEADER_TEXT);
			catalogColumn.writeByte(7);
			catalogColumn.writeBytes(Constants.FALSE);
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.close();
			
			String[] cur_row_id_value = {"10", Constants.TABLE_CATALOG,"cur_row_id","INT","3",Constants.FALSE,Constants.FALSE};		
			Table.insertInto(Constants.COLUMN_CATALOG,cur_row_id_value,Constants.dirCatalog);	// Adds current Row ID column to the davisbase_columns
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}

	public static String[] parseCondition(String condition){
		
		String parsedCondition[] = new String[3];
		String temp[] = new String[2];
		if(condition.contains(Constants.EQUALS_SIGN)) {
			temp = condition.split(Constants.EQUALS_SIGN);
			parsedCondition[0] = temp[0].trim();
			parsedCondition[1] = Constants.EQUALS_SIGN;
			parsedCondition[2] = temp[1].trim();
		}
		
		if(condition.contains(Constants.LESS_THAN_SIGN)) {
			temp = condition.split(Constants.LESS_THAN_SIGN);
			parsedCondition[0] = temp[0].trim();
			parsedCondition[1] = Constants.LESS_THAN_SIGN;
			parsedCondition[2] = temp[1].trim();
		}
		
		if(condition.contains(Constants.GREATER_THAN_SIGN)) {
			temp = condition.split(Constants.GREATER_THAN_SIGN);
			parsedCondition[0] = temp[0].trim();
			parsedCondition[1] = Constants.GREATER_THAN_SIGN;
			parsedCondition[2] = temp[1].trim();
		}
		
		if(condition.contains(Constants.LESS_THAN_EQUAL_SIGN)) {
			temp = condition.split(Constants.LESS_THAN_EQUAL_SIGN);
			parsedCondition[0] = temp[0].trim();
			parsedCondition[1] = Constants.LESS_THAN_EQUAL_SIGN;
			parsedCondition[2] = temp[1].trim();
		}

		if(condition.contains(Constants.GREATER_THAN_EQUAL_SIGN)) {
			temp = condition.split(Constants.GREATER_THAN_EQUAL_SIGN);
			parsedCondition[0] = temp[0].trim();
			parsedCondition[1] = Constants.GREATER_THAN_EQUAL_SIGN;
			parsedCondition[2] = temp[1].trim();
		}
		
		if(condition.contains(Constants.NOT_EQUAL_SIGN)) {
			temp = condition.split(Constants.NOT_EQUAL_SIGN);
			parsedCondition[0] = temp[0].trim();
			parsedCondition[1] = Constants.NOT_EQUAL_SIGN;
			parsedCondition[2] = temp[1].trim();
		}

		return parsedCondition;
	}
		
	public static void parseUserCommand (String userCommand) {
		
		ArrayList<String> commandTokens = new ArrayList<String>(Arrays.asList(userCommand.split(" ")));

		switch (commandTokens.get(0)) {

		    case "show":
		    	showTables();
			    break;
			
		    case "create":
		    	switch (commandTokens.get(1)) {
		    	case "table": 
		    		parseCreateString(userCommand);
		    		break;
		    		
		    	case "index":
		    		parseIndexString(userCommand);
		    		break;
		    		
		    	default:
					System.out.println("Invalid Command: \"" + userCommand + "\"");
					System.out.println();
					break;
		    	}
		    	break;

			case "insert":
				parseInsertString(userCommand);
				break;
				
			case "delete":
				parseDeleteString(userCommand);
				break;	

			case "update":
				parseUpdateString(userCommand);
				break;
				
			case "select":
				parseQueryString(userCommand);
				break;

			case "drop":
				dropTable(userCommand);
				break;	

			case "help":
				help();
				break;

			case "version":
				System.out.println("DataBase Version " + Constants.VERSION);
				break;

			case "exit":
				exit=true;
				break;
				
			case "quit":
				exit=true;
				break;
	
			default:
				System.out.println("Invalid Command: \"" + userCommand + "\"");
				System.out.println();
				break;
		}
	} 

	// Displays the list of existing tables
	public static void showTables() {
		
		String table = Constants.TABLE_CATALOG;
		String[] cols = {Constants.HEADER_TABLE_NAME};
		String[] condition = new String[0];
		Table.select(table, cols, condition, true);
	}
	
	// Function to parse the CREATE Query
    public static void parseCreateString(String createString) {
		
    	String tableName = createString.split(" ")[2];
		String cols = createString.split(tableName)[1].trim();
		String[] create_cols = cols.substring(1, cols.length()-1).split(",");
		
		for(int i = 0; i < create_cols.length; i++)
			create_cols[i] = create_cols[i].trim();
		
		if(tableExists(tableName))
			System.out.println("Table " + tableName + " already exists.");
		else
			Table.createTable(tableName, create_cols);				
	}
    
    // Function to parse the INSERT Query 
    public static void parseInsertString(String insertString) {
    	
    	try{
    		
    		String table = insertString.split(" ")[2];
    		String rawColumns=insertString.split("values")[1].trim();
    		String[] insertValuesInit = rawColumns.substring(1, rawColumns.length()-1).split(",");
    		String[] insertValues = new String[insertValuesInit.length + 1];
    		
    		for(int i = 1; i <= insertValuesInit.length; i++)
    			insertValues[i] = insertValuesInit[i-1].trim();
	
    		if(tableExists(table))
    			Table.insertInto(table, insertValues, Constants.dirUserdata+"/");
    		else
    			System.out.println("Table " + table + " does not exist.");
    	}
    	catch(Exception e)
    	{
    		System.out.println(e+e.toString());
    	}
	}
    
    // Function to parse the DELETE Query
    public static void parseDeleteString(String deleteString) {
		
		String table = deleteString.split(" ")[2];
		String[] rawConditionArray = deleteString.split("where");
		String rawCondition = rawConditionArray.length > 1 ? rawConditionArray[1] : "";
		String[] parsedCondition = rawConditionArray.length>1?parseCondition(rawCondition) : new String[0];
		
		if(tableExists(table))
			Table.delete(table, parsedCondition, Constants.dirUserdata);
		else
			System.out.println("Table " + table + " does not exist.");
	}
    
    // Function to parse the UPDATE Query
    public static void parseUpdateString(String updateString) {
		
		String table = updateString.split(" ")[1];
		String whereCondition = updateString.split("set")[1].split("where")[1];
		String setCondition = updateString.split("set")[1].split("where")[0];
		String[] parsedCondition = parseCondition(whereCondition);
		String[] parsedSetCondition = parseCondition(setCondition);
		
		if(!tableExists(table))
			System.out.println("Table " + table + " does not exist.");
		else
			Table.update(table, parsedCondition, parsedSetCondition, Constants.dirUserdata);
	}
    
    // Function to parse a Query
    public static void parseQueryString(String queryString) {
		
		String[] parsedCondition;
		String[] columns;
		String[] columnsCondition = queryString.split("where");
		if(columnsCondition.length > 1){
			parsedCondition = parseCondition(columnsCondition[1].trim());
		}
		else{
			parsedCondition = new String[0];
		}
		String[] select = columnsCondition[0].split("from");
		String tableName = select[1].trim();
		String cols = select[0].replace("select", "").trim();
		
		if(cols.contains("*")){
			columns = new String[1];
			columns[0] = "*";
		}
		else{
			columns = cols.split(",");
			for(int i = 0; i < columns.length; i++)
				columns[i] = columns[i].trim();
		}
		
		if(!tableExists(tableName))
			System.out.println("Table " + tableName + " does not exist.");
		else
		    Table.select(tableName, columns, parsedCondition, true);
	}
	
    // Function to parse the DROP Query
	public static void dropTable(String dropTableString) {
		
		String[] tokens = dropTableString.split(" ");
		String tableName = tokens[2];
		
		if(tableExists(tableName))
			Table.drop(tableName);
		else
			System.out.println("Table " + tableName + " does not exist.");	
	}
	
	public static void parseIndexString(String createString) {
		
		String[] tokens = createString.split(" ");
		String tableName = tokens[3];
		String[] temp = createString.split(tableName);
		String columns = temp[1].trim();
		String[] createColumns = columns.substring(1, columns.length()-1).split(",");
		
		for(int i = 0; i < createColumns.length; i++)
			createColumns[i] = createColumns[i].trim();
		
		Table.createIndex(tableName, createColumns);		
	}
}