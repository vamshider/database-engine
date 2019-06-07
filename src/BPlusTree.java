import java.io.RandomAccessFile;
import java.util.Date;
import java.text.SimpleDateFormat;

public class BPlusTree{
	
	public static short calPayloadSize(String[] values, String[] dataType){
		int val = dataType.length; 
		for(int i = 1; i < dataType.length; i++){
			String dt = dataType[i];
			
			if(dt.equals("TINYINT")) {
				val += 1;
			}
			else if(dt.equals("SMALLINT")) {
				val += 2;
			}
			else if(dt.equals("INT") || dt.equals("REAL")) {
				val += 4;
			}
			else if(dt.equals("BIGINT") || dt.equals("DOUBLE") || dt.equals("DATETIME") || dt.equals("DATE")) {
				val += 8;
			}
			else if(dt.equals("TEXT")) {
				String text = values[i];
				int len = text.length();
				val += len;
			}
			
		}
		return (short)val;
	}
	
	public static int makePage(RandomAccessFile file, int x) {
		
		int numberOfPages = 0;
		try{
			numberOfPages = (int)(file.length()/(new Long(Constants.pageSize)));
			numberOfPages = numberOfPages + 1;
			file.setLength(Constants.pageSize * numberOfPages);
			file.seek((numberOfPages - 1) * Constants.pageSize);
			file.writeByte(x); 
		}catch(Exception e){
			System.out.println(e);
		}

		return numberOfPages;
	}
	
	public static int makeInteriorPage(RandomAccessFile file){
		return makePage(file, Constants.SHORTINT);	
	}

	public static int makeLeafPage(RandomAccessFile file){
		return makePage(file, Constants.recordsPage);
	}

	public static int findMidKey(RandomAccessFile file, int page){
		int value = 0;
		try{
			file.seek((page - 1) * Constants.pageSize);
			byte pageType = file.readByte();
			int numCells = getCellNumber(file, page);
			int mid = (int) Math.ceil((double) numCells / 2);
			long location = getCellLoc(file, page, mid - 1);
			file.seek(location);
			
			if(pageType == Constants.SHORTINT) {
				file.readInt(); 
				value = file.readInt();
			}
			else if(pageType == Constants.recordsPage){
				file.readShort();
				value = file.readInt();
			}
			
		}catch(Exception e){
			System.out.println(e);
		}

		return value;
	}
	
	public static void splitLeafPage(RandomAccessFile file, int currentPage, int newPage){
		try{
			
			int numCells = getCellNumber(file, currentPage);
			
			int mid = (int) Math.ceil((double) numCells / 2);

			int numCellA = mid - 1;
			int numCellB = numCells - numCellA;
			int content = 512;

			for(int i = numCellA; i < numCells; i++){
				long loc = getCellLoc(file, currentPage, i);
				file.seek(loc);
				int cellSize = file.readShort()+6;
				content = content - cellSize;
				file.seek(loc);
				byte[] cell = new byte[cellSize];
				file.read(cell);
				file.seek((newPage-1)*Constants.pageSize+content);
				file.write(cell);
				setCellOffset(file, newPage, i - numCellA, content);
			}

			
			file.seek((newPage-1)*Constants.pageSize+2);
			file.writeShort(content);

			
			short offset = getCellOffset(file, currentPage, numCellA-1);
			file.seek((currentPage-1)*Constants.pageSize+2);
			file.writeShort(offset);

			
			int rightMost = getRightMost(file, currentPage);
			setRightMost(file, newPage, rightMost);
			setRightMost(file, currentPage, newPage);

			
			int parent = getParent(file, currentPage);
			setParent(file, newPage, parent);

			
			byte num = (byte) numCellA;
			setCellNumber(file, currentPage, num);
			num = (byte) numCellB;
			setCellNumber(file, newPage, num);
			
		}catch(Exception e){
			System.out.println(e);
			
		}
	}
	
