package com.thenetcircle.services.commons.rest.script;

import com.google.common.io.PatternFilenameFilter;
import com.thenetcircle.services.commons.FileMonitor;
import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.ProcTrace;
import com.thenetcircle.services.commons.rest.utils.AjaxResContext;
import com.thenetcircle.services.commons.web.joint.script.ScriptUtils;
import com.thenetcircle.services.commons.web.mvc.ResCacheMgr;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.model.Resource;

import javax.script.*;
import javax.ws.rs.core.Application;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Created by fan on 2016/11/25.
 */
public class ScriptResLoader extends ResourceConfig {
    private static final Logger log = LogManager.getLogger(ScriptResLoader.class);
    public static final String SCRIPT_PATH = "script-path";

    public ScriptResLoader() {
        ProcTrace.start(MiscUtils.invocationInfo());
        ProcTrace.ongoing("set packages scan");

        register(JacksonFeature.class);
        register(EncodingFilter.class);
        register(GZipEncoder.class);
        register(DeflateEncoder.class);

        ProcTrace.end();
        log.info(ProcTrace.flush());

        instance = this;

        loadResourcesFromScript();
        startWatchScriptFolder();
    }

    private static ScriptResLoader instance = null;
    private AtomicBoolean startedWatch = new AtomicBoolean(false);
    private ExecutorService watchThread = Executors.newSingleThreadExecutor(MiscUtils.namedThreadFactory("script-folder-watcher"));

    private void startWatchScriptFolder() {
        if (!startedWatch.compareAndSet(false, true)) return;
        watchThread.submit(() -> {
            try (FileMonitor fm = new FileMonitor()) {
                fm.addObserver(this::onFileChange);
                fm.run();
            } catch (Exception e) {
                log.error("something wrong with file watcher", e);
            }
        });
    }

    public void loadResourcesFromScript() {
        String[] pathStrs = getScriptPaths();
        Set<Resource> resSet = Stream.of(pathStrs).parallel()
            .map(pathStr -> ResCacheMgr.getAbsoluteResPath("WEB-INF", pathStr))
            .map(Paths::get)
            .filter(Files::exists)
            .map(Path::toFile)
            .filter(File::isDirectory)
            .map(dir -> dir.listFiles(new PatternFilenameFilter("*.js")))
            .flatMap(Stream::of)
            .map(this::executeScriptFile)
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

        AjaxResContext ajaxResContext = AjaxResContext.getInstance(this.getApplicationName());
        ajaxResContext.build(resSet);
        registerResources(resSet);
    }

    public String[] getScriptPaths() {
        String pathProperty = Objects.toString(this.getConfiguration().getProperty(SCRIPT_PATH), "").trim();
        return Stream.of(StringUtils.split(pathProperty, ",")).map(String::trim).toArray(String[]::new);
    }

    private Set<Resource> executeScriptFile(File file) {
        log.info("loading rest jersey resource from {}", file);

        String extStr = FilenameUtils.getExtension(file.getName());
        ScriptEngine se = ScriptUtils.getScriptEngineByMimeType(extStr);
        if (se == null) {
            log.error("invalid script file extension: {}", file);
            return Collections.emptySet();
        }

        String scriptStr = null;
        try {
            scriptStr = FileUtils.readFileToString(file, Charset.defaultCharset());
            if (StringUtils.isBlank(scriptStr)) {
                log.error("scriptStr: {} is blank", scriptStr);
                return Collections.emptySet();
            }

            ScriptContext sc = new SimpleScriptContext();
            Bindings bindings = sc.getBindings(ScriptContext.GLOBAL_SCOPE);
            if (bindings == null) {
                log.debug("bindings are null, need initialization");
                bindings = se.createBindings();
                sc.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
                bindings.put("Application", this);
            }

            se.eval(scriptStr, sc);
            Invocable inv = (Invocable) se;
            IResourceGenerator resGen = inv.getInterface(IResourceGenerator.class);
            if (resGen == null) {
                log.error("failed to get function to create Resources at {}", file);
                return Collections.emptySet();
            }
            Set<Resource> resourceSet = resGen.apply(this);
            scriptPathAndResources.put(file.toPath(), resourceSet);
            return resourceSet;
        } catch (IOException e) {
            log.error("fail to execute script file: ", e);
        } catch (ScriptException e) {
            log.info(MiscUtils.lineNumber(scriptStr));
            log.error(String.format("failed to execute script: \n\t %s \n\t", MiscUtils.lineNumber(scriptStr)), e);
        }
        return Collections.emptySet();
    }

    interface IResourceGenerator extends Function<Application, Set<Resource>> {
    }

    private void onFileChange(Observable fm, Object _watchEvents) {

        Map<WatchEvent.Kind, Set<Path>> eventAndPaths = FileMonitor.castEvent(_watchEvents);
        eventAndPaths.get(ENTRY_DELETE).forEach(path -> scriptPathAndResources.remove(path));
        Set<Resource> newResources = eventAndPaths.get(ENTRY_CREATE).stream()
            .map(Path::toFile)
            .map(this::executeScriptFile)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
        registerResources(newResources);

        eventAndPaths.get(ENTRY_MODIFY).forEach(path -> scriptPathAndResources.remove(path));
        Set<Resource> modifiedResources = eventAndPaths.get(ENTRY_CREATE).stream()
            .map(Path::toFile)
            .map(this::executeScriptFile)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
        registerResources(modifiedResources);

        AjaxResContext ajaxResContext = AjaxResContext.getInstance(this.getApplicationName());
        ajaxResContext.getProxyList().clear();
        ajaxResContext.build(scriptPathAndResources.values().stream().flatMap(Set::stream).collect(Collectors.toList()));
    }

    private Map<Path, Set<Resource>> scriptPathAndResources = new HashMap<>();
}
