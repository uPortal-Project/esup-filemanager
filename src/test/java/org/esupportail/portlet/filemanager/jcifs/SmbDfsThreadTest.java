package org.esupportail.portlet.filemanager.jcifs;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.Random;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbFile;

/**
 * Please configure test.properties before calling the main method of this class application.
 * 
 * * put a dfs share as jcifsUrl property
 * 
 * * jcifsUsername0 and jcifsUsername1 should not have same access rights
 * 
 * Using JCIFS version 1.3.17, and with synchronizeRootListing = false, you will have some unexpected exceptions like : 
 *  jcifs.smb.SmbAuthException: Access is denied.
	at jcifs.smb.SmbTransport.checkStatus(SmbTransport.java:546)
	at jcifs.smb.SmbTransport.send(SmbTransport.java:663)
 *
 * With synchronizeRootListing = true, no more exceptions ...
 *
 */
public class SmbDfsThreadTest extends Thread {

    int id;
    String url;
    NtlmPasswordAuthentication auth;
    long start_time;
    boolean synchronizeRootListing;

    static Random rnd = new Random(1234);
    static long num_sessions = 2;
    static int nbThreads = 2;
    static int maxDepth = 3;
    static long session_time = 1 * 1000;

    static boolean verbose = true;


    SmbDfsThreadTest(NtlmPasswordAuthentication auth, String url, boolean synchronizeRootListing, int id) {
        this.url = url;
        this.auth = auth;
        this.id = id;
        this.synchronizeRootListing = synchronizeRootListing;
        this.start_time = System.currentTimeMillis();
    }

    void traverse( SmbFile f, int depth ) throws MalformedURLException, IOException {

        if( depth == 0 ) {
            return;
        }
        SmbFile[] l = null;
        try {
            if (f.exists()) {
            	if(synchronizeRootListing && this.url.equals(f.getCanonicalPath())) {
	            	synchronized (SmbDfsThreadTest.class) {
	            		l = f.listFiles();
	            	}
            	} else {
            		l = f.listFiles();
            	}
            }
        } catch (SmbAuthException ae) {
            System.err.println("SAE: " + ae.getMessage() + " on " + f + " for " + auth.getUsername());
            ae.printStackTrace( System.err );
            return;
        } catch (NullPointerException npe) {
            System.err.println("NPE");
            npe.printStackTrace( System.err );
            return;
        }
        for(int i = 0; l != null && i < l.length; i++ ) {
            try {
                boolean exists = l[i].exists();
                if (verbose) {
                    System.out.print(id + " ");
                    System.out.print("[" + auth.getUsername() + "] ");
                    for( int j = maxDepth - depth; j > 0; j-- ) {
                       System.out.print( "  " );
                    }
                    System.out.println( l[i] + " " + exists );
                }
                if( l[i].isDirectory() ) {
                	if(!l[i].getName().contains("MEDPR") && !l[i].getName().contains("CFC"))
                		traverse( l[i], depth - 1 );
                }
            } catch (SmbAuthException ae) {
                System.err.println("SAE: " + ae.getMessage());
                ae.printStackTrace( System.err );
            } catch( IOException ioe ) {
                System.out.println( l[i] + ":" );
                ioe.printStackTrace( System.out );
            }
            try {
                Thread.sleep(Math.abs(rnd.nextInt(2)+1));
            } catch (InterruptedException e) {

            } 
        }
    }

    public void run () {
        SmbFile f = null;
        int runs = 0;
        while(true) {
            try {
                Thread.sleep(100);
            }catch (InterruptedException e) {}

            while (f == null) {
                try {
                    f = new SmbFile(url, auth);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }
            }
            try {
                traverse(f, maxDepth);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
            runs++;
            long time =  System.currentTimeMillis() - start_time;
            if (time > session_time) {
                System.err.println(id + " exit (" + time/runs + ")");
                return;
            }
        }
    }

    public static void createThreads(int i, int count) throws IOException {
    	
    	Properties testProps = new Properties();
    	InputStream in= SmbDfsThreadTest.class.getResourceAsStream("/test.properties");
    	testProps.load(in);
    	in.close();
    	
    	String jcifsUrl = testProps.getProperty("jcifsUrl");
    	boolean synchronizeRootListing = new Boolean(testProps.getProperty("jcifsSynchronizeRootListing"));
    	int j = i%2;
    	String jcifsUsername = testProps.getProperty("jcifsUsername" + j);
    	String jcifsPassword = testProps.getProperty("jcifsPassword" + j);
    	String jcifsDomain = testProps.getProperty("jcifsDomain" + j);
        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(jcifsDomain, jcifsUsername, jcifsPassword);
        
        int num = 0;
        System.err.println("creating " + count  + " threads");
        while (num < count) {
            SmbDfsThreadTest sc = new SmbDfsThreadTest(auth, jcifsUrl, synchronizeRootListing, i * 100 + num++);
            sc.start();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public static void main(String[] args) throws Exception {
        for(int i = 0; i < num_sessions; i++) {
            createThreads(i+1, nbThreads);
        }
        sleep(6000000);
    }
}

