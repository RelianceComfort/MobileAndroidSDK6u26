package com.metrix.architecture.utilities;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;  
import java.io.File;  
import java.io.FileInputStream;  
import java.io.FileOutputStream;  
import java.io.IOException;  
import java.util.ArrayList;
import java.util.zip.DataFormatException;  
import java.util.zip.Deflater;  
import java.util.zip.Inflater;  
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MetrixCompressionHelper {
	  //private static final Logger LOG = Logger.getLogger(CompressionUtils.class);  
    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;
    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * @param zipFilePath
     * @param destDirectory
     * @throws IOException
     */
    public static ArrayList<String> unzip(String zipFilePath, String destDirectory) throws IOException {
    	ArrayList<String> fileList = new ArrayList<String>();
    	
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();            
            fileList.add(entry.getName());
            
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
        
        return fileList;
    }
    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }  
	    
	  public static byte[] compress(byte[] data) throws IOException {  
	   Deflater deflater = new Deflater();  
	   deflater.setInput(data);  
	   
	   ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);   
	       
	   deflater.finish();  
	   byte[] buffer = new byte[1024];   
	   while (!deflater.finished()) {  
	    int count = deflater.deflate(buffer); // returns the generated code... index  
	    outputStream.write(buffer, 0, count);   
	   }  
	   outputStream.close();  
	   byte[] output = outputStream.toByteArray();  
	   
	   deflater.end();

//	   LOG.debug("Original: " + data.length / 1024 + " Kb");  
//	   LOG.debug("Compressed: " + output.length / 1024 + " Kb");  
	   return output;  
	  }  
	   
	  public static byte[] decompress(byte[] data) throws IOException, DataFormatException {  
	   Inflater inflater = new Inflater();   
	   inflater.setInput(data);  
	   
	   ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);  
	   byte[] buffer = new byte[1024];  
	   while (!inflater.finished()) {  
	    int count = inflater.inflate(buffer);  
	    outputStream.write(buffer, 0, count);  
	   }  
	   outputStream.close();  
	   byte[] output = outputStream.toByteArray();  
	   
	   inflater.end();
	   
//	   LOG.debug("Original: " + data.length);  
//	   LOG.debug("Uncompressed: " + output.length);  
	   return output;  
	  }  
}
