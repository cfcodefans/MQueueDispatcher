import java.io.IOException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.thenetcircle.services.commons.MiscUtils;


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

}
