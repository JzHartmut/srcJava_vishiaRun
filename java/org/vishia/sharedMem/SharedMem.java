package org.vishia.sharedMem;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class SharedMem implements Closeable {

  
  //For file locking mechanism see also https://www.baeldung.com/java-lock-files
  
  
  RandomAccessFile f1;
  FileChannel c1;
  MappedByteBuffer m1;
  byte[] b;
  
  public SharedMem() {
  }
  
  
  
  public void open(File file, int size)  throws IOException {
    this.f1 = new RandomAccessFile(file, "rws");
    this.c1 = this.f1.getChannel();
    this.m1 = this.c1.map(MapMode.READ_WRITE, 0, size);
    this.m1.order(ByteOrder.LITTLE_ENDIAN);
    //this.b = m1.array();
    
  }
  
  
  
  
  
  
  
  public byte[] buffer() { return this.b; }
  
  public MappedByteBuffer getBuffer() { return this.m1; }
  
  
  @Override public void close() throws IOException {
    this.c1.close();
    if(this.f1 !=null) {
      this.f1.close();
    }
  }

  
  public void open_B ( File file, int size) {
    try {
      Path path = Paths.get(file.getAbsolutePath());
      this.c1 = FileChannel.open(path, StandardOpenOption.READ );
      this.m1 = this.c1.map(MapMode.READ_ONLY, 0, size);
      this.m1.order(ByteOrder.LITTLE_ENDIAN);
    }
    catch(Exception exc) {
      System.err.println(exc.getMessage());
    }
  }
  
  
  //This is the port to JNI, but adds complexity. Is it necessary?
  //https://www.baeldung.com/jni
  /**Gets a shared mem reference as byte[] for a given share name.
   * This routine needs JNI access to a dynamic linked library from the operation system.
   * <br>
   * Hint: The application should load the proper shared library which fulfills this operation. 
   * <br>
   * Hint: Using shared memory may be a vulnerability of source of errors because
   * any other application can use this shared memory and disturb data or read data.
   * 
   * @param name Name of the share global for the OS
   * @return byte[] array to access the data. 
   */
  public native byte[] getSharedMemRef ( String name);
  
}
