package com.thenetcircle.services.commons;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Created by fan on 2016/11/25.
 */
public class FileMonitor extends Observable implements Runnable, AutoCloseable {
    private static final Logger log = LogManager.getLogger(FileMonitor.class);
    WatchService ws = null;
    Map<WatchKey, Path> keys;

    public void regAllForWatch(Path _start) throws IOException {
        Files.walkFileTree(_start, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                regForWatch(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void regForWatch(Path dir) throws IOException {
        WatchKey regKey = dir.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(regKey, dir);
    }

    private Map<WatchEvent.Kind, Set<Path>> wrap(WatchKey wk, List<WatchEvent> watchEvents) {
        Path dir = keys.get(wk);
        if (dir == null) {
            log.error("WatchKey not recognized! " + wk.watchable());
            return Collections.emptyMap();
        }
        return MiscUtils.map(StandardWatchEventKinds.ENTRY_DELETE,
            watchEvents.stream().filter(we -> ENTRY_DELETE.equals(we.kind())).map(we -> (Path) we.context()).map(dir::resolve).collect(Collectors.toSet()),
            StandardWatchEventKinds.ENTRY_CREATE,
            watchEvents.stream().filter(we -> ENTRY_CREATE.equals(we.kind())).map(we -> (Path) we.context()).map(dir::resolve).collect(Collectors.toSet()),
            StandardWatchEventKinds.ENTRY_MODIFY,
            watchEvents.stream().filter(we -> ENTRY_MODIFY.equals(we.kind())).map(we -> (Path) we.context()).map(dir::resolve).collect(Collectors.toSet()));
    }

    public static Map<WatchEvent.Kind, Set<Path>> castEvent(Object event) {
        return (Map<WatchEvent.Kind, Set<Path>>) event;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            // wait for key to be signalled
            WatchKey wk;
            try {
                wk = ws.take();
                log.info(wk);
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(wk);
            if (dir == null) {
                log.error("WatchKey not recognized! " + wk.watchable());
                continue;
            }

            List<WatchEvent<?>> watchEvents = wk.pollEvents();
            super.notifyObservers(watchEvents);

            for (WatchEvent<?> event : watchEvents) {
                WatchEvent.Kind kind = event.kind();
                //TBD - provide example of how  OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                //print out event
                log.info("{}: {}\n", event.kind().name(), child);

                if (kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            regAllForWatch(child);
                        } else {
                            regForWatch(child);
                        }
                    } catch (IOException x) {
                        // ignore to keep sample readbale
                    }
                }
            }


            if (wk.reset()) {
                keys.remove(wk);
                if (keys.isEmpty()) {
                    return;
                }
            }
        }
    }

    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    @Override
    public void close() throws Exception {
        if (ws != null) ws.close();
    }
}
