package org.esupportail.filemanager.web;

import org.esupportail.filemanager.services.StorageConnectionMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;

@RequestMapping("/javaperf")
@Controller
@PreAuthorize("hasRole(@environment.getProperty('security.role.admin'))")
public class JavaPerfController {

    @Autowired
    private StorageConnectionMonitor storageConnectionMonitor;

	@RequestMapping
	public String getJavaPerf(Model uiModel) throws IOException {

		Runtime runtime = Runtime.getRuntime();
		long maxMemoryInMB = runtime.maxMemory() / 1024 / 1024;
		long totalMemoryInMB = runtime.totalMemory() / 1024 / 1024;
		long freeMemoryInMB = runtime.freeMemory() / 1024 / 1024;
		long usedMemoryInMB = totalMemoryInMB - freeMemoryInMB;
		uiModel.addAttribute("maxMemoryInMB", maxMemoryInMB);
		uiModel.addAttribute("totalMemoryInMB", totalMemoryInMB);
		uiModel.addAttribute("freeMemoryInMB", freeMemoryInMB);
		uiModel.addAttribute("usedMemoryInMB", usedMemoryInMB);


		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		List<ThreadInfo> threadInfos = Arrays.asList(threadMXBean.dumpAllThreads(true, true));
		Collections.sort(threadInfos, (ThreadInfo o1, ThreadInfo o2) -> o1.getThreadState().compareTo(o2.getThreadState()));
		Map<String, Long> threadStateCount = new HashMap<>();
		for (ThreadInfo threadInfo : threadInfos) {
			String threadState = threadInfo.getThreadState().toString();
			if (threadStateCount.containsKey(threadState)) {
				threadStateCount.put(threadState, threadStateCount.get(threadState) + 1);
			} else {
				threadStateCount.put(threadState, 1L);
			}
		}
		uiModel.addAttribute("threadMXBean", threadMXBean);
		uiModel.addAttribute("threadInfos", threadInfos);
		uiModel.addAttribute("threadStateCount", threadStateCount);
		long currentThreadId = Thread.currentThread().getId();
		uiModel.addAttribute("currentThreadId", currentThreadId);

        uiModel.addAttribute("javaPerfWrapper", new JavaPerfWrapper(threadMXBean, threadInfos));

        // Storage connection stats
        uiModel.addAttribute("storageStats", storageConnectionMonitor.getStats());

        return "javaperf";
	}


    class JavaPerfWrapper {
        private final ThreadMXBean threadMXBean;
        private final List<ThreadInfo> threadInfos;
        public JavaPerfWrapper(ThreadMXBean threadMXBean, List<ThreadInfo> threadInfos) {
            this.threadMXBean = threadMXBean;
            this.threadInfos = threadInfos;
        }
        public ThreadMXBean getThreadMXBean() {
            return threadMXBean;
        }
        public List<ThreadInfo> getThreadInfos() {
            return threadInfos;
        }
        public int getThreadCount() {
            return threadMXBean.getThreadCount();
        }
        public int getPeakThreadCount() {
            return threadMXBean.getPeakThreadCount();
        }
        public List<ThreadInfoWrapper> getThreadInfoWrappers() {
            List<ThreadInfoWrapper> wrappers = new ArrayList<>();
            for (ThreadInfo threadInfo : threadInfos) {
                wrappers.add(new ThreadInfoWrapper(threadInfo));
            }
            return wrappers;
        }
    }

    /*
    Permet d'accéder aux informations d'un ThreadInfo via thymeleaf
                threadId
               threadName
               threadState
               blockedCount
               blockedTime
               waitedCount
               waitedTime
               stackTrace
     */
    class ThreadInfoWrapper {
        private final ThreadInfo threadInfo;
        public ThreadInfoWrapper(ThreadInfo threadInfo) {
            this.threadInfo = threadInfo;
        }
        public long getThreadId() {
            return threadInfo.getThreadId();
        }
        public String getThreadName() {
            return threadInfo.getThreadName();
        }
        public String getThreadState() {
            return threadInfo.getThreadState().name();
        }
        public long getBlockedCount() {
            return threadInfo.getBlockedCount();
        }
        public long getBlockedTime() {
            return threadInfo.getBlockedTime();
        }
        public long getWaitedCount() {
            return threadInfo.getWaitedCount();
        }
        public long getWaitedTime() {
            return threadInfo.getWaitedTime();
        }
        public StackTraceElementWrapper[] getStackTrace() {
            StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
            StackTraceElementWrapper[] wrappers = new StackTraceElementWrapper[stackTraceElements.length];
            for (int i = 0; i < stackTraceElements.length; i++) {
                wrappers[i] = new StackTraceElementWrapper(stackTraceElements[i]);
            }
            return wrappers;
        }
    }

    class StackTraceElementWrapper {
        private final StackTraceElement stackTraceElement;

        public StackTraceElementWrapper(StackTraceElement stackTraceElement) {
            this.stackTraceElement = stackTraceElement;
        }

        public String getClassName() {
            return stackTraceElement.getClassName();
        }

        public String getMethodName() {
            return stackTraceElement.getMethodName();
        }

        public String getFileName() {
            return stackTraceElement.getFileName();
        }

        public int getLineNumber() {
            return stackTraceElement.getLineNumber();
        }

        public String getToString() {
            return stackTraceElement.toString();
        }
    }
}
