package org.esupportail.filemanager.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;

/**
 * Please configure test.properties (vfs part) before calling the main method of this class application.
 *
 */
public class VfsThreadTest extends Thread {

    static final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();

    static int verboseCount = 0;
	
    int id;
    String url;
    long start_time;

    static Random rnd = new Random(1234);
    static int nbThreads = 2000000;
    static long session_time = 1 * 100000;

    static boolean verbose = false;
    static boolean veryVerbose = false;
    
    VfsThreadTest(String url) {
        this.url = url;
        this.start_time = System.currentTimeMillis();
    }
    
    private void openUrl() throws FileSystemException {
    	FileSystemOptions fsOptions = new FileSystemOptions();
    	FileSystemManager fsManager = VFS.getManager();
    	FileObject root = fsManager.resolveFile(url, fsOptions);
    	
    	if(veryVerbose) {
	    	System.out.println("Name : " + root.getName().getBaseName());
	    	System.out.println("Type : " + root.getType());
	    	List<String> children = new ArrayList<String>();
	    	for(FileObject child : root.getChildren()) {
	    		children.add(child.getName().getBaseName());
	    	}
	    	System.out.println("Children : " + children);
    	}
	}

    public void run () {
        int runs = 0;
        while(true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}

            try {
            	openUrl();
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
            if(runs%100==0) {
            	printThreadStatus();
            }
        }
    }



	public static void createThreads(int count) throws IOException {
    	
    	Properties testProps = new Properties();
    	InputStream in= VfsThreadTest.class.getResourceAsStream("/test.properties");
    	testProps.load(in);
    	in.close();
    	
    	String vfsUrl = testProps.getProperty("vfsUrl");
        
        int num = 0;
        System.err.println("creating " + count  + " threads");
        while (num < count) {
            VfsThreadTest sc = new VfsThreadTest(vfsUrl);
            sc.start();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();  
            }
        }
    }

    public static void main(String[] args) throws Exception {
        createThreads(nbThreads);
        sleep(6000000);
    }
    
    protected static void printThreadStatus() {
    	if(verboseCount <= 0) {
	        ThreadInfo[] infos = mbean.dumpAllThreads(true, true);
	
	        List<ThreadInfo> blockedThreads = new ArrayList<ThreadInfo>();
	        for (ThreadInfo threadInfo : infos) {
	        	if(Thread.State.BLOCKED.equals(threadInfo.getThreadState())) {
	        		blockedThreads.add(threadInfo);
	        		if(verbose) {
	        			System.err.println(threadInfo.getThreadName() + " state = " + threadInfo.getThreadState());
	        			Throwable t = new Throwable();
	        			t.setStackTrace(threadInfo.getStackTrace());
	        			t.printStackTrace();
	        			verboseCount = 100;
	        		}
	        	}
	        }
	        System.err.println("blocked threads : " + blockedThreads.size());
    	} else {
    		verboseCount--;
    	}
    }
}