	public static void splitInteriorPage(RandomAccessFile file, int currentPage, int newPage){
		try{
			
			int numCells = getCellNumber(file, currentPage);
			
			int mid = (int) Math.ceil((double) numCells / 2);

			int numCellA = mid - 1;
			int numCellB = numCells - numCellA - 1;
			short content = 512;

			for(int i = numCellA+1; i < numCells; i++){
				long loc = getCellLoc(file, currentPage, i);
				short cellSize = 8;
				content = (short)(content - cellSize);
				file.seek(loc);
				byte[] cell = new byte[cellSize];
				file.read(cell);
				file.seek((newPage - 1) * Constants.pageSize + content);
				file.write(cell);
				file.seek(loc);
				int page = file.readInt();
				setParent(file, page, newPage);
				setCellOffset(file, newPage, i - (numCellA + 1), content);
			}
			
			int tmp = getRightMost(file, currentPage);
			setRightMost(file, newPage, tmp);
			
			long midLoc = getCellLoc(file, currentPage, mid - 1);
			file.seek(midLoc);
			tmp = file.readInt();
			setRightMost(file, currentPage, tmp);
			
			file.seek((newPage - 1) * Constants.pageSize + 2);
			file.writeShort(content);
			
			short offset = getCellOffset(file, currentPage, numCellA-1);
			file.seek((currentPage - 1) * Constants.pageSize + 2);
			file.writeShort(offset);

			int parent = getParent(file, currentPage);
			setParent(file, newPage, parent);
			
			byte num = (byte) numCellA;
			setCellNumber(file, currentPage, num);
			num = (byte) numCellB;
			setCellNumber(file, newPage, num);
			
		}catch(Exception e){
			System.out.println(e);
		}
	}
	
	public static Integer split(RandomAccessFile file, int page, int newPage, int midKey, int parent) {
		if(parent == 0){
			int rootPage = makeInteriorPage(file);
			setParent(file, page, rootPage);
			setParent(file, newPage, rootPage);
			setRightMost(file, rootPage, newPage);
			insertInteriorCell(file, rootPage, page, midKey);
			
			return rootPage;
		}else{
			long ploc = getPointerLoc(file, page, parent);
			setPointerLoc(file, ploc, parent, newPage);
			insertInteriorCell(file, parent, page, midKey);
			sortCellArray(file, parent);
			
			return parent;
		}
	}
	
	public static void splitLeaf(RandomAccessFile file, int page){
		int newPage = makeLeafPage(file);
		int midKey = findMidKey(file, page);
		splitLeafPage(file, page, newPage);
		int parent = getParent(file, page);
				
		split(file, page, newPage, midKey, parent);
		if(parent!=0) {
			while(checkInteriorSpace(file, parent)){
				parent = splitInterior(file, parent);
			}
		}
	}

	public static int splitInterior(RandomAccessFile file, int page){
		int newPage = makeInteriorPage(file);
		int midKey = findMidKey(file, page);
		splitInteriorPage(file, page, newPage);
		int parent = getParent(file, page);
		
		return split(file, page, newPage, midKey, parent);
	}

	public static void swap(int[] array, int x, int y) {
		int temp = array[x];
		array[x] = array[y];
		array[y] = temp;
	}
	
	public static void swap(short[] array, int x, int y) {
		short temp = array[x];
		array[x] = array[y];
		array[y] = temp;
	}
	
	public static void sortCellArray(RandomAccessFile file, int page){
		 byte number = getCellNumber(file, page);
		 int[] keyArray = getKeyArray(file, page);
		 short[] cellArray = getCellArray(file, page);
		 
		 for (int i = 1; i < number; i++) {
            for(int j = i ; j > 0 ; j--){
                if(keyArray[j] < keyArray[j-1]){
                	swap(keyArray, j, j-1);                	
                	swap(cellArray, j, j-1);                   
                }
            }
         }

         try{
         	file.seek((page - 1) * Constants.pageSize + 12);
         	for(int i = 0; i < number; i++){
				file.writeShort(cellArray[i]);
			}
         }catch(Exception e){
         	System.out.println("Error: At sortCellArray()");
         }
	}

	public static int[] getKeyArray(RandomAccessFile file, int page){
		int number = new Integer(getCellNumber(file, page));
		int[] array = new int[number];

		try{
			file.seek((page - 1) * Constants.pageSize);
			byte pageType = file.readByte();
			byte offset = 0;
			
			if(pageType == Constants.SHORTINT)
				offset = 4;
			else
				offset = 2;
			
			for(int i = 0; i < number; i++){
				long location = getCellLoc(file, page, i);
				file.seek(location + offset);
				array[i] = file.readInt();
			}

		}catch(Exception e){
			System.out.println(e);
		}

		return array;
	}
	
