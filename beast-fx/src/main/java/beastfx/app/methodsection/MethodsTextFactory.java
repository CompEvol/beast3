package beastfx.app.methodsection;


import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;
import beastfx.app.inputeditor.BeautiDoc;


import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class MethodsTextFactory {
	/** maps BEAST class to accompanying methodsText class **/
	static Map<Class<?>, Class<?>> object2MethodsText = null;
	
	/** initialises object2MethodsText through introspection **/
	static private void init() {
		if (object2MethodsText != null) {
			return;
		}
		Log.warning.print("Discovering MethodsText classes ... ");
		object2MethodsText = new HashMap<>();
        List<String> methodsTypes = PackageManager.find(MethodsText.class, new String[]{"beastfx.app.methodsection"});
        for (String methodsTypeName : methodsTypes) {
            try {
                MethodsText methodsType = (MethodsText) BEASTClassLoader.forName(methodsTypeName).getDeclaredConstructor().newInstance();
                object2MethodsText.put(methodsType.type(), methodsType.getClass());
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
		Log.warning("Done");
	}
	
	
	static private MethodsText createMethodsText(Class<?> clazz) {

		if (object2MethodsText.containsKey(clazz)) {
			Class<?> c = object2MethodsText.get(clazz);
			try {
				MethodsText m = (MethodsText) c.getDeclaredConstructor().newInstance();
				return m;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (clazz != Object.class) {
			return createMethodsText(clazz.getSuperclass());
		}
		
		// should never get here
		return null;
	}

	
	
	
	static public MethodsText createMethodsText(Object o) {
		init();
		if (o instanceof BEASTObject) {
			return createMethodsText(o.getClass());
		}
		return new MethodsText() {
			
			@Override
			public Class<?> type() {
				return null;
			}
			
			@Override
			public List<Phrase> getModelDescription(Object o, BEASTInterface parent, Input<?> input, BeautiDoc doc) {
				List<Phrase> m = new ArrayList<>();
				m.add(new Phrase(o, parent, input, o.toString()));
				return m;
			}
		};
	}
	
	static public List<Phrase> getModelDescription(Object o, BEASTInterface parent, Input<?> input, BeautiDoc doc) {
		List<Phrase> m0 = BeautiSubTemplateMethodsText.getModelDescription(o, parent, input, doc);
		if (m0 != null) {
			return m0;
		}
		
		MethodsText m = createMethodsText(o);
		List<Phrase> modelDescription = m.getModelDescription(o, parent, input, doc);
		return modelDescription;
	}

	static public String getDataDescription(Object o) {
		MethodsText m = createMethodsText(o);
		String dataDescription = m.getDataDescription(o);
		return dataDescription;
	}
}
