/*-----------------------------------------------------------------------------
 * Example: 
 *  - Several Java threads produce some objects with string IDs. 
 *  - We need a container to collect these objects, and then
 *    to return them sorted by ID. 
 *  - The container is implemented in C++ (JDK 1.1 does not have 
 *    sort facilities). 
 *---------------------------------------------------------------------------*/

import java.io.*;
import java.util.Random;
import java.lang.Math;

public class JniComplexExample {
   // load the library which implements the native method
   static {
	  try {
		 System.loadLibrary("jni_complex_example");
	  }
	  catch (Throwable e) {
		 System.out.println("Exception: " + e);
	  }
   }

   /* Native functions declarations:
      - 'init_native_resources()' initializes the native code data structures
      - 'register_object()' inserts a given object into the container
      - 'recall_objects()' returns a sorted array of the collected objects */
   
   public static native void init_native_resources();
   public static native void clean_native_resources();
   public static native void register_object(NameWithInfo obj);
   public static native NameWithInfo[] recall_objects();

   // Main function:
   // creates a number of random generators of NameWithInfo objects,
   // and runs them in parallel.
   
   public static void main(String[] args) {
	  JniComplexExample x = new JniComplexExample();
	  init_native_resources();
	  
	  int N_THREADS = 10;
	  int N_EXAMPLES = 10;
	  int MAX_DELAY_MSEC = 100;

	  // initialization
	  init_native_resources();
	  Generator threads[] = new Generator[N_THREADS];
	  for (int i = 0; i < N_THREADS; i++)
		 threads[i] = new Generator(i, N_EXAMPLES, MAX_DELAY_MSEC);

	  // launch threads and wait until they are finished
	  for (int i = 0; i < N_THREADS; i++)
		 threads[i].start();

	  for (int i = 0; i < N_THREADS; i++) {
		 try {
			threads[i].join();
		 }
		 catch (InterruptedException e) {}
	  }

	  // print the sorted array of objects
	  System.out.println("--------------------------------------------------");
	  NameWithInfo[] allObjects = recall_objects();
	  for (int i = 0; i < allObjects.length; i++)
		 System.out.println(allObjects[i].name + ": " + allObjects[i].info +
							" (generated by thread " + allObjects[i].threadId +
			                ")");

	  clean_native_resources();
   }
}

/*-----------------------------------------------------------------------------
 * Class 'NameWithInfo' represents a sample object,
 * containing a name (ID), some information component, as well as the number 
 * of the thread which created this object (for pretty output)
 *---------------------------------------------------------------------------*/
class NameWithInfo {
   public String name;
   public String info;
   public int threadId;

   public NameWithInfo(String aName, String aInfo, int aThreadId) {
	  name = aName;
	  info = aInfo;
	  threadId = aThreadId;
   }
};

/*-----------------------------------------------------------------------------
 * Generator class generates the given number of random NameWithInfo objects
 * with random time intervals.
 *---------------------------------------------------------------------------*/
class Generator extends Thread
{
   private int maxDelay;		// Maximal delay between time intervals
   private int nObjects;		// Number of objects to create
   private int threadId;

   public Generator(int aThreadId, int aNObjects, int aMaxDelay) {
	  threadId = aThreadId;
	  maxDelay = aMaxDelay;
	  nObjects = aNObjects;
   }
	
   public void run() {
	  for (int i = 0; i < nObjects; i++) {
		 String name = RandomGenerator.getRandomString();
		 String info = RandomGenerator.getRandomString();
 		 NameWithInfo obj = new NameWithInfo(name, info, threadId);
		 System.out.println("Register " + obj.name + " (" + obj.info +
							", thread #" + obj.threadId + ")");
		 JniComplexExample.register_object(obj);
		 try {
			sleep(RandomGenerator.getRandomNumber(1, maxDelay));
		 }
		 catch (InterruptedException e) {}
	  }
   }
}

/*-----------------------------------------------------------------------------
 * Class 'RandomGenerator' contains utilities for generating
 * random strings and random integers in the given interval.
 *---------------------------------------------------------------------------*/
class RandomGenerator {
   static private Random rand = new Random();
   static final int MAX_LENGTH = 10;
      
   static public int getRandomNumber(int m, int n) {
	  double x = rand.nextDouble();
	  int result = (int) Math.round(x * (n - m)) + m;
	  return Math.max(m, Math.min(n, result));
   }
   
   static public String getRandomString() {
	  int length = getRandomNumber(1, MAX_LENGTH);
	  char str[] = new char[length];
	  for (int i = 0; i < length; i++)
		 str[i] = (char) getRandomNumber('a', 'z');
	  return new String(str);
   }
}