	public static short[] getCellArray(RandomAccessFile file, int page){
		int number = new Integer(getCellNumber(file, page));
		short[] array = new short[number];

		try{
			file.seek((page - 1) * Constants.pageSize + 12);
			for(int i = 0; i < number; i++){
				array[i] = file.readShort();
			}
		}catch(Exception e){
			System.out.println(e);
		}

		return array;
	}
	
	public static long getPointerLoc(RandomAccessFile file, int page, int parent){
		long value = 0;
		try{
			int numCells = new Integer(getCellNumber(file, parent));
			for(int i = 0; i < numCells; i++){
				long location = getCellLoc(file, parent, i);
				file.seek(location);
				int childPage = file.readInt();
				if(childPage == page){
					value = location;
				}
			}
		}catch(Exception e){
			System.out.println(e);
		}

		return value;
	}

	public static void setPointerLoc(RandomAccessFile file, long location, int parent, int page){
		try{
			if(location == 0){
				file.seek((parent - 1) * Constants.pageSize + 4);
			}else{
				file.seek(location);
			}
			file.writeInt(page);
		}catch(Exception e){
			System.out.println(e);
		}
	} 

	public static void insertInteriorCell(RandomAccessFile file, int page, int child, int key){
		try{
			
			file.seek((page - 1) * Constants.pageSize + 2);
			short content = file.readShort();
			
			if(content == 0)
				content = 512;
			
			content = (short)(content - 8);
			
			file.seek((page - 1) * Constants.pageSize + content);
			file.writeInt(child);
			file.writeInt(key);
			
			file.seek((page - 1) * Constants.pageSize + 2);
			file.writeShort(content);
			
			byte cellNumber = getCellNumber(file, page);
			setCellOffset(file, page ,cellNumber, content);
			
			cellNumber = (byte) (cellNumber + 1);
			setCellNumber(file, page, cellNumber);

		}catch(Exception e){
			System.out.println(e);
		}
	}
		
	public static void insertLeafCell(RandomAccessFile file, int page, int offset, short plsize, int key, byte[] stc, String[] vals){
		try{
			updateLeafCell(file, page, offset, plsize, key, stc, vals);
			
			int n = getCellNumber(file, page);
			byte tmp = (byte) (n+1);
			setCellNumber(file, page, tmp);
			file.seek((page - 1) * Constants.pageSize + 12 + n * 2);
			file.writeShort(offset);
			file.seek((page - 1) * Constants.pageSize + 2);
			int content = file.readShort();
			if(content >= offset || content == 0){
				file.seek((page - 1) * Constants.pageSize + 2);
				file.writeShort(offset);
			}
		}catch(Exception e){
			System.out.println(e);
		}
	}

	public static void updateLeafCell(RandomAccessFile file, int page, int offset, int plsize, int key, byte[] stc, String[] values){
		try{
			String s;
			file.seek((page - 1) * Constants.pageSize + offset);
			file.writeShort(plsize);
			file.writeInt(key);
			int column = values.length - 1;
			file.writeByte(column);
			file.write(stc);
			for(int i = 1; i < values.length; i++){
				switch(stc[i-1]){
					case Constants.NULL:
						file.writeByte(0);
						break;
					case Constants.SHORTNULL:
						file.writeShort(0);
						break;
					case Constants.INTNULL:
						file.writeInt(0);
						break;
					case Constants.LONGNULL:
						file.writeLong(0);
						break;
					case Constants.TINYINT:
						file.writeByte(new Byte(values[i]));
						break;
					case Constants.SHORTINT:
						file.writeShort(new Short(values[i]));
						break;
					case Constants.INT:
						file.writeInt(new Integer(values[i]));
						break;
					case Constants.LONG:
						file.writeLong(new Long(values[i]));
						break;
					case Constants.FLOAT:
						file.writeFloat(new Float(values[i]));
						break;
					case Constants.DOUBLE:
						file.writeDouble(new Double(values[i]));
						break;
					case Constants.DATETIME:
						s = values[i];
						Date tempOne = new SimpleDateFormat(Constants.datePattern).parse(s.substring(1, s.length()-1));
						long timeOne = tempOne.getTime();
						file.writeLong(timeOne);
						break;
					case Constants.DATE:
						s = values[i];
						s = s.substring(1, s.length() - 1);
						s = s+"_00:00:00";
						Date tempTwo = new SimpleDateFormat(Constants.datePattern).parse(s);
						long timeTwo = tempTwo.getTime();
						file.writeLong(timeTwo);
						break;
					default:
						file.writeBytes(values[i]);
						break;
				}
			}
		}catch(Exception e){
			System.out.println(e);
		}
	}

