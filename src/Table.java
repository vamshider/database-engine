import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Table{
	
	public static int numRecords;
	
	public static int getPageCount(RandomAccessFile file){
		
		int numPages = 0;
		try{
			numPages = (int)(file.length()/((long)(Constants.pageSize)));
		}
		catch(Exception e){
			System.out.println(e);
		}

		return numPages;
	}

	public static void drop(String table){
		
		try{
			delete(Constants.TABLE_CATALOG, new String[] {"table_name", "=", table}, Constants.dirCatalog);
			delete(Constants.COLUMN_CATALOG, new String[] {"table_name", "=", table}, Constants.dirCatalog);

			File oldFile = new File(Constants.dirUserdata, table+Constants.FILE_TYPE); 
			oldFile.delete();
			
		}catch(Exception e){
			System.out.println(e);
		}

	}
    
	public static void delete(String table, String[] cmp, String dir){
		try{
			ArrayList<Integer> rowIds = new ArrayList<Integer>();
			
			if(cmp.length==0 || !"rowid".equals(cmp[0])) {
				// Getting the rowids that are to be updated
				Records records = select(table, new String[] {"*"}, cmp, false);
				rowIds.addAll(records.content.keySet());
			}
			else
				// Adding the Row ID to the list
				rowIds.add(Integer.parseInt(cmp[2]));
			
			for(int rowId : rowIds) {
				// Opening the file for the table
				RandomAccessFile file = new RandomAccessFile(dir + table + Constants.FILE_TYPE, "rw");
				int numPages = getPageCount(file);
				int page = 0;
				
				// Finding the page where data is located
				for(int currentPage = 1; currentPage <= numPages; currentPage++)
					if(BPlusTree.hasKey(file, currentPage, rowId) && BPlusTree.getPageType(file, currentPage)==Constants.recordsPage){
						page = currentPage;
						break;
					}
				
				if(page==0){
					System.out.println("Data NOT found in the table.");
					return;
				}
				
				// Getting all the cells on that page
				short[] cells = BPlusTree.getCellArray(file, page);
				int k = 0;
				
				// Iterating over all the cells
				for(int cellNum = 0; cellNum < cells.length; cellNum++)
				{
					// Getting location for current cell
					long currLoc = BPlusTree.getCellLoc(file, page, cellNum);
					
					// Retrieving all the values
					String[] values = retrieveValues(file, currLoc);
					
					// Getting the current Row ID
					int currRowId = Integer.parseInt(values[0]);
					
					if(currRowId != rowId){
						BPlusTree.setCellOffset(file, page, k, cells[cellNum]);
						k++;
					}
				}
				
				// Changing cell number
				BPlusTree.setCellNumber(file, page, (byte)k);
			}
		}catch(Exception e)
		{
			System.out.println(e);
		}
	}
	
	// Method used to retrieve values from a certain location in the file
	public static String[] retrieveValues(RandomAccessFile file, long loc){
		
		String[] values = null;
		try{
			
			SimpleDateFormat dateFormat = new SimpleDateFormat (Constants.datePattern);

			file.seek(loc+2);
			int rowId = file.readInt();
			int numCols = file.readByte();
			
			byte[] typeCode = new byte[numCols];
			file.read(typeCode);
			
			values = new String[numCols+1];
			
			values[0] = Integer.toString(rowId);
			
			for(int i = 1; i <= numCols; i++){
				switch(typeCode[i-1]){
					case Constants.NULL:  file.readByte();
					    values[i] = "null";
						break;

					case Constants.SHORTNULL:  file.readShort();
					    values[i] = "null";
						break;

					case Constants.INTNULL:  file.readInt();
					    values[i] = "null";
						break;

					case Constants.LONGNULL:  file.readLong();
					    values[i] = "null";
						break;

					case Constants.TINYINT:  
						values[i] = Integer.toString(file.readByte());
						break;

					case Constants.SHORTINT:  
						values[i] = Integer.toString(file.readShort());
						break;

					case Constants.INT:  
						values[i] = Integer.toString(file.readInt());
						break;

					case Constants.LONG:  
						values[i] = Long.toString(file.readLong());
						break;

					case Constants.FLOAT:  
						values[i] = String.valueOf(file.readFloat());
						break;

					case Constants.DOUBLE:  
						values[i] = String.valueOf(file.readDouble());
						break;

					case Constants.DATETIME:  
						Long temp = file.readLong();
						Date dateTime = new Date(temp);
						values[i] = dateFormat.format(dateTime);
						break;

					case Constants.DATE:  
						temp = file.readLong();
						Date date = new Date(temp);
						values[i] = dateFormat.format(date).substring(0,10);
						break;

					default:    int len = typeCode[i-1]-0x0C;
								byte[] bytes = new byte[len];
								file.read(bytes);
								values[i] = new String(bytes);
								break;
				}
			}

		}catch(Exception e){
			System.out.println(e);
		}
		return values;
	}

	public static void createTable(String table, String[] cols){
		try{
			// Adding Row ID as the first column
			String[] newCol = new String[cols.length+1];	
			newCol[0] = "rowid INT UNIQUE";
			for(int i=0;i<cols.length;i++) {
				newCol[i+1] = cols[i];
			}
			
			//Creating a file for the New Table
			RandomAccessFile file = new RandomAccessFile(Constants.dirUserdata+table+Constants.FILE_TYPE, "rw");
			file.setLength(Constants.pageSize);
			file.seek(0);
			file.writeByte(Constants.recordsPage);
			file.close();	
			
			// Inserting values into the davisbase_tables
			String[] values = {"0", table, String.valueOf(0)};
			insertInto(Constants.TABLE_CATALOG, values,Constants.dirCatalog);
			
			// Parsing the column data and Inserting into davisbase_columns
			for(int i = 0; i < newCol.length; i++){
				String[] tokens = newCol[i].split(" ");
				String nullable;
				String unique="NO";
				
				if(tokens.length > 2)
				{
					nullable = "NO";
					if(tokens[2].toUpperCase().trim().equals("UNIQUE"))
						unique = "YES";
					else
						unique = "NO";
				}
				else
					 nullable = "YES";
				
				// Inserting the value into davisbase_columns
				String[] value = {"0", table, tokens[0], tokens[1].toUpperCase(), String.valueOf(i+1), nullable, unique};
				insertInto("davisbase_columns", value,Constants.dirCatalog);
			}

		}catch(Exception e){
			System.out.println(e);
		}
	}

	public static void update(String table, String[] cmp, String[] set, String dir){
		try{
			ArrayList<Integer> rowids = new ArrayList<Integer>();
			
			// Getting the Row IDs that are to be updated
			if(cmp.length==0 || !"rowid".equals(cmp[0])) {
				
				Records records = select(table, new String[] {"*"}, cmp, false);
				rowids.addAll(records.content.keySet());
			}
			else
				rowids.add(Integer.parseInt(cmp[2]));
			
			for(int key : rowids) {
				RandomAccessFile file = new RandomAccessFile(dir+table+Constants.FILE_TYPE, "rw");
				int numPages = getPageCount(file);
				
				// Iterating over all the pages to check which page contains our key
				int page = 0;
				for(int currPage = 1; currPage <= numPages; currPage++) {
					if(BPlusTree.hasKey(file, currPage, key) && BPlusTree.getPageType(file, currPage)==Constants.recordsPage){
						page = currPage;
					}
				}
				
				if(page==0){
					System.out.println("The given key value does not exist");
					return;
				}
				
				// Getting all the keys on the current page
				int[] keys = BPlusTree.getKeyArray(file, page);
				int cellNo = 0;
				
				// Searching for our key
				for(int i = 0; i < keys.length; i++)
					if(keys[i] == key)
						cellNo = i;
				
				// Getting the location of our key
				int offset = BPlusTree.getCellOffset(file, page, cellNo);
				long loc = BPlusTree.getCellLoc(file, page, cellNo);
				
				// Getting all the columns, saved values and data types for current key
				String[] cols = getColName(table);
				String[] values = retrieveValues(file, loc);
				String[] type = getDataType(table);
				
				// Handling the date data type
				for(int i=0; i < type.length; i++)
					if(type[i].equals("DATE") || type[i].equals("DATETIME"))
						values[i] = "'"+values[i]+"'";
				
				// Searching for our column
				int x = 0;
				for(int i = 0; i < cols.length; i++)
					if(cols[i].equals(set[0])) {
						x = i;
						break;
					}
				
				// Updating column value
				values[x] = set[2];
	
				
				// Checking for null-value constraint
				String[] nullable = getNullable(table);
				for(int i = 0; i < nullable.length; i++){
					if(values[i].equals("null") && nullable[i].equals("NO")){
						System.out.println("NULL-VALUE constraint violation");
						return;
					}
				}
				
				// Updating the value in file
				byte[] stc = new byte[cols.length-1];
				int plsize = calPayloadSize(table, values, stc);
				BPlusTree.updateLeafCell(file, page, offset, plsize, key, stc, values);
	
				file.close();
			}

		}catch(Exception e){
			System.out.println(e);
		}
	}

	public static void insertInto(String table, String[] values,String dir_s){
		try{
			RandomAccessFile file = new RandomAccessFile(dir_s + table + Constants.FILE_TYPE, "rw");
			insertInto(file, table, values);
			file.close();

		}catch(Exception e){
			System.out.println(e);
		}
	}
	
	public static void insertInto(RandomAccessFile file, String table, String[] values){
		String[] dtype = getDataType(table);
		String[] nullable = getNullable(table);
		String[] unique = getUnique(table);
		
		int rowId = 0;
		if(Constants.TABLE_CATALOG.equals(table) || Constants.COLUMN_CATALOG.equals(table)) {
			
			int numOfPages = getPageCount(file);
			int pages = 1;
			for(int p = 1; p <= numOfPages; p++){
				int rm = BPlusTree.getRightMost(file, p);
				if(rm == 0)
					pages = p;
			}
			int[] keys = BPlusTree.getKeyArray(file, pages);
			for(int i = 0; i < keys.length; i++)
				if(keys[i]>rowId)
					rowId = keys[i];
		}
		else {
			
			Records rowIdRecords = select(Constants.TABLE_CATALOG, new String[]{"cur_row_id"}, new String[]{"table_name", "=", table}, false);
			rowId = Integer.parseInt(rowIdRecords.content.entrySet().iterator().next().getValue()[2]);
		}
		
		values[0]=String.valueOf(rowId + 1);
		
		for(int i = 0; i < nullable.length; i++)
			if(values[i].equals("null") && nullable[i].equals("NO")){
				System.out.println("NULL-VALUE constraint violation");
				System.out.println();
				return;
			}
		
		for(int i = 0; i < unique.length; i++)
			if(unique[i].equals("YES")){
				System.out.println("Checking for UNIQUE constraint violation");
				System.out.println();
				
				try {
					String[] columnName = getColName(table);
					
					String[] cmp = {columnName[i],"=",values[i]};
					Records records = select(table, new String[] {"*"}, cmp, false);
					
					
					if(records.rowNumber>0){
						System.out.println("Duplicate key found for " + columnName[i].toString());
						System.out.println();
						return;
					}
				}
				catch (Exception e){
					System.out.println(e);
				}
			}


		int newRowId = Integer.parseInt(values[0]);
		int page = searchKeyPage(file, newRowId);
		if(page != 0)
			if(BPlusTree.hasKey(file, page, newRowId)){
				System.out.println("Uniqueness constraint violation");
				System.out.println("for");
				for(int k=0; k < values.length; k++)
				System.out.println(values[k]);
				
				return;
			}
		
		if(page == 0)
			page = 1;
		
		
		byte[] typeCode = new byte[dtype.length-1];
		short payloadSize = (short) calPayloadSize(table, values, typeCode);
		int cellSize = payloadSize + 6;
		int offset = BPlusTree.checkLeafSpace(file, page, cellSize);
		
		if(offset != -1){
			BPlusTree.insertLeafCell(file, page, offset, payloadSize, newRowId, typeCode, values);

		}else{
			BPlusTree.splitLeaf(file, page);
			insertInto(file, table, values);
		}
		
		if(!Constants.TABLE_CATALOG.equals(table) && !Constants.COLUMN_CATALOG.equals(table)) {
			update(Constants.TABLE_CATALOG, 
					new String[] {"table_name", "=", table}, 
					new String[] {"cur_row_id", "=", String.valueOf(values[0])}, 
					Constants.dirCatalog);
		}
	}

	public static int calPayloadSize(String table, String[] vals, byte[] typeCode){
		String[] dataType = getDataType(table);
		int size =dataType.length;
		for(int i = 1; i < dataType.length; i++){
			typeCode[i - 1]= getTypeCode(vals[i], dataType[i]);
			size = size + fieldLength(typeCode[i - 1]);
		}
		return size;
	}
	

	public static byte getTypeCode(String value, String dataType){
		if(value.equals("null")){
			switch(dataType){
				case "TINYINT":     return Constants.NULL;
				case "SMALLINT":    return Constants.SHORTNULL;
				case "INT":			return Constants.INTNULL;
				case "BIGINT":      return Constants.LONGNULL;
				case "REAL":        return Constants.INTNULL;
				case "DOUBLE":      return Constants.LONGNULL;
				case "DATETIME":    return Constants.LONGNULL;
				case "DATE":        return Constants.LONGNULL;
				case "TEXT":        return Constants.LONGNULL;
				default:			return Constants.NULL;
			}							
		}else{
			switch(dataType){
				case "TINYINT":     return Constants.TINYINT;
				case "SMALLINT":    return Constants.SHORTINT;
				case "INT":			return Constants.INT;
				case "BIGINT":      return Constants.LONG;
				case "REAL":        return Constants.FLOAT;
				case "DOUBLE":      return Constants.DOUBLE;
				case "DATETIME":    return Constants.DATETIME;
				case "DATE":        return Constants.DATE;
				case "TEXT":        return (byte)(value.length() + Constants.TEXT);
				default:			return Constants.NULL;
			}
		}
	}
	
    public static short fieldLength(byte typeCode){
		switch(typeCode){
			case Constants.NULL: return 1;
			case Constants.SHORTNULL: return 2;
			case Constants.INTNULL: return 4;
			case Constants.LONGNULL: return 8;
			case Constants.TINYINT: return 1;
			case Constants.SHORTINT: return 2;
			case Constants.INT: return 4;
			case Constants.LONG: return 8;
			case Constants.FLOAT: return 4;
			case Constants.DOUBLE: return 8;
			case Constants.DATETIME: return 8;
			case Constants.DATE: return 8;
			default:   return (short)(typeCode - Constants.TEXT);
		}
	}
    
    
    public static int searchKeyPage(RandomAccessFile file, int key){
		try{
			int numPages = getPageCount(file);
			
			for(int currPage = 1; currPage <= numPages; currPage++){

				file.seek((currPage - 1)*Constants.pageSize);
				byte pageType = file.readByte();
				
				if(pageType == Constants.recordsPage){
					
					int[] keys = BPlusTree.getKeyArray(file, currPage);
					
					if(keys.length == 0)
						return 0;
					
					int rm = BPlusTree.getRightMost(file, currPage);
					
					if(keys[0] <= key && key <= keys[keys.length - 1])
						return currPage;
					else if(rm == 0 && keys[keys.length - 1] < key)
						return currPage;
				}
			}
		}
		catch(Exception e){
			System.out.println(e);
		}

		return 1;
	}

	public static String[] getDataType(String table){
		return getDatabaseColumnsColumn(3, table);
	}

	public static String[] getColName(String table){
		return getDatabaseColumnsColumn(2, table);
	}

	public static String[] getNullable(String table){
		return getDatabaseColumnsColumn(5, table);
	}
	
	public static String[] getUnique(String table){
		return getDatabaseColumnsColumn(6, table);
	}
	
	public static String[] getDatabaseColumnsColumn(int i, String table){
		try{
			RandomAccessFile file = new RandomAccessFile(Constants.dirCatalog+"davisbase_columns.tbl", "rw");
			Records records = new Records();
			String[] columnName = {"rowid", "table_name", "column_name", "data_type", "ordinal_position", "is_nullable","is_unique"};
			String[] cmp = {"table_name","=",table};
			filter(file, cmp, columnName,new String[] {}, records);

			HashMap<Integer, String[]> content = records.content;

			ArrayList<String> array = new ArrayList<String>();
			for(String[] x : content.values()){
				array.add(x[i]);
			}
			return array.toArray(new String[array.size()]);
		}
		catch(Exception e){
			System.out.println(e);
		}
		return new String[0];
	}
	
	public static Records select(String table, String[] cols, String[] cmp, boolean display){
		try{
			
			String path = Constants.dirUserdata ;
			if (table.equalsIgnoreCase(Constants.TABLE_CATALOG) || table.equalsIgnoreCase(Constants.COLUMN_CATALOG))
				path = Constants.dirCatalog ;
			
			
			RandomAccessFile file = new RandomAccessFile(path+table+Constants.FILE_TYPE, "rw");
			
			String[] columnName = getColName(table);
			String[] dataType = getDataType(table);
			
			Records records = new Records();
			
			if (cmp.length > 0 && cmp[1].equals("=") && cmp[2].equalsIgnoreCase("null")) 
			{
				System.out.println("Empty Set");
				file.close();
				return null;
			}
			if (cmp.length > 0 && cmp[1].equals("!=") && cmp[2].equalsIgnoreCase("null")) 
				cmp = new String[0];
			
			filter(file, cmp, columnName, dataType, records);
			
			if(display) records.display(cols); 
			
			file.close();
			
			return records;
		}catch(Exception e){
			System.out.println(e);
			return null;
		}
	}

	public static void filter(RandomAccessFile file, String[] cmp, String[] columnName, String[] type, Records records){
		try{
			
			int numOfPages = getPageCount(file);
			
			for(int page = 1; page <= numOfPages; page++){
				file.seek((page-1) * Constants.pageSize);
				byte pageType = file.readByte();
				
				if(pageType == Constants.recordsPage){
					byte numOfCells = BPlusTree.getCellNumber(file, page);
	
					for(int cellNum=0; cellNum < numOfCells; cellNum++){
						long loc = BPlusTree.getCellLoc(file, page, cellNum); 
						String[] vals = retrieveValues(file, loc);
						int rowid=Integer.parseInt(vals[0]);
						
						// Date Handling
						for(int j=0; j < type.length; j++)
							if(type[j].equals("DATE") || type[j].equals("DATETIME"))
								vals[j] = "'"+vals[j]+"'";
						
						boolean check = cmpCheck(vals, rowid , cmp, columnName);
	
						// Date Handling
						for(int j=0; j < type.length; j++)
							if(type[j].equals("DATE") || type[j].equals("DATETIME"))
								vals[j] = vals[j].substring(1, vals[j].length()-1);
	
						if(check)
							records.add(rowid, vals);

					}
				}
			    else
					continue;
			}
			
			records.columnName = columnName;
			records.format = new int[columnName.length];

		}catch(Exception e){
			System.out.println("Error: At filter()");
			e.printStackTrace();
		}

	}
	
	public static boolean cmpCheck(String[] values, int rowid, String[] cmp, String[] columnName){
		boolean check = false;
		
		if(cmp.length == 0){
			check = true;
		}
		else{
			int colPos = 1;
			for(int i = 0; i < columnName.length; i++){
				if(columnName[i].equals(cmp[0])){
					colPos = i + 1;
					break;
				}
			}
			
			if(colPos == 1){
				int val = Integer.parseInt(cmp[2]);
				String operator = cmp[1];
				
				switch(operator){
					case Constants.EQUALS_SIGN: 
						return rowid == val;
					case Constants.GREATER_THAN_SIGN: 
						return rowid > val;
					case Constants.GREATER_THAN_EQUAL_SIGN: 
						return rowid >= val;
					case Constants.LESS_THAN_SIGN: 
						return rowid < val;
					case Constants.LESS_THAN_EQUAL_SIGN: 
						return rowid <= val;
					case Constants.NOT_EQUAL_SIGN: 
						return rowid != val;						  							  							  							
				}
			}
			else 
				return cmp[2].equals(values[colPos-1]);
		}
		return check;
	}
	
	public static void createIndex(String table, String[] cols){
		try{
			String path = Constants.dirUserdata ;
			
			RandomAccessFile file = new RandomAccessFile(path+table+Constants.FILE_TYPE, "rw");
			String[] columnName = getColName(table);
			
			BTree b = new BTree(new RandomAccessFile(path+table+Constants.INDEX_FILE_TYPE, "rw"));
			
			int control=0;
			for(int j = 0; j < cols.length; j++)
				for(int i = 0; i < columnName.length; i++)
					if(cols[j].equals(columnName[i]))
						control = i;
			
			try{
				
				int numOfPages = getPageCount(file);
				for(int page = 1; page <= numOfPages; page++){
					
					file.seek((page-1)*Constants.pageSize);
					byte pageType = file.readByte();
					if(pageType == 0x0D)
					{
						byte numOfCells = BPlusTree.getCellNumber(file, page);

						for(int i=0; i < numOfCells; i++){
							long loc = BPlusTree.getCellLoc(file, page, i);	
							String[] vals = retrieveValues(file, loc);
							// int rowid=Integer.parseInt(vals[0]);
							
							b.add(String.valueOf(vals[control]), String.format("%04x",loc));
						}
					}
					else
						continue;
				}

				// buffer.columnName = columnName;
				// buffer.format = new int[columnName.length];

			}catch(Exception e){
				System.out.println("Error: At createIndex()");
				e.printStackTrace();
			}
			file.close();
			
		}catch(Exception e){
			System.out.println(e);
		}
	}
}