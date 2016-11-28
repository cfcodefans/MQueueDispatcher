import com.thenetcircle.services.commons.rest.script.ScriptResLoader.IResourceGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.model.Resource;
import org.junit.Test;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptEngineTest {

    @Test
    public void testScriptEngineFactories() {
        ScriptEngineManager sem = new ScriptEngineManager();
        for (ScriptEngineFactory sef : sem.getEngineFactories()) {
            System.out.println("engine: { \n\tname: " + sef.getEngineName()
                + ", \n\tversion: " + sef.getEngineVersion()
                + ", \n\textensions: " + sef.getExtensions()
                + ", \n\tmime_types: " + sef.getMimeTypes()
                + ", \n\tnames: " + sef.getNames()
                + ", \n\tlanguage_name: " + sef.getLanguageName()
                + ", \n\tlanguage_version: " + sef.getLanguageVersion());
            System.out.println("}\n");
        }
    }

    @Test
    public void testPreformance() throws Exception {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByExtension("py");
        String loadResAsString = loadResAsString(ScriptEngineTest.class, "pref_sqrt.py");

        System.out.println(se.eval(loadResAsString));
    }

    public static String loadResAsString(final Class<?> cls, final String fileName) {
        if (cls == null || StringUtils.isBlank(fileName)) {
            return StringUtils.EMPTY;
        }

        try {
            return IOUtils.toString(cls.getResourceAsStream(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return StringUtils.EMPTY;
    }

    @Test
    public void testMap() {
        ConcurrentMap<Integer, String> map = new ConcurrentHashMap<>();
        map.computeIfAbsent(1, String::valueOf);
        System.out.println(map);
        System.out.println(map.putIfAbsent(2, "2"));
        System.out.println(map);
    }

    @Test
    public void testFormat() {
        //^(format:[a-z_]+;)(.*)$
        Matcher m = Pattern.compile("^(format:[a-z_]+;)(.*)$").matcher("format:json;{\"name\":\"jack\",\"age\":10,\"vip\":false,\"amqp:publisher:info\":{\"HTTP_ORIGIN\":null}}");
        System.out.println(m.find());
        System.out.println(m.groupCount());
        System.out.println(m.group(0));
        System.out.println(m.group(1));
    }

    @Test
    public void testResource() throws Exception {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByExtension("js");
        se.getContext().setWriter(new PrintWriter(System.out));

        String loadResAsString = FileUtils.readFileToString(new File("src/main/javascript/rest-tests.js"), Charset.defaultCharset());
        System.out.println(se.eval(loadResAsString));
        Invocable inv = (Invocable) se;

//        IResourceGenerator resGen = (IResourceGenerator) se.get("resGen");
        IResourceGenerator resGen = inv.getInterface(IResourceGenerator.class);
        Set<Resource> resourceSet = resGen.apply(null);
        System.out.println(resourceSet);
    }

}
