package beastfx.app.methodsection.objecteditor;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;
import beastfx.app.inputeditor.BeautiDoc;


public class ObjectEditorFactory {
    /**
     * map that identifies the InputEditor to use for a particular type of Input *
     */
    HashMap<Class<?>, String> objectEditorMap;
    HashMap<Class<?>, String> listObjectEditorMap;

	public void init(BeautiDoc doc) {
		if (objectEditorMap != null) {
			return;
		}
		objectEditorMap = new LinkedHashMap<>();
		listObjectEditorMap = new LinkedHashMap<>();
		String[] PACKAGE_DIRS = { "methods.objecteditor", };
		for (String packageName : PACKAGE_DIRS) {
			List<String> inputEditors = PackageManager.find("methods.objecteditor.ObjectEditor", packageName);
			registerInputEditors(inputEditors.toArray(new String[0]), doc);
		}
	}

	private void registerInputEditors(String[] inputEditors, BeautiDoc doc) {
		// BeautiDoc doc = new BeautiDoc();
		for (String inputEditor : inputEditors) {
			// ignore inner classes, which are marked with $
			if (!inputEditor.contains("$")) {
				try {
					Class<?> _class = BEASTClassLoader.forName(inputEditor);

	                Constructor<?> con = _class.getConstructor(BeautiDoc.class);
	                ObjectEditor editor = (ObjectEditor) con.newInstance(doc);

					// InputEditor editor = (InputEditor) _class.newInstance();
					Class<?>[] types = editor.getTypes();
					for (Class<?> type : types) {
						objectEditorMap.put(type, inputEditor);
						if (editor instanceof ListObjectEditor) {
							Class<?> baseType = ((ListObjectEditor) editor).getBaseType();
							listObjectEditorMap.put(baseType, inputEditor);
						}
					}
				} catch (java.lang.InstantiationException e) {
					// ignore input editors that are inner classes
				} catch (Throwable e) {
					// print message
					Log.err.println(e.getClass().getName() + ": " + e.getMessage());
				}
			}
		}
	}

	public ObjectEditor getObjectEditor(BEASTInterface o, Input<?> input, BeautiDoc doc) throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (input.getType() == null) {
            input.determineClass(o);
        }
		
        Class<?> inputClass = input.getType();
        if (inputClass == null) {
        	return null;
        }
    	if (input.get() != null && !input.get().getClass().equals(inputClass)
    			&& !(input.get() instanceof ArrayList)) {
    		Log.trace.println(input.get().getClass() + " != " + inputClass);
    		inputClass = input.get().getClass();
    	}

        //Log.trace.print(inputClass.getName() + " => ");
        ObjectEditor objectEditor = null;


        if (List.class.isAssignableFrom(inputClass) ||
                (input.get() != null && input.get() instanceof List<?>)) {
            // handle list inputs
            if (listObjectEditorMap.containsKey(inputClass)) {
                // use custom list input editor
                String inputEditorName = listObjectEditorMap.get(inputClass);
                Constructor<?> con = BEASTClassLoader.forName(inputEditorName).getConstructor(BeautiDoc.class);
                objectEditor = (ObjectEditor) con.newInstance(doc);

                //inputEditor = (InputEditor) BEASTClassLoader.forName(inputEditor).newInstance();
            } else {
                // otherwise, use generic list editor
                objectEditor = new ListObjectEditor(doc);
            }
        } else if (input.possibleValues != null) {
            // handle enumeration inputs
            objectEditor = new EnumObjectEditor(doc);
        } else {
        	Class<?> inputClass2 = inputClass;
        	while (inputClass2 != null && !objectEditorMap.containsKey(inputClass2)) {
        		inputClass2 = inputClass2.getSuperclass(); 
        	}
        	if (inputClass2 == null) {
        		objectEditor = new BEASTObjectEditor(doc, this);
        	} else {
	            // handle BEASTObject-input with custom input editors
	            String inputEditorName = objectEditorMap.get(inputClass2);
	            
	            Constructor<?> con = BEASTClassLoader.forName(inputEditorName).getConstructor(BeautiDoc.class);
	            objectEditor = (ObjectEditor) con.newInstance(doc);
        	}
        }        	
        return objectEditor;
	}
	
	
	
}
