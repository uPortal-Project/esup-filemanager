package org.esupportail.filemanager.services;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Singleton component that tracks the number of currently open connections
 * for each configured storage drive (FsAccess implementations).
 *
 * Drives call {@link #connectionOpened(String, String)} when a connection is
 * established, and {@link #connectionClosed(String)} when it is torn down.
 */
@Component
public class StorageConnectionMonitor {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StorageConnectionMonitor.class);

    /** Circumference of the SVG gauge circle (r=38, 2πr ≈ 238.76). */
    private static final double GAUGE_CIRCUMFERENCE = 238.76;

    /** Number of connections that corresponds to a full gauge (100 %). */
    private static final int GAUGE_MAX_CONNECTIONS = 200;

    private final ConcurrentHashMap<String, DriveConnectionInfo> drives = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Package-level API called by FsAccess subclasses
    // -----------------------------------------------------------------------

    /**
     * Records that a new connection has been opened for the given drive.
     *
     * @param driveName the logical name of the drive
     * @param type      a human-readable protocol label ("SFTP", "S3", "SMB 3.1.1", …)
     */
    public void connectionOpened(String driveName, String type) {
        DriveConnectionInfo info = drives.computeIfAbsent(driveName, k -> new DriveConnectionInfo(driveName, type));
        // Always refresh the type so the most recently negotiated protocol is shown
        info.updateType(type);
        info.increment();
        log.debug("Connection opened on drive '{}' (type={}) – open count={}, max count={}",
                driveName, type, info.getOpenConnections(), info.getMaxConnections());
    }

    /**
     * Records that an existing connection has been closed for the given drive.
     *
     * @param driveName the logical name of the drive
     */
    public void connectionClosed(String driveName) {
        DriveConnectionInfo info = drives.get(driveName);
        if (info != null) {
            info.decrement();
            log.debug("Connection closed on drive '{}' – open count={}",
                    driveName, info.getOpenConnections());
        }
    }

    /**
     * Returns an immutable snapshot of connection statistics, sorted by drive name.
     */
    public List<DriveConnectionInfo> getStats() {
        return drives.values().stream()
                .sorted(Comparator.comparing(DriveConnectionInfo::getDriveName))
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Inner DTO
    // -----------------------------------------------------------------------

    /**
     * Holds live statistics for a single drive and pre-computes values needed
     * to render the SVG gauge in the Thymeleaf template.
     */
    public static class DriveConnectionInfo {

        private final String driveName;
        /** Protocol label – updated on every new connection (e.g. "SMB 3.1.1"). */
        private volatile String type;
        private final AtomicInteger openConnections = new AtomicInteger(0);
        /** Peak (maximum) concurrent connections since application startup. */
        private final AtomicInteger maxConnections  = new AtomicInteger(0);

        public DriveConnectionInfo(String driveName, String type) {
            this.driveName = driveName;
            this.type = type;
        }

        public void updateType(String type) {
            if (type != null && !type.isEmpty()) {
                this.type = type;
            }
        }

        public void increment() {
            int current = openConnections.incrementAndGet();
            maxConnections.updateAndGet(max -> Math.max(max, current));
        }

        public void decrement() {
            // never go below 0
            openConnections.updateAndGet(v -> v > 0 ? v - 1 : 0);
        }

        // --- Getters used by Thymeleaf ---

        public String getDriveName() {
            return driveName;
        }

        public String getType() {
            return type;
        }

        public int getOpenConnections() {
            return openConnections.get();
        }

        /**
         * Peak number of simultaneous connections observed on this drive since
         * the application started (volatile, reset on restart).
         */
        public int getMaxConnections() {
            return maxConnections.get();
        }

        /**
         * Gauge fill percentage (0–100) where 100 % corresponds to
         * {@value StorageConnectionMonitor#GAUGE_MAX_CONNECTIONS} connections.
         */
        public int getGaugePercent() {
            return Math.min(openConnections.get() * 100 / GAUGE_MAX_CONNECTIONS, 100);
        }

        /**
         * SVG {@code stroke-dashoffset} for the progress arc.
         * A value of 0 means the ring is fully drawn; the full circumference
         * means the ring is hidden.
         */
        public double getGaugeDashOffset() {
            return GAUGE_CIRCUMFERENCE * (1.0 - getGaugePercent() / 100.0);
        }

        /**
         * Returns a Bootstrap-compatible hex colour for the gauge arc depending
         * on the number of open connections.
         */
        public String getGaugeColor() {
            int c = openConnections.get();
            if (c == 0)   return "#adb5bd"; // gray   – no connections
            if (c < 10)   return "#198754"; // green  – low load    (1–9)
            if (c <= 50)  return "#0d6efd"; // blue   – medium load (10–50)
            if (c <= 200) return "#fd7e14"; // orange – high load   (51–200)
            return "#dc3545";               // red    – very high load (201+)
        }

        /**
         * Returns the Bootstrap badge CSS class suffix matching {@link #getGaugeColor()}.
         */
        public String getStatusBadgeClass() {
            int c = openConnections.get();
            if (c == 0)   return "secondary";
            if (c < 10)   return "success";
            if (c <= 50)  return "primary";
            if (c <= 200) return "warning";
            return "danger";
        }
    }
}