	public static boolean checkInteriorSpace(RandomAccessFile file, int page){
		byte numCells = getCellNumber(file, page);
		if(numCells > 30)
			return true;
		else
			return false;
	}

	public static int checkLeafSpace(RandomAccessFile file, int page, int size){
		int value = -1;

		try{
			file.seek((page - 1) * Constants.pageSize + 2);
			int content = file.readShort();
			if(content == 0)
				return Constants.pageSize - size;
			int numCells = getCellNumber(file, page);
			int space = content - 20 - 2*numCells;
			if(size < space)
				return content - size;
			
		}catch(Exception e){
			System.out.println(e);
		}

		return value;
	}

	
	public static int getParent(RandomAccessFile file, int page){
		int val = 0;

		try{
			file.seek((page - 1) * Constants.pageSize + 8);
			val = file.readInt();
		}catch(Exception e){
			System.out.println(e);
		}

		return val;
	}

	public static void setParent(RandomAccessFile file, int page, int parent){
		try{
			file.seek((page - 1) * Constants.pageSize + 8);
			file.writeInt(parent);
		}catch(Exception e){
			System.out.println(e);
		}
	}
	
	public static int getRightMost(RandomAccessFile file, int page){
		int rl = 0;

		try{
			file.seek((page - 1) * Constants.pageSize + 4);
			rl = file.readInt();
		}catch(Exception e){
			System.out.println("Error: At getRightMost()");
		}

		return rl;
	}

	public static void setRightMost(RandomAccessFile file, int page, int rightLeaf){

		try{
			file.seek((page - 1) * Constants.pageSize + 4);
			file.writeInt(rightLeaf);
		}catch(Exception e){
			System.out.println("Error: At setRightMost()");
		}
	}

	public static boolean hasKey(RandomAccessFile file, int page, int key){
		int[] keys = getKeyArray(file, page);
		for(int i : keys)
			if(key == i)
				return true;
		return false;
	}
	
	public static long getCellLoc(RandomAccessFile file, int page, int id){
		long location = 0;
		try{
			file.seek((page - 1) * Constants.pageSize + 12 + id * 2);
			short offset = file.readShort();
			long origin = (page - 1) * Constants.pageSize;
			location = origin + offset;
		}catch(Exception e){
			System.out.println(e);
		}
		return location;
	}

	public static byte getCellNumber(RandomAccessFile file, int page){
		byte value = 0;

		try{
			file.seek((page - 1) * Constants.pageSize + 1);
			value = file.readByte();
		}catch(Exception e){
			System.out.println(e);
		}

		return value;
	}

	public static void setCellNumber(RandomAccessFile file, int page, byte number){
		try{
			file.seek((page - 1) * Constants.pageSize + 1);
			file.writeByte(number);
		}catch(Exception e){
			System.out.println(e);
		}
	}
	
	public static short getCellOffset(RandomAccessFile file, int page, int id){
		short offset = 0;
		try{
			file.seek((page - 1) * Constants.pageSize + 12 + id * 2);
			offset = file.readShort();
		}catch(Exception e){
			System.out.println(e);
		}
		return offset;
	}

	public static void setCellOffset(RandomAccessFile file, int page, int id, int offset){
		try{
			file.seek((page - 1) * Constants.pageSize + 12 + id * 2);
			file.writeShort(offset);
		}catch(Exception e){
			System.out.println(e);
		}
	}
    
	public static byte getPageType(RandomAccessFile file, int page){
		byte type=Constants.SHORTINT;
		try {
			file.seek((page-1)*Constants.pageSize);
			type = file.readByte();
		} catch (Exception e) {
			System.out.println(e);
		}
		return type;
	}
}