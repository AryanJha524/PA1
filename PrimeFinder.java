// Name: Aryan Jha
// NID: ar392004
// COP 4520, Spring 2022

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class PrimeFinder {

// setting constants to be used in the program
public static final int numThreads = 8;
public static final int maxLimit = 1000;

// Create a shared sieve which holds a boolean flag
// to represent if the number at that index is prime
public static boolean [] sieve = new boolean [maxLimit + 1];

// create a shared complete boolean array to communicate between
// threads when a thread is finished computing
public static boolean [] complete = new boolean [numThreads];

// create List to store our threads
private ArrayList<Thread> primeSolverThreads;

// need a lock to allow single access to our sieve
// set fairness to true to allow FIFO access of resource
public static final Semaphore sem = new Semaphore(1, true);

public static void main(String args[]) {
        long startTime = System.nanoTime();

        long numPrimes = 0;
        long sumOfPrimes = 0;

        PrimeFinder alphaThread = new PrimeFinder();
        alphaThread.primeSolverThreads = new ArrayList<>();

        // we know that 2 and 3 are primes and the sieve of atkin algorithm
        // requires us to set these manually
        alphaThread.sieve[2] = true;
        alphaThread.sieve[3] = true;

        // initialize threads that will be used to solve the problem
        for (int i = 0; i < numThreads; i++) {
                // create an instance of the thread with the alpha thread so
                // access to the sieve, lock, and other shared variables is there
                // and a unique thread number between 0-7 to identify threads by
                PrimeSolver primeSolver = new PrimeSolver(alphaThread, i, maxLimit);
                Thread newThread = new Thread(primeSolver);
                alphaThread.primeSolverThreads.add(newThread);
        }
        // start all the threads
        for (int i = 0; i < numThreads; i++) {
                alphaThread.primeSolverThreads.get(i).start();
        }
        // wait for threads to complete tasks
        for (int i = 0; i < numThreads; i++) {
                try {
                        alphaThread.primeSolverThreads.get(i).join();
                }
                catch (Exception e) {
                        System.out.println(e);
                }
        }
        // terminate threads
        try {
                for (int i = 0; i < numThreads; i++) {
                        alphaThread.primeSolverThreads.get(i).interrupt();
                }
        }
        catch (Exception e) {
                System.out.println(e);
        }
        // grab end time
        long endTime = System.nanoTime();

        long executionTime = (endTime - startTime) / 1000000;
        // calculate number of primes found and sum of primes
        for (int i = 2; i < sieve.length; i++) {
                if (sieve[i] == true) {
                        numPrimes++;
                        sumOfPrimes += i;
                }
        }

        // find top 10 highest primes
        ArrayList<Integer> highestPrimes = new ArrayList<Integer>(10);
        int counter = 0;
        int index = maxLimit;
        // loop backwards through sieve until 10 primes found
        while (counter < 10) {
                if (sieve[index] == true) {
                        highestPrimes.add(index);
                        counter++;
                }
                index--;
        }
        Collections.reverse(highestPrimes);

        // write to a file
        try {
                File file = new File("primes.txt");
                file.createNewFile();
                FileWriter writer = new FileWriter("primes.txt");

                writer.write("Execution Time (ms): " + executionTime + " Number of Primes: "
                             + numPrimes + " Sum of Primes: " + sumOfPrimes);
                writer.write("\nTop 10 primes from lowest to highest: \n");
                for (Integer i : highestPrimes) {
                        writer.write(i + ", ");
                }
                writer.close();
        }
        catch (Exception e) {
                System.out.println(e);
        }
        return;
}
}

// this class represents the threads that will be solving the problem
class PrimeSolver implements Runnable {
// shared thread allows access to the sieve, the lock, the done array, and others
private PrimeFinder sharedThread;
// unique for each thread in order to see what threads have completed
private int threadNum;
private int maxVal;

public PrimeSolver(PrimeFinder inputThread, int inputThreadNum, int inputMaxVal) {
        this.sharedThread = inputThread;
        this.threadNum = inputThreadNum;
        this.maxVal = inputMaxVal;
}

// main function to override if implementing Runnable interface
@Override
public void run() {
        try {
                SieveOfAtkinModified(sharedThread, threadNum, maxVal);
        }
        catch (Exception e) {
                System.out.println(e);
        }
}

// original algorithm referenced from GeeksForGeeks: https://www.geeksforgeeks.org/sieve-of-atkin/
public void SieveOfAtkinModified(PrimeFinder sharedThread, int threadNum, int maxVal) {
        // start at 1 + threadNum so each individual thread is working on its own set of numbers
        // and doesn't overlap. Increment by number of threads for the same reason, to prevent
        // duplicate work
        for (int x = 1 + threadNum; x * x < maxVal; x += sharedThread.numThreads) {
                for (int y = 1; y * y < maxVal; y++) {

                        int n = (4 * x * x) + (y * y);
                        // if the below statement is true, n is a prime number
                        if (n <= maxVal && (n % 12 == 1 || n % 12 == 5)) {
                                // try to acquire a lock on the sieve and set the nth element to true (prime number found)
                                try {
                                        this.sharedThread.sem.acquire();
                                        this.sharedThread.sieve[n] ^= true;
                                        this.sharedThread.sem.release();
                                }
                                catch (Exception e) {
                                        System.out.println(e);
                                }
                        }

                        n = (3 * x * x) + (y * y);
                        if (n <= maxVal && n % 12 == 7) {
                                try {
                                        this.sharedThread.sem.acquire();
                                        this.sharedThread.sieve[n] ^= true;
                                        this.sharedThread.sem.release();
                                }
                                catch (Exception e) {
                                        System.out.println(e);
                                }
                        }

                        n = (3 * x * x) - (y * y);
                        if (x > y && n <= maxVal && n % 12 == 11) {
                                try {
                                        this.sharedThread.sem.acquire();
                                        this.sharedThread.sieve[n] ^= true;
                                        this.sharedThread.sem.release();
                                }
                                catch (Exception e) {
                                        System.out.println(e);
                                }
                        }
                }
        }

        // current thread is finished
        sharedThread.complete[threadNum] = true;

        // wait for ALL threads to complete marking prime numbers
        boolean completed = false;
        while (!completed) {
                // if other threads are still working, free up this current thread
                // from using more resources since its execution is done for now
                try {
                        Thread.sleep(100);
                }
                catch (Exception e) {
                        System.out.println(e);
                }
                // assume all threads are complete
                completed = true;
                // if any threads are still working, set completed to false
                for (int i = 0; i < sharedThread.numThreads; i++) {
                        if (sharedThread.complete[i] == false) {
                                completed = false;
                                break;
                        }
                }
        }
        // Mark all multiples of squares as non-prime
        for (int r = 5; r * r < maxVal; r++) {
                try {
                        // acquire lock to allow exclusive access to modify sieve
                        this.sharedThread.sem.acquire();
                        if (this.sharedThread.sieve[r]) {
                                for (int i = r * r; i < maxVal; i += r * r)
                                        this.sharedThread.sieve[i] = false;
                        }
                        this.sharedThread.sem.release();
                }
                catch (Exception e) {
                        System.out.println(e);
                }
        }
}
}
